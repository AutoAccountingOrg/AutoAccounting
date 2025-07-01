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
// ② 创建 TextInputLayout
        val inputLayout = TextInputLayout(context).apply {
            id = View.generateViewId()

            // layout_width="match_parent"  layout_height="wrap_content"
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                val vPad = context.resources.getDimensionPixelSize(R.dimen.one_padding)
                topMargin = vPad
                bottomMargin = vPad
            }
        }

        // ③ 创建内部的 TextInputEditText
        editText = TextInputEditText(context).apply {
            id = View.generateViewId()
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            // 你愿意的话可以先填点默认值
            setText(string)
            inputType = inputTypeInt
        }

        // ④ 组合 & 添加到父布局
        inputLayout.addView(editText)   // 把 EditText 放进 TextInputLayout
        setView(inputLayout)
        return this
    }

    fun setEditorPositiveButton(
        text: Int,
        listener: ((result: String) -> Unit)?
    ): EditorDialogBuilder {
        val buttonText = context.getString(text)
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