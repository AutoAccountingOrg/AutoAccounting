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
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
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
 * 使用方式：
 * ```kotlin
 * AssetsMapDialog.create(activity)
 *     .setAssetsMapModel(existingModel)
 *     .setOnClose { model ->
 *         // 处理保存结果
 *     }
 *     .show()
 * ```
 */
class AssetsMapDialog internal constructor(
    context: android.content.Context
) : BaseSheetDialog<DialogMapBinding>(context) {

    private var assetsMapModel: AssetsMapModel = AssetsMapModel()
    private var onClose: ((AssetsMapModel) -> Unit)? = null

    /**
     * 设置资产映射模型
     * @param model 要编辑的资产映射模型，默认为新建
     * @return 当前对话框实例，支持链式调用
     */
    fun setAssetsMapModel(model: AssetsMapModel) = apply {
        this.assetsMapModel = model
        setBindingData()
    }

    /**
     * 设置保存完成回调
     * @param callback 保存完成后的回调函数
     * @return 当前对话框实例，支持链式调用
     */
    fun setOnClose(callback: (AssetsMapModel) -> Unit) = apply {
        this.onClose = callback
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
            target.setText(ctx.getString(R.string.map_target))
        } else {
            target.setText(assetsMapModel.mapName)
        }
        regex.isChecked = assetsMapModel.regex

        launch {
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

        if (name.isEmpty() || mapName == ctx.getString(R.string.map_target)) {
            ToastUtils.error(ctx.getString(R.string.map_empty))
        } else {
            assetsMapModel.apply {
                this.name = name
                this.mapName = mapName
                this.regex = binding.regex.isChecked
            }
            launch {
                onClose?.invoke(assetsMapModel)
                dismiss()
            }
        }
    }

    /**
     * 显示资产选择器
     */
    private fun showAssetSelector() {
        val selectorDialog = create<AssetsSelectorDialog>(ctx)

        selectorDialog.setCallback { asset ->
            updateSelectedAsset(asset)
        }.show()
    }

    /**
     * 更新选中的资产
     */
    private fun updateSelectedAsset(asset: org.ezbook.server.db.model.AssetsModel) {
        assetsMapModel.mapName = asset.name
        binding.target.setText(asset.name)

        launch {
            binding.target.imageView().setAssetIcon(asset)
        }
    }


}