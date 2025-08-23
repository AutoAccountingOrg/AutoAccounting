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
import net.ankio.auto.databinding.DialogBottomSheetBinding
import net.ankio.auto.databinding.SettingItemInputBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseSheetDialog

/**
 * 编辑器对话框构建器
 *
 * 继承自 BottomSheetDialogBuilder，专门用于文本输入场景
 *
 * 使用方式：
 * ```kotlin
 * EditorDialogBuilder.create(activity)
 *     .setInputType(InputType.TYPE_CLASS_NUMBER)
 *     .setTitle("输入数量")
 *     .setMessage("请输入数量")
 *     .setEditorPositiveButton("确认") { result ->
 *         // 处理输入结果
 *     }
 *     .show()
 * ```
 */
class EditorDialogBuilder private constructor(
    context: android.content.Context,
    lifecycleOwner: LifecycleOwner?,
    isOverlay: Boolean,
    private var inputTypeInt: Int = InputType.TYPE_CLASS_TEXT
) : BottomSheetDialogBuilder(context, lifecycleOwner, isOverlay) {

    private lateinit var editText: TextInputEditText

    /**
     * 设置输入类型
     * @param inputType 输入类型，如 InputType.TYPE_CLASS_NUMBER
     * @return 当前构建器实例，支持链式调用
     */
    fun setInputType(inputType: Int) = apply {
        this.inputTypeInt = inputType
        if (::editText.isInitialized) {
            editText.inputType = inputType
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
        editText.inputType = inputTypeInt

        // 将视图设置到对话框
        addCustomView(inputLayout)
        return this
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
        Logger.d("Setting positive button with resource ID $text: '$buttonText'")
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
        Logger.d("Setting positive button: '$text'")
        binding.positiveButton.text = text
        binding.positiveButton.setOnClickListener {
            Logger.d("Positive button clicked: '$text'")
            if (listener != null) {
                Logger.d("Executing positive button listener")
                listener(editText.text.toString())
            } else {
                Logger.d("No positive button listener provided")
            }
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

    companion object {
        /**
         * 从Activity创建编辑器对话框构建器
         * @param activity 宿主Activity
         * @param inputType 输入类型，默认为文本类型
         * @return 构建器实例
         */
        fun create(
            activity: Activity,
            inputType: Int = InputType.TYPE_CLASS_TEXT
        ): EditorDialogBuilder {
            return EditorDialogBuilder(activity, activity as LifecycleOwner, false, inputType)
        }

        /**
         * 从Fragment创建编辑器对话框构建器
         * @param fragment 宿主Fragment
         * @param inputType 输入类型，默认为文本类型
         * @return 构建器实例
         */
        fun create(
            fragment: Fragment,
            inputType: Int = InputType.TYPE_CLASS_TEXT
        ): EditorDialogBuilder {
            return EditorDialogBuilder(
                fragment.requireContext(),
                fragment.viewLifecycleOwner,
                false,
                inputType
            )
        }

        /**
         * 从Service创建编辑器对话框构建器（悬浮窗模式）
         * @param service 宿主Service
         * @param inputType 输入类型，默认为文本类型
         * @return 构建器实例
         */
        fun create(
            service: LifecycleService,
            inputType: Int = InputType.TYPE_CLASS_TEXT
        ): EditorDialogBuilder {
            return EditorDialogBuilder(service, service, true, inputType)
        }
    }
}