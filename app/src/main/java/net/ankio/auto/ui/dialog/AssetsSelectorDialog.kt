/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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

package net.ankio.auto.ui.dialog

import android.app.Activity
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import net.ankio.auto.databinding.ComponentAssetBinding
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.api.bindAs
import net.ankio.auto.ui.fragment.book.AssetComponent
import org.ezbook.server.constant.AssetsType
import org.ezbook.server.db.model.AssetsModel

/**
 * 资产选择对话框
 *
 * 该对话框用于选择资产，基于AssetComponent组件实现，提供以下功能：
 * - 显示所有可用资产列表
 * - 支持按资产类型过滤
 * - 自动排序和状态管理
 * - 点击选择资产并回调
 *
 * 支持三种构造方式：
 * - Activity构造：传入Activity实例
 * - Fragment构造：传入Fragment实例
 * - Service构造：传入LifecycleService实例（悬浮窗模式）
 */
class AssetsSelectorDialog : BaseSheetDialog<ComponentAssetBinding> {

    private val filter: List<AssetsType>
    private val callback: (AssetsModel) -> Unit
    private lateinit var assetComponent: AssetComponent
    private val lifecycleOwner: LifecycleOwner

    /**
     * 使用Activity构造资产选择对话框
     * @param activity 宿主Activity
     * @param filter 资产类型过滤列表，为空表示显示所有类型
     * @param callback 选择资产后的回调函数
     */
    constructor(
        activity: Activity,
        filter: List<AssetsType> = emptyList(),
        callback: (AssetsModel) -> Unit
    ) : super(activity) {
        this.filter = filter
        this.callback = callback
        this.lifecycleOwner = activity as LifecycleOwner
    }

    /**
     * 使用Fragment构造资产选择对话框
     * @param fragment 宿主Fragment
     * @param filter 资产类型过滤列表，为空表示显示所有类型
     * @param callback 选择资产后的回调函数
     */
    constructor(
        fragment: Fragment,
        filter: List<AssetsType> = emptyList(),
        callback: (AssetsModel) -> Unit
    ) : super(fragment) {
        this.filter = filter
        this.callback = callback
        this.lifecycleOwner = fragment.viewLifecycleOwner
    }

    /**
     * 使用LifecycleService构造资产选择对话框（悬浮窗模式）
     * @param service 宿主Service
     * @param filter 资产类型过滤列表，为空表示显示所有类型
     * @param callback 选择资产后的回调函数
     */
    constructor(
        service: LifecycleService,
        filter: List<AssetsType> = emptyList(),
        callback: (AssetsModel) -> Unit
    ) : super(service) {
        this.filter = filter
        this.callback = callback
        this.lifecycleOwner = service
    }

    override fun onViewCreated(view: View?) {
        super.onViewCreated(view)
        setupAssetComponent()
    }

    /**
     * 设置资产组件
     */
    private fun setupAssetComponent() {
        // 使用bindAs扩展函数创建AssetComponent实例
        assetComponent = binding.bindAs(lifecycleOwner.lifecycle)

        // 设置资产选择回调
        assetComponent.setOnAssetSelectedListener { asset ->
            asset?.let {
                callback(it)
                dismiss()
            }
        }

        // 如果有过滤条件，设置过滤器
        if (filter.isNotEmpty()) {
            assetComponent.setAssetTypesFilter(filter)
        } else {
            // 没有过滤条件时，刷新数据显示所有资产
            assetComponent.refreshData()
        }
    }
}