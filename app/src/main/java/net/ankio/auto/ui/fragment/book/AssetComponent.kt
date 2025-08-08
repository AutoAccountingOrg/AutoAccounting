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

package net.ankio.auto.ui.fragment.book

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import kotlinx.coroutines.launch
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
 * @param binding 组件绑定对象
 * @param lifecycle 生命周期
 */
class AssetComponent(
    binding: ComponentAssetBinding,
    private val lifecycle: Lifecycle
) : BaseComponent<ComponentAssetBinding>(binding, lifecycle) {

    // 资产选择回调函数
    private var onAssetSelected: ((AssetsModel?) -> Unit)? = null

    // 长按回调函数
    private var onAssetLongClick: ((AssetsModel, View) -> Unit)? = null

    // 当前选中的资产
    private var selectedAsset: AssetsModel? = null

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
    fun setOnAssetSelectedListener(callback: (AssetsModel?) -> Unit) {
        this.onAssetSelected = callback
    }

    /**
     * 设置长按回调
     * @param callback 回调函数，参数为被长按的资产、位置和视图
     */
    fun setOnAssetLongClickListener(callback: (AssetsModel, View) -> Unit) {
        this.onAssetLongClick = callback
    }

    override fun init() {
        super.init()
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

        // 初始化适配器
        adapter = AssetSelectorAdapter(
            onItemClick = { asset, view ->
                selectedAsset = asset
                onAssetSelected?.invoke(selectedAsset)
            },
            onItemLongClick = { asset, view ->
                onAssetLongClick?.invoke(asset, view)
            },
            showCurrency = true
        )

        recyclerView.adapter = adapter
    }

    /**
     * 加载资产数据
     */
    private fun loadData() {
        binding.statusPage.showLoading()

        lifecycle.coroutineScope.launch {
            try {
                assets.clear()
                val allAssets = AssetsAPI.list()

                // 根据过滤类型集合筛选资产
                val filteredAssets = filterTypes?.let { types ->
                    allAssets.filter { it.type in types }
                } ?: allAssets

                assets.addAll(filteredAssets)
                adapter.updateAssets(assets)

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
    fun refreshData() {
        loadData()
    }

    /**
     * 获取当前选中的资产
     * @return 选中的资产，如果没有选中则返回null
     */
    fun getSelectedAsset(): AssetsModel? {
        return selectedAsset
    }

    /**
     * 设置选中的资产
     * @param asset 要选中的资产
     */
    fun setSelectedAsset(asset: AssetsModel?) {
        selectedAsset = asset
        onAssetSelected?.invoke(selectedAsset)
    }

    /**
     * 获取当前资产列表
     * @return 当前的资产数据列表
     */
    fun getAssets(): List<AssetsModel> {
        return assets.toList()
    }

    /**
     * 清除选择
     */
    fun clearSelection() {
        selectedAsset = null
        onAssetSelected?.invoke(null)
    }

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