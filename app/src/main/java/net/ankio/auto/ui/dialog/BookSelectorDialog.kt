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

package net.ankio.auto.ui.dialog

import android.app.Activity
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import net.ankio.auto.databinding.ComponentBookBinding
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.api.bindAs
import net.ankio.auto.ui.fragment.book.BookComponent
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BookNameModel

/**
 * 账本选择对话框
 *
 * 该对话框用于选择账本，基于BookComponent组件实现，提供以下功能：
 * - 显示所有可用账本列表
 * - 支持选择按钮显示控制
 * - 自动排序和状态管理
 * - 点击选择账本并回调
 *
 * 使用方式：
 * ```kotlin
 * // Activity中使用
 * BookSelectorDialog.create(activity)
 *     .setShowSelect(true)
 *     .setCallback { book, type -> }
 *     .show()
 *
 * // Fragment中使用
 * BookSelectorDialog.create(fragment)
 *     .setCallback { book, type -> }
 *     .show()
 *
 * // Service中使用（悬浮窗）
 * BookSelectorDialog.create(service)
 *     .setCallback { book, type -> }
 *     .show()
 * ```
 */
class BookSelectorDialog private constructor(
    context: android.content.Context,
    lifecycleOwner: LifecycleOwner?,
    isOverlay: Boolean
) : BaseSheetDialog<ComponentBookBinding>(context, lifecycleOwner, isOverlay) {

    private var showSelect: Boolean = false
    private var callback: ((BookNameModel, BillType) -> Unit)? = null
    private lateinit var bookComponent: BookComponent

    /**
     * 设置是否显示选择按钮
     * @param show 是否显示选择按钮
     * @return 当前对话框实例，支持链式调用
     */
    fun setShowSelect(show: Boolean) = apply {
        this.showSelect = show
        if (::bookComponent.isInitialized) {
            bookComponent.setShowOption(show, false)
        }
    }

    /**
     * 设置账本选择回调
     * @param callback 选择账本后的回调函数
     * @return 当前对话框实例，支持链式调用
     */
    fun setCallback(callback: (BookNameModel, BillType) -> Unit) = apply {
        this.callback = callback
    }

    override fun onViewCreated(view: View?) {
        super.onViewCreated(view)

        // 使用bindAs创建BookComponent实例
        bookComponent = binding.bindAs(lifecycleOwner!!.lifecycle)

        // 设置是否显示选择按钮
        bookComponent.setShowOption(showSelect, false)

        // 设置账本选择回调
        bookComponent.setOnBookSelectedListener { selectedBook, billType ->
            callback?.invoke(selectedBook, BillType.valueOf(billType))
            this.dismiss()
        }
    }

    /**
     * 刷新账本数据
     */
    fun refreshData() {
        if (::bookComponent.isInitialized) {
            bookComponent.refreshData()
        }
    }

    /**
     * 获取当前账本列表
     */
    fun getBookList(): List<BookNameModel> {
        return if (::bookComponent.isInitialized) {
            bookComponent.getDataItems()
        } else {
            emptyList()
        }
    }

    companion object {
        /**
         * 从Activity创建账本选择对话框
         * @param activity 宿主Activity
         * @return 对话框实例
         */
        fun create(activity: Activity): BookSelectorDialog {
            return BookSelectorDialog(activity, activity as LifecycleOwner, false)
        }

        /**
         * 从Fragment创建账本选择对话框
         * @param fragment 宿主Fragment
         * @return 对话框实例
         */
        fun create(fragment: Fragment): BookSelectorDialog {
            return BookSelectorDialog(fragment.requireContext(), fragment.viewLifecycleOwner, false)
        }

        /**
         * 从Service创建账本选择对话框（悬浮窗模式）
         * @param service 宿主Service
         * @return 对话框实例
         */
        fun create(service: LifecycleService): BookSelectorDialog {
            return BookSelectorDialog(service, service, true)
        }
    }
}
