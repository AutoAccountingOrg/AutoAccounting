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
import androidx.lifecycle.LifecycleOwner
import net.ankio.auto.databinding.ComponentBookBinding
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.api.bindAs
import net.ankio.auto.ui.fragment.book.BookComponent
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BookNameModel

class BookSelectorDialog(
    private val showSelect: Boolean = false,
    val callback: (BookNameModel, BillType) -> Unit,
    activity: Activity
) : BaseSheetDialog<ComponentBookBinding>(activity) {

    private lateinit var bookComponent: BookComponent
    private val lifecycleOwner: LifecycleOwner = activity as LifecycleOwner

    override fun onViewCreated(view: View?) {
        super.onViewCreated(view)

        // 使用bindAs创建BookComponent实例
        bookComponent = binding.bindAs(lifecycleOwner.lifecycle)

        // 设置是否显示选择按钮
        bookComponent.setShowOption(showSelect, false)

        // 设置账本选择回调
        bookComponent.setOnBookSelectedListener { selectedBook, billType ->
            callback(selectedBook, BillType.valueOf(billType))
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
}
