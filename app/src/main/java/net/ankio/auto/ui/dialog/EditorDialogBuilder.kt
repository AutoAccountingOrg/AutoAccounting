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
import android.app.Service
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import net.ankio.auto.R
import net.ankio.auto.databinding.DialogBottomSheetBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseSheetDialog
import kotlin.properties.Delegates


class EditorDialogBuilder : BottomSheetDialogBuilder {

    private var inputTypeInt by Delegates.notNull<Int>()

    private fun initType(inputType: Int) {
        this.inputTypeInt = inputType
    }

    /**
     * 使用Activity上下文构造底部弹窗构建器
     * @param activity Activity实例
     */
    constructor(activity: Activity, inputType: Int = InputType.TYPE_CLASS_TEXT) : super(activity) {
        initType(inputType)
    }

    /**
     * 使用Fragment上下文构造底部弹窗构建器
     * @param fragment Fragment实例
     */
    constructor(fragment: Fragment, inputType: Int = InputType.TYPE_CLASS_TEXT) : super(fragment) {
        initType(inputType)
    }

    /**
     * 使用Service上下文构造底部弹窗构建器
     * @param service Service实例
     */
    constructor(service: Service, inputType: Int = InputType.TYPE_CLASS_TEXT) : super(service) {
        initType(inputType)
    }

    private lateinit var editText: TextInputEditText


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
        text: Int,
        listener: ((dialog: BaseSheetDialog<DialogBottomSheetBinding>, which: Int) -> Unit)?
    ): EditorDialogBuilder {
        super.setNegativeButton(text, listener)
        return this
    }

    override fun setMessage(string: Int): EditorDialogBuilder {
        super.setMessage(string)
        return this
    }
    override fun setMessage(string: String): EditorDialogBuilder {

        // ① 创建 TextInputLayout（父容器就是 LinearLayout，本身继承自它）
        val inputLayout = TextInputLayout(
            ctx,
            null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            id = View.generateViewId()
            // match_parent / wrap_content，带外边距
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                val vPad = ctx.resources.getDimensionPixelSize(R.dimen.one_padding)
                topMargin = vPad
                bottomMargin = vPad

            }
        }

        // ② 创建内层 TextInputEditText —— 关键：一定要用 LinearLayout.LayoutParams
        editText =
            TextInputEditText(ctx, null, com.google.android.material.R.attr.editTextStyle).apply {
            id = View.generateViewId()

                layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

                // 默认内容
            setText(string)
            inputType = inputTypeInt
        }

        // ③ 组装并设置到 Dialog
        inputLayout.addView(editText)   // 把 EditText 放进 TextInputLayout
        setView(inputLayout)            // AlertDialog.Builder 的自定义视图

        return this
    }


    fun setEditorPositiveButton(
        text: Int,
        listener: ((result: String) -> Unit)?
    ): EditorDialogBuilder {
        val buttonText = ctx.getString(text)
        Logger.d("Setting positive button with resource ID $text: '$buttonText'")
        return setEditorPositiveButton(buttonText, listener)
    }

    /**
     * 设置确认按钮
     * @param text 按钮文本
     * @param listener 按钮点击监听器，可为null
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


}