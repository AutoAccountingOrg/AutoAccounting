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
import android.app.Service
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleService
import net.ankio.auto.databinding.DialogBottomSheetBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseSheetDialog

/**
 * 底部弹窗构建器
 *
 * 提供链式调用的API来构建和配置底部弹窗对话框。
 * 支持设置标题、消息、按钮和自定义视图。
 *
 * 使用示例：
 * ```
 * BottomSheetDialogBuilder(activity)
 *     .setTitle("确认操作")
 *     .setMessage("是否确认执行此操作？")
 *     .setPositiveButton("确认") { dialog, which ->
 *         // 处理确认逻辑
 *     }
 *     .setNegativeButton("取消") { dialog, which ->
 *         // 处理取消逻辑
 *     }
 *     .show()
 * ```
 */
open class BottomSheetDialogBuilder : BaseSheetDialog<DialogBottomSheetBinding> {

    /**
     * 使用Activity上下文构造底部弹窗构建器
     * @param activity Activity实例
     */
    constructor(activity: Activity) : super(activity) {
        Logger.d("BottomSheetDialogBuilder created with Activity: ${activity.javaClass.simpleName}")
    }

    /**
     * 使用Fragment上下文构造底部弹窗构建器
     * @param fragment Fragment实例
     */
    constructor(fragment: Fragment) : super(fragment) {
        Logger.d("BottomSheetDialogBuilder created with Fragment: ${fragment.javaClass.simpleName}")
    }

    /**
     * 使用Service上下文构造底部弹窗构建器
     * @param service Service实例
     */
    constructor(service: LifecycleService) : super(service) {
        Logger.d("BottomSheetDialogBuilder created with Service: ${service.javaClass.simpleName}")
    }

    init {
        // 默认隐藏所有UI元素，等待后续配置
        binding.title.visibility = View.GONE
        binding.positiveButton.visibility = View.GONE
        binding.negativeButton.visibility = View.GONE

        Logger.d("Default UI elements hidden (title, positiveButton, negativeButton)")
    }

    override fun setTitle(titleId: Int) {
        setTitleInt(titleId)
    }
    /**
     * 设置弹窗标题
     * @param title 标题文本
     * @return 当前构建器实例，支持链式调用
     */
    open fun setTitle(title: String): BottomSheetDialogBuilder {
        Logger.d("Setting title: '$title'")
        binding.title.text = title
        binding.title.visibility = View.VISIBLE
        return this
    }

    /**
     * 设置弹窗标题（使用字符串资源ID）
     * @param title 字符串资源ID
     * @return 当前构建器实例，支持链式调用
     */
    open fun setTitleInt(title: Int): BottomSheetDialogBuilder {
        val titleText = ctx.getString(title)
        Logger.d("Setting title from resource ID $title: '$titleText'")
        binding.title.setText(title)
        binding.title.visibility = View.VISIBLE
        return this
    }

    /**
     * 设置确认按钮（使用字符串资源ID）
     * @param text 按钮文本的资源ID
     * @param listener 按钮点击监听器，可为null
     * @return 当前构建器实例，支持链式调用
     */
    open fun setPositiveButton(
        text: Int,
        listener: ((dialog: BaseSheetDialog<DialogBottomSheetBinding>, which: Int) -> Unit)?
    ): BottomSheetDialogBuilder {
        val buttonText = ctx.getString(text)
        Logger.d("Setting positive button with resource ID $text: '$buttonText'")
        return setPositiveButton(buttonText, listener)
    }

    /**
     * 设置确认按钮
     * @param text 按钮文本
     * @param listener 按钮点击监听器，可为null
     * @return 当前构建器实例，支持链式调用
     */
    open fun setPositiveButton(
        text: String,
        listener: ((dialog: BaseSheetDialog<DialogBottomSheetBinding>, which: Int) -> Unit)?
    ): BottomSheetDialogBuilder {
        Logger.d("Setting positive button: '$text'")
        binding.positiveButton.text = text
        binding.positiveButton.setOnClickListener {
            Logger.d("Positive button clicked: '$text'")
            if (listener != null) {
                Logger.d("Executing positive button listener")
                listener(this, 0)
            } else {
                Logger.d("No positive button listener provided")
            }
            dismiss()
        }
        binding.positiveButton.visibility = View.VISIBLE
        return this
    }

    /**
     * 设置取消按钮（使用字符串资源ID）
     * @param text 按钮文本的资源ID
     * @param listener 按钮点击监听器，可为null
     * @return 当前构建器实例，支持链式调用
     */
    open fun setNegativeButton(
        text: Int,
        listener: ((dialog: BaseSheetDialog<DialogBottomSheetBinding>, which: Int) -> Unit)?
    ): BottomSheetDialogBuilder {
        val buttonText = ctx.getString(text)
        Logger.d("Setting negative button with resource ID $text: '$buttonText'")
        return setNegativeButton(buttonText, listener)
    }

    /**
     * 设置取消按钮
     * @param text 按钮文本
     * @param listener 按钮点击监听器，可为null
     * @return 当前构建器实例，支持链式调用
     */
    open fun setNegativeButton(
        text: String,
        listener: ((dialog: BaseSheetDialog<DialogBottomSheetBinding>, which: Int) -> Unit)?
    ): BottomSheetDialogBuilder {
        Logger.d("Setting negative button: '$text'")
        binding.negativeButton.text = text
        binding.negativeButton.setOnClickListener {
            Logger.d("Negative button clicked: '$text'")
            if (listener != null) {
                Logger.d("Executing negative button listener")
                listener(this, 0)
            } else {
                Logger.d("No negative button listener provided")
            }
            dismiss()
        }
        binding.negativeButton.visibility = View.VISIBLE
        return this
    }

    /**
     * 设置自定义视图
     * 将指定的视图添加到弹窗的内容容器中
     * @param view 要添加的自定义视图
     * @return 当前构建器实例，支持链式调用
     */
    fun setView(view: View): BottomSheetDialogBuilder {
        Logger.d("Setting custom view: ${view.javaClass.simpleName}")
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        view.layoutParams = layoutParams
        binding.container.addView(view)
        Logger.d("Custom view added to container successfully")
        return this
    }

    /**
     * 设置弹窗消息
     * 创建一个TextView来显示消息文本
     * @param string 消息文本
     * @return 当前构建器实例，支持链式调用
     */
    open fun setMessage(string: String): BottomSheetDialogBuilder {
        Logger.d(
            "Setting message: '${
                if (string.length > 50) string.substring(
                    0,
                    50
                ) + "..." else string
            }'"
        )
        val textView = TextView(ctx)
        textView.text = string
        textView.setPadding(0, 0, 0, 0)
        textView.textSize = 16f
        setView(textView)
        return this
    }

    /**
     * 设置弹窗消息（使用字符串资源ID）
     * @param string 字符串资源ID
     * @return 当前构建器实例，支持链式调用
     */
    open fun setMessage(string: Int): BottomSheetDialogBuilder {
        val messageText = ctx.getString(string)
        Logger.d(
            "Setting message from resource ID $string: '${
                if (messageText.length > 50) messageText.substring(
                    0,
                    50
                ) + "..." else messageText
            }'"
        )
        return setMessage(messageText)
    }
}