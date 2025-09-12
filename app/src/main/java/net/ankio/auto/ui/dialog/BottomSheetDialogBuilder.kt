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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import net.ankio.auto.databinding.DialogBottomSheetBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.utils.toThemeColor
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * 底部弹窗构建器
 *
 * 提供链式调用的API来构建和配置底部弹窗对话框。
 * 支持设置标题、消息、按钮和自定义视图。
 *
 * 使用示例：
 * ```
 * BaseSheetDialog.create<BottomSheetDialogBuilder>(context)
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
open class BottomSheetDialogBuilder internal constructor(
    context: android.content.Context
) : BaseSheetDialog<DialogBottomSheetBinding>(context) {

    init {
        // 默认隐藏所有UI元素，等待后续配置
        binding.title.visibility = View.GONE
        binding.positiveButton.visibility = View.GONE
        binding.negativeButton.visibility = View.GONE
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
        binding.title.text = titleText
        binding.title.visibility = View.VISIBLE
        return this
    }

    /**
     * 设置消息内容
     * @param message 消息文本
     * @return 当前构建器实例，支持链式调用
     */
    open fun setMessage(message: String): BottomSheetDialogBuilder {
        logger.debug { "Setting message: '$message'" }
        // 确保消息容器存在
        ensureMessageContainer()
        val messageView = createMessageTextView(message)
        binding.container.addView(messageView)
        return this
    }

    /**
     * 设置消息内容（使用字符串资源ID）
     * @param messageId 字符串资源ID
     * @return 当前构建器实例，支持链式调用
     */
    open fun setMessage(messageId: Int): BottomSheetDialogBuilder {
        val message = ctx.getString(messageId)
        return setMessage(message)
    }

    /**
     * 设置正面按钮（确认按钮）
     * @param text 按钮文本
     * @param listener 点击监听器
     * @return 当前构建器实例，支持链式调用
     */
    open fun setPositiveButton(
        text: String,
        listener: ((dialog: BaseSheetDialog<DialogBottomSheetBinding>, which: Int) -> Unit)?
    ): BottomSheetDialogBuilder {
        logger.debug { "Setting positive button: '$text'" }
        binding.positiveButton.text = text
        binding.positiveButton.visibility = View.VISIBLE
        binding.positiveButton.setOnClickListener {
            listener?.invoke(this, 0)
            dismiss()
        }
        return this
    }

    /**
     * 设置正面按钮（使用字符串资源ID）
     * @param textId 按钮文本资源ID
     * @param listener 点击监听器
     * @return 当前构建器实例，支持链式调用
     */
    open fun setPositiveButton(
        textId: Int,
        listener: ((dialog: BaseSheetDialog<DialogBottomSheetBinding>, which: Int) -> Unit)?
    ): BottomSheetDialogBuilder {
        val text = ctx.getString(textId)
        return setPositiveButton(text, listener)
    }

    /**
     * 设置负面按钮（取消按钮）
     * @param text 按钮文本
     * @param listener 点击监听器
     * @return 当前构建器实例，支持链式调用
     */
    open fun setNegativeButton(
        text: String,
        listener: ((dialog: BaseSheetDialog<DialogBottomSheetBinding>, which: Int) -> Unit)?
    ): BottomSheetDialogBuilder {
        logger.debug { "Setting negative button: '$text'" }
        binding.negativeButton.text = text
        binding.negativeButton.visibility = View.VISIBLE
        binding.negativeButton.setOnClickListener {
            listener?.invoke(this, 1)
            dismiss()
        }
        return this
    }

    /**
     * 设置负面按钮（使用字符串资源ID）
     * @param textId 按钮文本资源ID
     * @param listener 点击监听器
     * @return 当前构建器实例，支持链式调用
     */
    open fun setNegativeButton(
        textId: Int,
        listener: ((dialog: BaseSheetDialog<DialogBottomSheetBinding>, which: Int) -> Unit)?
    ): BottomSheetDialogBuilder {
        val text = ctx.getString(textId)
        return setNegativeButton(text, listener)
    }

    /**
     * 添加自定义视图到消息容器
     * @param view 要添加的自定义视图
     * @return 当前构建器实例，支持链式调用
     */
    open fun addCustomView(view: View): BottomSheetDialogBuilder {
        logger.debug { "Adding custom view: ${view.javaClass.simpleName}" }
        ensureMessageContainer()
        binding.container.addView(view)
        return this
    }

    /**
     * 确保消息容器存在并可见
     */
    private fun ensureMessageContainer() {
        binding.container.visibility = View.VISIBLE
    }

    /**
     * 创建消息文本视图
     * @param message 消息内容
     * @return 配置好的TextView
     */
    private fun createMessageTextView(message: String): TextView {
        return TextView(ctx).apply {
            text = message
            textSize = 16f
            setPadding(0, 16, 0, 16)
            // 设置文本样式
            setTextColor(
                com.google.android.material.R.attr.colorOnBackground.toThemeColor()
            )
        }
    }


}