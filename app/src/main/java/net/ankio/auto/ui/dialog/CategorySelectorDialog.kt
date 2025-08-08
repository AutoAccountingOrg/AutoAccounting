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
import androidx.lifecycle.LifecycleOwner
import net.ankio.auto.R
import net.ankio.auto.databinding.ComponentCategoryBinding
import net.ankio.auto.databinding.DialogCategorySelectBinding
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.api.bindAs
import net.ankio.auto.ui.fragment.book.CategoryComponent
import net.ankio.auto.ui.utils.ToastUtils
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.CategoryModel

/**
 * 分类选择对话框
 * 使用CategoryComponent来展示分类选择功能
 */
class CategorySelectorDialog(
    private val book: String = "",
    private val type: BillType = BillType.Expend,
    val callback: (CategoryModel?, CategoryModel?) -> Unit,
    activity: Activity
) : BaseSheetDialog<DialogCategorySelectBinding>(activity) {

    private lateinit var categoryComponent: CategoryComponent
    private val lifecycleOwner: LifecycleOwner = activity as LifecycleOwner


    private var parentCategory: CategoryModel? = null;
    private var childCategory: CategoryModel? = null;

    override fun onViewCreated(view: View?) {
        super.onViewCreated(view)

        // 使用bindAs创建CategoryComponent实例
        categoryComponent = binding.category.bindAs<CategoryComponent>(lifecycleOwner.lifecycle)

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

            callback.invoke(parentCategory, childCategory)
            dismiss()
        }
    }

}
