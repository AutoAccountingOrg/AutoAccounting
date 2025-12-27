/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package net.ankio.auto.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import net.ankio.auto.databinding.AdapterAssetGroupHeaderBinding
import net.ankio.auto.databinding.AdapterAssetListBinding
import net.ankio.auto.ui.utils.setAssetIcon
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.constant.AssetsType
import org.ezbook.server.db.model.AssetsModel

/**
 * 资产列表项密封类
 * 使用密封类消除类型判断的特殊情况，让编译器强制覆盖所有分支
 */
sealed class AssetListItem {
    /**
     * 分组头项
     * @param type 资产类型
     * @param title 分组标题
     * @param count 该分组下的资产数量
     * @param expanded 是否展开
     */
    data class GroupHeader(
        val type: AssetsType,
        val title: String,
        val count: Int,
        val expanded: Boolean
    ) : AssetListItem()

    /**
     * 资产项
     * @param asset 资产数据模型
     */
    data class AssetItem(val asset: AssetsModel) : AssetListItem()
}

/**
 * 资产分组适配器
 *
 * 支持可折叠分组的资产列表，使用两种视图类型：
 * - 分组头：显示类型标题、数量、展开/折叠图标
 * - 资产项：显示资产图标、名称、货币
 *
 * 设计原则（遵循Linus好品味）：
 * 1. 数据结构优先：使用密封类消除类型判断的if/else
 * 2. 简洁实现：折叠状态存储在数据中，无需额外状态管理
 * 3. 职责单一：适配器只负责渲染，折叠逻辑通过回调交给外部
 */
