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

import android.view.View
import net.ankio.auto.databinding.ComponentBookBinding
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.api.bindAs
import net.ankio.auto.ui.fragment.components.BookComponent
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
 * BaseSheetDialog.create<BookSelectorDialog>(context)
 *     .setShowSelect(true)
 *     .setCallback { book, type -> }
 *     .show()
 * ```
 */
class BookSelectorDialog internal constructor(
    context: android.content.Context
) : BaseSheetDialog<ComponentBookBinding>(context) {

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
        bookComponent = binding.bindAs()

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

    /**
     * 弹窗销毁时清理回调引用，防止内存泄漏
     */
    override fun onDialogDestroy() {
        super.onDialogDestroy()
        // 清理回调引用，防止持有 Fragment 引用导致内存泄漏
        callback = null
    }
}
