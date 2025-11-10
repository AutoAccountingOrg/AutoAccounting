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

package net.ankio.auto.ui.dialog

import android.app.Activity
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import com.google.android.material.textfield.TextInputEditText
import net.ankio.auto.BuildConfig
import net.ankio.auto.databinding.DialogBottomSheetBinding
import net.ankio.auto.databinding.SettingItemInputBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.utils.PrefManager

/**
 * 编辑器对话框构建器
 *
 * 继承自 BottomSheetDialogBuilder，专门用于文本输入场景
 *
 * 使用方式：
 * ```kotlin
 * BaseSheetDialog.create<EditorDialogBuilder>(context)
 *     .setInputType(InputType.TYPE_CLASS_NUMBER)
 *     .setTitle("输入数量")
 *     .setMessage("请输入数量")
 *     .setEditorPositiveButton("确认") { result ->
 *         // 处理输入结果
 *     }
 *     .show()
 * ```
 */
class EditorDialogBuilder internal constructor(
    context: android.content.Context
) : BottomSheetDialogBuilder(context) {

    private var inputTypeInt: Int = InputType.TYPE_CLASS_TEXT
    private var isMultiLine: Boolean = false
    private var minLines: Int = 1
    private var maxLines: Int = Int.MAX_VALUE

    private lateinit var editText: TextInputEditText

    /**
     * 设置输入类型
     * @param inputType 输入类型，如 InputType.TYPE_CLASS_NUMBER
     * @return 当前构建器实例，支持链式调用
     */
    fun setInputType(inputType: Int) = apply {
        this.inputTypeInt = inputType
        if (::editText.isInitialized) {
            // 调试模式下，如果为密码输入类型，则切换为明文输入类型，便于调试
            // 非调试模式保持原样，确保不破坏用户空间（Never break userspace）
            editText.inputType = adjustInputTypeForDebug(inputType)
        }
    }

    /**
     * 设置多行输入模式
     * @param multiLine 是否为多行模式，默认为 true
     * @param minLines 最小行数，默认为 8
     * @param maxLines 最大行数，默认为 Int.MAX_VALUE（无限制）
     * @return 当前构建器实例，支持链式调用
     */
    fun setMultiLine(multiLine: Boolean = true, minLines: Int = 8, maxLines: Int = Int.MAX_VALUE) =
        apply {
            this.isMultiLine = multiLine
            this.minLines = minLines
            this.maxLines = maxLines
            if (::editText.isInitialized) {
                applyMultiLineSettings()
            }
        }

    override fun setTitle(title: String): EditorDialogBuilder {
        super.setTitle(title)
        return this
    }

    override fun setTitleInt(title: Int): EditorDialogBuilder {
        super.setTitleInt(title)
        return this
    }

    override fun setNegativeButton(
        text: String,
        listener: ((dialog: BaseSheetDialog<DialogBottomSheetBinding>, which: Int) -> Unit)?
    ): EditorDialogBuilder {
        super.setNegativeButton(text, listener)
        return this
    }

    override fun setNegativeButton(
        textId: Int,
        listener: ((dialog: BaseSheetDialog<DialogBottomSheetBinding>, which: Int) -> Unit)?
    ): EditorDialogBuilder {
        super.setNegativeButton(textId, listener)
        return this
    }

    override fun setMessage(messageId: Int): EditorDialogBuilder {
        super.setMessage(messageId)
        return this
    }

    override fun setMessage(message: String): EditorDialogBuilder {
        // 使用现有的布局资源 setting_item_input 来构建输入框
        val view = SettingItemInputBinding.inflate(LayoutInflater.from(ctx))
        val inputLayout = view.inputLayout
        editText = view.input

        // 预填内容与输入类型
        editText.setText(message)
        // 调试模式下，如果为密码输入类型，则切换为明文输入类型；否则按原样设置
        editText.inputType = adjustInputTypeForDebug(inputTypeInt)

        // 应用多行设置
        applyMultiLineSettings()

        // 将视图设置到对话框
        addCustomView(inputLayout)
        return this
    }

    /**
     * 应用多行设置到 EditText
     */
    private fun applyMultiLineSettings() {
        if (isMultiLine) {
            // 设置多行输入类型
            val multiLineInputType = (editText.inputType and InputType.TYPE_MASK_CLASS) or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    (editText.inputType and InputType.TYPE_MASK_VARIATION)
            editText.inputType = adjustInputTypeForDebug(multiLineInputType)

            // 设置行数
            editText.minLines = minLines
            editText.maxLines = maxLines

            // 设置垂直对齐方式，让光标从顶部开始
            editText.gravity = android.view.Gravity.TOP or android.view.Gravity.START
        }
    }

    /**
     * 设置编辑器确认按钮（使用资源ID）
     * @param text 按钮文本资源ID
     * @param listener 按钮点击监听器，返回输入的文本
     * @return 当前构建器实例，支持链式调用
     */
    fun setEditorPositiveButton(
        text: Int,
        listener: ((result: String) -> Unit)?
    ): EditorDialogBuilder {
        val buttonText = ctx.getString(text)
        return setEditorPositiveButton(buttonText, listener)
    }

    /**
     * 设置编辑器确认按钮
     * @param text 按钮文本
     * @param listener 按钮点击监听器，返回输入的文本
     * @return 当前构建器实例，支持链式调用
     */
    fun setEditorPositiveButton(
        text: String,
        listener: ((result: String) -> Unit)?
    ): EditorDialogBuilder {
        binding.positiveButton.text = text
        binding.positiveButton.setOnClickListener {
            listener?.invoke(editText.text.toString())
            dismiss()
        }
        binding.positiveButton.visibility = View.VISIBLE
        return this
    }

    /**
     * 获取输入文本
     * @return 当前输入框中的文本
     */
    fun getText(): String {
        return if (::editText.isInitialized) editText.text.toString() else ""
    }


}

/**
 * 工具方法区域：与输入类型相关的判断与调整
 */
private fun adjustInputTypeForDebug(original: Int): Int {
    // 非调试构建直接返回，避免任何行为差异
    if (!PrefManager.debugMode) return original

    // 仅对密码类型进行明文化处理，其他类型保持不变
    if (!isPasswordInputType(original)) return original

    val classMask = original and InputType.TYPE_MASK_CLASS
    return when (classMask) {
        // 文本密码改为可见密码变体，保持文本键盘体验
        InputType.TYPE_CLASS_TEXT -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        // 数字密码改为纯数字类，显示明文数字
        InputType.TYPE_CLASS_NUMBER -> InputType.TYPE_CLASS_NUMBER
        else -> original
    }
}

/**
 * 判断是否为密码输入类型（仅识别需要被隐藏的密码变体）
 * - 文本：TYPE_TEXT_VARIATION_PASSWORD / TYPE_TEXT_VARIATION_WEB_PASSWORD
 * - 数字：TYPE_NUMBER_VARIATION_PASSWORD
 */
private fun isPasswordInputType(inputType: Int): Boolean {
    val classMask = inputType and InputType.TYPE_MASK_CLASS
    val variationMask = inputType and InputType.TYPE_MASK_VARIATION
    return when (classMask) {
        InputType.TYPE_CLASS_TEXT -> variationMask == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variationMask == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD

        InputType.TYPE_CLASS_NUMBER -> variationMask == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        else -> false
    }
}