class AssetGroupAdapter : RecyclerView.Adapter<AssetGroupAdapter.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    // 当前显示的列表项（包含分组头和资产项）
    private val items = mutableListOf<AssetListItem>()

    // 原始分组数据：类型 -> 资产列表
    private var groupedAssets: Map<AssetsType, List<AssetsModel>> = emptyMap()

    // 各分组的展开状态
    private val expandedStates = mutableMapOf<AssetsType, Boolean>()

    // 是否显示货币标签
    private val showCurrency: Boolean = PrefManager.featureMultiCurrency

    // 点击回调
    private var onItemClick: ((AssetsModel) -> Unit)? = null
    private var onItemLongClick: ((AssetsModel, View) -> Unit)? = null

    /**
     * 设置资产点击回调
     */
    fun setOnItemClickListener(listener: (AssetsModel) -> Unit) = apply {
        onItemClick = listener
    }

    /**
     * 设置资产长按回调
     */
    fun setOnItemLongClickListener(listener: (AssetsModel, View) -> Unit) = apply {
        onItemLongClick = listener
    }

    /**
     * 更新资产数据
     * @param assets 资产列表
     * @param typeOrder 类型顺序，控制分组显示顺序
     * @param typeNameMapper 类型名称映射函数
     */
    fun updateAssets(
        assets: List<AssetsModel>,
        typeOrder: List<AssetsType>,
        typeNameMapper: (AssetsType) -> String
    ) {
        // 按类型分组
        groupedAssets = assets.groupBy { it.type }

        // 初始化展开状态（默认全部展开）
        typeOrder.forEach { type ->
            if (type !in expandedStates) {
                expandedStates[type] = true
            }
        }

        // 重建显示列表
        rebuildDisplayList(typeOrder, typeNameMapper)
    }

    /**
     * 重建显示列表
     * 根据展开状态构建实际显示的列表项
     */
    private fun rebuildDisplayList(
        typeOrder: List<AssetsType>,
        typeNameMapper: (AssetsType) -> String
    ) {
        val oldItems = items.toList()
        val newItems = mutableListOf<AssetListItem>()

        typeOrder.forEach { type ->
            val assetsOfType = groupedAssets[type] ?: emptyList()
            // 即使没有资产也显示分组头（方便用户知道有这个分类）
            if (assetsOfType.isNotEmpty()) {
                val expanded = expandedStates[type] ?: true

                // 添加分组头
                newItems.add(
                    AssetListItem.GroupHeader(
                        type = type,
                        title = typeNameMapper(type),
                        count = assetsOfType.size,
                        expanded = expanded
                    )
                )

                // 如果展开，添加该分组下的资产
                if (expanded) {
                    assetsOfType.forEach { asset ->
                        newItems.add(AssetListItem.AssetItem(asset))
                    }
                }
            }
        }

        // 使用DiffUtil高效更新
        val diffResult = DiffUtil.calculateDiff(ItemDiffCallback(oldItems, newItems))
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * 切换分组展开状态
     * @param type 要切换的资产类型
     * @param typeOrder 类型顺序
     * @param typeNameMapper 类型名称映射
     */
    fun toggleGroup(
        type: AssetsType,
        typeOrder: List<AssetsType>,
        typeNameMapper: (AssetsType) -> String
    ) {
        expandedStates[type] = !(expandedStates[type] ?: true)
        rebuildDisplayList(typeOrder, typeNameMapper)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AssetListItem.GroupHeader -> VIEW_TYPE_HEADER
            is AssetListItem.AssetItem -> VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = AdapterAssetGroupHeaderBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding)
            }

            else -> {
                val binding = AdapterAssetListBinding.inflate(inflater, parent, false)
                ItemViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val item = items[position]) {
            is AssetListItem.GroupHeader -> (holder as HeaderViewHolder).bind(item)
            is AssetListItem.AssetItem -> (holder as ItemViewHolder).bind(item.asset)
        }
    }

    override fun getItemCount(): Int = items.size

    /**
     * ViewHolder基类
     */
    abstract class ViewHolder(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root)

    /**
     * 分组头ViewHolder
     */
    inner class HeaderViewHolder(
        private val binding: AdapterAssetGroupHeaderBinding
    ) : ViewHolder(binding) {

        private var currentType: AssetsType? = null

        init {
            // 点击分组头切换展开状态
            binding.root.setOnClickListener {
                currentType?.let { type ->
                    // 这里不直接调用toggleGroup，而是通过回调让外部处理
                    // 保持职责分离
                    onHeaderClick?.invoke(type)
                }
            }
        }

        fun bind(header: AssetListItem.GroupHeader) {
            currentType = header.type
            binding.tvGroupTitle.text = header.title
            binding.tvCount.text = "${header.count}项"

            // 旋转图标表示展开/折叠状态
            binding.ivExpandIcon.rotation = if (header.expanded) 0f else -90f
        }
    }

    /**
     * 资产项ViewHolder
     */
    inner class ItemViewHolder(
        private val binding: AdapterAssetListBinding
    ) : ViewHolder(binding) {

        private var currentAsset: AssetsModel? = null

        init {
            binding.root.setOnClickListener {
                currentAsset?.let { onItemClick?.invoke(it) }
            }
            binding.root.setOnLongClickListener { view ->
                currentAsset?.let { onItemLongClick?.invoke(it, view) }
                true
            }
        }

        fun bind(asset: AssetsModel) {
            currentAsset = asset

            // 使用IconView组件设置资产图标和名称
            binding.iconViewAsset.apply {
                imageView().setAssetIcon(asset)
                setText(asset.name)
                setTint(false)
            }

            // 货币标签
            binding.tvCurrencyLabel.isVisible = showCurrency
            binding.tvCurrencyLabel.text = asset.currency.name
        }
    }

    // 分组头点击回调
    private var onHeaderClick: ((AssetsType) -> Unit)? = null

    /**
     * 设置分组头点击回调（用于切换展开状态）
     */
    fun setOnHeaderClickListener(listener: (AssetsType) -> Unit) = apply {
        onHeaderClick = listener
    }

    /**
     * DiffUtil回调
     */
    private class ItemDiffCallback(
        private val oldList: List<AssetListItem>,
        private val newList: List<AssetListItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
            val oldItem = oldList[oldPos]
            val newItem = newList[newPos]
            return when {
                oldItem is AssetListItem.GroupHeader && newItem is AssetListItem.GroupHeader ->
                    oldItem.type == newItem.type

                oldItem is AssetListItem.AssetItem && newItem is AssetListItem.AssetItem ->
                    oldItem.asset.id == newItem.asset.id

                else -> false
            }
        }

        override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
            return oldList[oldPos] == newList[newPos]
        }
    }
}

