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

import android.view.View
import androidx.core.view.isVisible
import net.ankio.auto.databinding.AdapterAssetListBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.utils.setAssetIcon
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.db.model.AssetsModel

/**
 * 资产选择器适配器
 * 提供资产列表的显示和交互功能
 *
 * 设计原则（遵循Linus好品味）：
 * 1. 简洁构造：无参数构造函数
 * 2. 链式配置：通过链式调用设置监听器
 * 3. 消除包装：直接使用父类方法
 */
class AssetSelectorAdapter : BaseAdapter<AdapterAssetListBinding, AssetsModel>() {

    private var onItemClick: ((AssetsModel, View) -> Unit)? = null
    private var onItemLongClick: ((AssetsModel, View) -> Unit)? = null
    private var showCurrency: Boolean = PrefManager.featureMultiCurrency

    /**
     * 设置点击监听器
     */
    fun setOnItemClickListener(listener: (AssetsModel, View) -> Unit) = apply {
        this.onItemClick = listener
    }

    /**
     * 设置长按监听器
     */
    fun setOnItemLongClickListener(listener: (AssetsModel, View) -> Unit) = apply {
        this.onItemLongClick = listener
    }

    /**
     * 初始化视图持有者
     */
    override fun onInitViewHolder(holder: BaseViewHolder<AdapterAssetListBinding, AssetsModel>) {
        val binding = holder.binding

        // 设置点击事件
        binding.root.setOnClickListener {
            holder.item?.let { item ->
                onItemClick?.invoke(item, it)
            }
        }

        // 设置长按事件
        binding.root.setOnLongClickListener {
            holder.item?.let { item ->
                onItemLongClick?.invoke(item, it)
            }
            true
        }
    }

    /**
     * 绑定数据到视图
     * 使用 IconView 组件设置资产图标和名称，使用标签风格显示货币信息
     *
     * @param holder 视图持有者
     * @param data 资产数据模型
     * @param position 位置索引
     */
    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterAssetListBinding, AssetsModel>,
        data: AssetsModel,
        position: Int
    ) {
        val binding = holder.binding
        // 使用 IconView 组件设置资产图标和名称
        binding.iconViewAsset.apply {
            // 设置资产图标
            imageView().setAssetIcon(data)
            // 设置显示文本（名称 + 类型信息）
            setText(data.name)
            // 禁用图标着色，保持原始颜色
            setTint(false)
        }

        // 根据外部传参决定是否展示货币
        binding.tvCurrencyLabel.isVisible = showCurrency
        // 设置货币标签
        binding.tvCurrencyLabel.text = data.currency.name
    }

    /**
     * 判断两个资产项是否为同一项
     * 用于 DiffUtil 计算差异
     */
    override fun areItemsSame(oldItem: AssetsModel, newItem: AssetsModel): Boolean {
        return oldItem.id == newItem.id
    }

    /**
     * 判断两个资产项的内容是否相同
     * 用于 DiffUtil 计算差异
     */
    override fun areContentsSame(oldItem: AssetsModel, newItem: AssetsModel): Boolean {
        return oldItem == newItem
    }


}