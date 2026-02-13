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

package net.ankio.auto.ui.fragment.components

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import net.ankio.auto.R
import net.ankio.auto.databinding.ComponentAssetBinding
import net.ankio.auto.http.api.AssetsAPI
import net.ankio.auto.ui.adapter.AssetSelectorAdapter
import net.ankio.auto.ui.api.BaseComponent
import org.ezbook.server.constant.AssetsType
import org.ezbook.server.db.model.AssetsModel

/**
 * 资产选择组件
 * 提供资产列表的显示和选择功能，支持单击和长按事件
 *
 * 设计原则（遵循Linus好品味）：
 * 1. 简化构造：只需要ViewBinding，自动推断生命周期
 * 2. 统一协程管理：使用BaseComponent的launch方法
 * 3. 清晰的职责分离：专注于资产选择逻辑
 *
 * @param binding 组件绑定对象
 */
class AssetComponent(
    binding: ComponentAssetBinding
) : BaseComponent<ComponentAssetBinding>(binding) {

    // 资产选择回调函数
    private var onAssetSelected: ((AssetsModel) -> Unit)? = null

    // 长按回调函数
    private var onAssetLongClick: ((AssetsModel, View) -> Unit)? = null

    // 资产列表
    private var assets = mutableListOf<AssetsModel>()

    // 过滤的资产类型集合；为 null 表示不过滤
    private var filterTypes: Set<AssetsType>? = null

    // RecyclerView适配器
    private lateinit var adapter: AssetSelectorAdapter

    /**
     * 设置资产选择回调
     * @param callback 回调函数，参数为选中的资产
     */
    fun setOnAssetSelectedListener(callback: (AssetsModel) -> Unit) {
        this.onAssetSelected = callback
    }

    /**
     * 设置长按回调
     * @param callback 回调函数，参数为被长按的资产、位置和视图
     */
    fun setOnAssetLongClickListener(callback: (AssetsModel, View) -> Unit) {
        this.onAssetLongClick = callback
    }

    override fun onComponentCreate() {
        super.onComponentCreate()
        setupRecyclerView()
        // 延迟加载数据，等待过滤器设置完成
    }

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        val recyclerView = binding.statusPage.contentView!!

        // 设置布局管理器
        recyclerView.layoutManager = LinearLayoutManager(context)
        (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        // 初始化适配器，设置类型名称映射以显示本地化中文
        adapter = AssetSelectorAdapter()
            .setTypeNameMapper { type ->
                when (type) {
                    AssetsType.NORMAL -> context.getString(R.string.type_normal)
                    AssetsType.CREDIT -> context.getString(R.string.type_credit)
                    AssetsType.BORROWER -> context.getString(R.string.type_borrower)
                    AssetsType.CREDITOR -> context.getString(R.string.type_creditor)
                }
            }
            .setOnItemClickListener { asset, _ ->
                onAssetSelected?.invoke(asset)
            }
            .setOnItemLongClickListener { asset, view ->
                onAssetLongClick?.invoke(asset, view)
            }

        recyclerView.adapter = adapter
    }

    /**
     * 加载资产数据
     */
    private fun loadData() {
        binding.statusPage.showLoading()

        launch {
            try {
                assets.clear()
                val allAssets = AssetsAPI.list()

                // 根据过滤类型集合筛选资产
                val filteredAssets = filterTypes?.let { types ->
                    allAssets.filter { it.type in types }
                } ?: allAssets

                assets.addAll(filteredAssets)
                adapter.updateItems(assets)

                if (assets.isEmpty()) {
                    binding.statusPage.showEmpty()
                } else {
                    binding.statusPage.showContent()
                }
            } catch (e: Exception) {
                binding.statusPage.showError()
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refreshData() = loadData()

    /**
     * 获取当前资产列表
     */
    fun getAssets(): List<AssetsModel> = assets.toList()

    /**
     * 设置资产类型过滤器（单类型，向后兼容）
     * @param assetType 要过滤的资产类型，传 null 表示不过滤
     */
    fun setAssetTypeFilter(assetType: AssetsType?) {
        filterTypes = assetType?.let { setOf(it) }
        loadData()
    }

    /**
     * 设置资产类型过滤器（多类型）
     * @param types 要过滤的资产类型集合，传 null 表示不过滤；传空集合将导致无结果
     */
    fun setAssetTypesFilter(types: Collection<AssetsType>?) {
        filterTypes = types?.toSet()
        loadData()
    }
}