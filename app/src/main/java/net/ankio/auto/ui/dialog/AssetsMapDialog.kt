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
 *   limitations under the License.
 */

package net.ankio.auto.ui.dialog

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.DialogMapBinding
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.ui.utils.setAssetIcon
import net.ankio.auto.ui.utils.setAssetIconByName
import org.ezbook.server.db.model.AssetsMapModel

/**
 * 资产映射对话框
 *
 * 该对话框用于创建或编辑资产映射关系，提供以下功能：
 * - 输入原始资产名称
 * - 选择映射的目标资产
 * - 设置是否为正则表达式匹配
 * - 保存或取消操作
 *
 * 支持三种构造方式：
 * - Activity构造：传入Activity实例
 * - Fragment构造：传入Fragment实例
 * - Service构造：传入LifecycleService实例（悬浮窗模式）
 */
class AssetsMapDialog : BaseSheetDialog<DialogMapBinding> {

    private val assetsMapModel: AssetsMapModel
    private val onClose: (AssetsMapModel) -> Unit
    private val context: Context

    // 保存宿主对象引用以便创建子对话框
    private val hostActivity: Activity?
    private val hostFragment: Fragment?
    private val hostService: LifecycleService?

    /**
     * 使用Activity构造资产映射对话框
     * @param activity 宿主Activity
     * @param assetsMapModel 要编辑的资产映射模型，默认为新建
     * @param onClose 保存完成后的回调函数
     */
    constructor(
        activity: Activity,
        assetsMapModel: AssetsMapModel = AssetsMapModel(),
        onClose: (AssetsMapModel) -> Unit
    ) : super(activity) {
        this.assetsMapModel = assetsMapModel
        this.onClose = onClose
        this.context = activity
        this.hostActivity = activity
        this.hostFragment = null
        this.hostService = null
    }

    /**
     * 使用Fragment构造资产映射对话框
     * @param fragment 宿主Fragment
     * @param assetsMapModel 要编辑的资产映射模型，默认为新建
     * @param onClose 保存完成后的回调函数
     */
    constructor(
        fragment: Fragment,
        assetsMapModel: AssetsMapModel = AssetsMapModel(),
        onClose: (AssetsMapModel) -> Unit
    ) : super(fragment) {
        this.assetsMapModel = assetsMapModel
        this.onClose = onClose
        this.context = fragment.requireContext()
        this.hostActivity = null
        this.hostFragment = fragment
        this.hostService = null
    }

    /**
     * 使用LifecycleService构造资产映射对话框（悬浮窗模式）
     * @param service 宿主Service
     * @param assetsMapModel 要编辑的资产映射模型，默认为新建
     * @param onClose 保存完成后的回调函数
     */
    constructor(
        service: LifecycleService,
        assetsMapModel: AssetsMapModel = AssetsMapModel(),
        onClose: (AssetsMapModel) -> Unit
    ) : super(service) {
        this.assetsMapModel = assetsMapModel
        this.onClose = onClose
        this.context = service
        this.hostActivity = null
        this.hostFragment = null
        this.hostService = service
    }

    override fun onViewCreated(view: View?) {
        super.onViewCreated(view)
        initializeView()
        setupEventListeners()
    }

    /**
     * 初始化视图
     * 如果是编辑模式，则填充现有数据
     */
    private fun initializeView() {
        if (assetsMapModel.id != 0L) {
            setBindingData()
        }
    }

    /**
     * 设置绑定数据到视图
     */
    private fun setBindingData() = with(binding) {
        raw.setText(assetsMapModel.name)
        if (assetsMapModel.mapName.isEmpty()) {
            target.setText(context.getString(R.string.map_target))
        } else {
            target.setText(assetsMapModel.mapName)
        }
        regex.isChecked = assetsMapModel.regex

        lifecycleScope.launch {
            target.imageView().setAssetIconByName(assetsMapModel.mapName)
        }
    }

    /**
     * 设置事件监听器
     */
    private fun setupEventListeners() = with(binding) {
        buttonCancel.setOnClickListener { dismiss() }
        buttonSure.setOnClickListener { handleSaveAction() }
        target.setOnClickListener { showAssetSelector() }
    }

    /**
     * 处理保存操作
     */
    private fun handleSaveAction() = with(binding) {
        val name = raw.text.toString()
        val mapName = target.getText()

        if (name.isEmpty() || mapName == context.getString(R.string.map_target)) {
            ToastUtils.error(context.getString(R.string.map_empty))
        } else {
            assetsMapModel.apply {
                this.name = name
                this.mapName = mapName
                this.regex = binding.regex.isChecked
            }
            lifecycleScope.launch {
                onClose(assetsMapModel)
                dismiss()
            }
        }
    }

    /**
     * 显示资产选择器
     */
    private fun showAssetSelector() {
        val selectorDialog = when {
            hostActivity != null -> AssetsSelectorDialog(hostActivity) { asset ->
                updateSelectedAsset(asset)
            }

            hostFragment != null -> AssetsSelectorDialog(hostFragment) { asset ->
                updateSelectedAsset(asset)
            }

            hostService != null -> AssetsSelectorDialog(hostService) { asset ->
                updateSelectedAsset(asset)
            }

            else -> {
                // 不应该到达这里
                return
            }
        }
        selectorDialog.show()
    }

    /**
     * 更新选中的资产
     */
    private fun updateSelectedAsset(asset: org.ezbook.server.db.model.AssetsModel) {
        assetsMapModel.mapName = asset.name
        binding.target.setText(asset.name)

        lifecycleScope.launch {
            binding.target.imageView().setAssetIcon(asset)
        }
    }
}