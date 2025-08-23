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
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import net.ankio.auto.R
import net.ankio.auto.databinding.DialogCategorySelectBinding
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.api.bindAs
import net.ankio.auto.ui.fragment.book.CategoryComponent
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
 * CategorySelectorDialog.create(activity)
 *     .setBook("我的账本")
 *     .setType(BillType.Income)
 *     .setCallback { parent, child ->
 *         // 处理分类选择结果
 *     }
 *     .show()
 * ```
 */
class CategorySelectorDialog private constructor(
    context: android.content.Context,
    lifecycleOwner: LifecycleOwner?,
    isOverlay: Boolean
) : BaseSheetDialog<DialogCategorySelectBinding>(context, lifecycleOwner, isOverlay) {

    private lateinit var categoryComponent: CategoryComponent

    private var book: String = ""
    private var type: BillType = BillType.Expend
    private var callback: ((CategoryModel?, CategoryModel?) -> Unit)? = null

    private var parentCategory: CategoryModel? = null
    private var childCategory: CategoryModel? = null

    /**
     * 设置账本名称
     * @param book 账本名称
     * @return 当前对话框实例，支持链式调用
     */
    fun setBook(book: String) = apply {
        this.book = book
        if (::categoryComponent.isInitialized) {
            categoryComponent.setBookInfo(book, type)
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
        categoryComponent = binding.category.bindAs<CategoryComponent>(lifecycleOwner!!.lifecycle)

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

    companion object {
        /**
         * 从Activity创建分类选择对话框
         * @param activity 宿主Activity
         * @return 对话框实例
         */
        fun create(activity: Activity): CategorySelectorDialog {
            return CategorySelectorDialog(activity, activity as LifecycleOwner, false)
        }

        /**
         * 从Fragment创建分类选择对话框
         * @param fragment 宿主Fragment
         * @return 对话框实例
         */
        fun create(fragment: Fragment): CategorySelectorDialog {
            return CategorySelectorDialog(
                fragment.requireContext(),
                fragment.viewLifecycleOwner,
                false
            )
        }

        /**
         * 从Service创建分类选择对话框（悬浮窗模式）
         * @param service 宿主Service
         * @return 对话框实例
         */
        fun create(service: LifecycleService): CategorySelectorDialog {
            return CategorySelectorDialog(service, service, true)
        }
    }
}