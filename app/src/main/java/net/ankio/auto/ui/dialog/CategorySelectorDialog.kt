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

import android.view.View
import net.ankio.auto.R
import net.ankio.auto.databinding.DialogCategorySelectBinding
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.api.bindAs
import net.ankio.auto.ui.fragment.components.CategoryComponent
import net.ankio.auto.ui.utils.ToastUtils
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.CategoryModel

/**
 * 分类选择对话框
 * 
 * 使用CategoryComponent来展示分类选择功能
 *
 * 使用方式：
 * ```kotlin
 * BaseSheetDialog.create<CategorySelectorDialog>(context)
 *     .setBook("我的账本")
 *     .setType(BillType.Income)
 *     .setCallback { parent, child ->
 *         // 处理分类选择结果
 *     }
 *     .show()
 * ```
 */
class CategorySelectorDialog internal constructor(
    context: android.content.Context
) : BaseSheetDialog<DialogCategorySelectBinding>(context) {

    private lateinit var categoryComponent: CategoryComponent

    private var book: String = ""
    private var type: BillType = BillType.Expend
    private var callback: ((CategoryModel?, CategoryModel?) -> Unit)? = null

    private var parentCategory: CategoryModel? = null
    private var childCategory: CategoryModel? = null

    /**
     * 设置账本名称
     * @param bookID 账本ID
     * @return 当前对话框实例，支持链式调用
     */
    fun setBook(bookID: String) = apply {
        this.book = bookID
        if (::categoryComponent.isInitialized) {
            categoryComponent.setBookInfo(bookID, type)
        }
    }


    /**
     * 设置账单类型
     * @param type 账单类型（收入/支出）
     * @return 当前对话框实例，支持链式调用
     */
    fun setType(type: BillType) = apply {
        this.type = type
        if (::categoryComponent.isInitialized) {
            categoryComponent.setBookInfo(book, type)
        }
    }

    /**
     * 设置分类选择回调
     * @param callback 分类选择后的回调函数
     * @return 当前对话框实例，支持链式调用
     */
    fun setCallback(callback: (CategoryModel?, CategoryModel?) -> Unit) = apply {
        this.callback = callback
    }

    override fun onViewCreated(view: View?) {
        super.onViewCreated(view)

        // 使用bindAs创建CategoryComponent实例
        categoryComponent = binding.category.bindAs()

        // 设置账本信息
        categoryComponent.setBookInfo(book, type)

        // 设置分类选择回调
        categoryComponent.setOnCategorySelectedListener { parent, child ->
            parentCategory = parent
            childCategory = child
        }

        binding.button.setOnClickListener {
            if (parentCategory == null) {
                // 显示错误提示：没有选择分类
                ToastUtils.error(R.string.useless_category)
                return@setOnClickListener
            }

            callback?.invoke(parentCategory, childCategory)
            dismiss()
        }
    }


}