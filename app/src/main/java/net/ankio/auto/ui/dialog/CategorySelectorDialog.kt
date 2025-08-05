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
import net.ankio.auto.databinding.ComponentCategoryBinding
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.api.bindAs
import net.ankio.auto.ui.fragment.book.CategoryComponent
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
) : BaseSheetDialog<ComponentCategoryBinding>(activity) {

    private lateinit var categoryComponent: CategoryComponent
    private val lifecycleOwner: LifecycleOwner = activity as LifecycleOwner

    override fun onViewCreated(view: View?) {
        super.onViewCreated(view)

        // 使用bindAs创建CategoryComponent实例
        categoryComponent = binding.bindAs(lifecycleOwner.lifecycle)

        // 设置账本信息
        categoryComponent.setBookInfo(book, type)

        // 设置分类选择回调
        categoryComponent.setOnCategorySelectedListener { parent, child ->
            callback(parent, child)
            this.dismiss()
        }
    }

    /**
     * 刷新分类数据
     */
    fun refreshData() {
        if (::categoryComponent.isInitialized) {
            categoryComponent.refreshData()
        }
    }

    /**
     * 获取当前分类列表
     */
    fun getCategoryList(): List<CategoryModel> {
        return if (::categoryComponent.isInitialized) {
            categoryComponent.getDataItems()
        } else {
            emptyList()
        }
    }

    /**
     * 获取当前选中的分类
     */
    fun getSelectedCategories(): Pair<CategoryModel?, CategoryModel?> {
        return if (::categoryComponent.isInitialized) {
            categoryComponent.getSelectedCategories()
        } else {
            Pair(null, null)
        }
    }

    /**
     * 清除选择
     */
    fun clearSelection() {
        if (::categoryComponent.isInitialized) {
            categoryComponent.clearSelection()
        }
    }
}
