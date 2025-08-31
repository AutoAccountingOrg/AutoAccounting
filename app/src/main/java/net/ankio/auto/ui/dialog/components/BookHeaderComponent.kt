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

package net.ankio.auto.ui.dialog.components

import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.launch
import net.ankio.auto.databinding.ComponentBookHeaderBinding
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.BookSelectorDialog
import net.ankio.auto.ui.utils.toBookCover
import org.ezbook.server.db.model.BillInfoModel

/**
 * 账簿头部组件 - 专用于账单编辑对话框
 *
 * 职责：
 * - 显示账簿封面图片
 * - 处理账簿选择点击事件
 * - 根据账簿名称更新封面
 *
 * 使用方式：
 * ```kotlin
 * val bookHeader: BookHeaderComponent = binding.bookHeader.bindAs()
 * bookHeader.setBillInfo(billInfoModel)
 * ```
 */
class BookHeaderComponent(
    binding: ComponentBookHeaderBinding
) : BaseComponent<ComponentBookHeaderBinding>(binding) {

    private lateinit var billInfoModel: BillInfoModel

    override fun onComponentCreate() {
        super.onComponentCreate()
        binding.bookImageClick.setOnClickListener {
            showBookSelector()
        }
    }

    /**
     * 设置账单信息
     */
    fun setBillInfo(billInfoModel: BillInfoModel) {
        this.billInfoModel = billInfoModel
        refresh()
    }

    fun refresh() {
        launch {
            billInfoModel.bookName.toBookCover(binding.bookImage)
        }
    }

    /**
     * 显示账本选择对话框
     */
    private fun showBookSelector() {
        if (!::billInfoModel.isInitialized) {
            return
        }

        // 使用BaseSheetDialog工厂方法创建对话框
        val dialog = BaseSheetDialog.create<BookSelectorDialog>(context)

        dialog.setCallback { selectedBook, _ ->
            // 更新账本名称
            billInfoModel.bookName = selectedBook.name
            // 刷新封面显示
            refresh()
        }.show()
    }


}
