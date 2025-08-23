/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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

package net.ankio.auto.ui.dialog.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import net.ankio.auto.databinding.ComponentActionButtonsBinding

/**
 * 操作按钮组件 - 专用于账单编辑对话框
 *
 * 职责：
 * - 提供取消和确认操作按钮
 * - 处理按钮点击事件
 *
 * 使用方式：
 * ```kotlin
 * actionButtons.setOnCancelClickListener {
 *     // 处理取消逻辑
 * }
 * actionButtons.setOnConfirmClickListener {
 *     // 处理确认逻辑
 * }
 * ```
 */
class ActionButtonsComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ComponentActionButtonsBinding =
        ComponentActionButtonsBinding.inflate(LayoutInflater.from(context), this)
    private var onCancelClickListener: (() -> Unit)? = null
    private var onConfirmClickListener: (() -> Unit)? = null

    private lateinit var lifecycleOwner: androidx.lifecycle.LifecycleOwner
    private lateinit var billInfoModel: org.ezbook.server.db.model.BillInfoModel

    init {
        orientation = VERTICAL
        setupClickListeners()
    }

    /**
     * 统一初始化方法 - 参考BookHeaderComponent.initBillInfo
     */
    fun initBillInfo(
        billInfoModel: org.ezbook.server.db.model.BillInfoModel,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner
    ) {
        this.lifecycleOwner = lifecycleOwner
        this.billInfoModel = billInfoModel
        refresh()
    }

    /**
     * 刷新显示 - 根据当前账单信息更新UI
     */
    fun refresh() {
        // 操作按钮组件通常不需要根据数据刷新UI
        // 保持方法以维持一致性
    }

    /**
     * 设置取消按钮点击监听器
     *
     * @param listener 取消点击回调
     */
    fun setOnCancelClickListener(listener: () -> Unit) {
        onCancelClickListener = listener
    }

    /**
     * 设置确认按钮点击监听器
     *
     * @param listener 确认点击回调
     */
    fun setOnConfirmClickListener(listener: () -> Unit) {
        onConfirmClickListener = listener
    }


    /**
     * 设置点击事件监听器
     */
    private fun setupClickListeners() {
        binding.cancelButton.setOnClickListener {
            onCancelClickListener?.invoke()
        }

        binding.confirmButton.setOnClickListener {
            onConfirmClickListener?.invoke()
        }
    }
}
