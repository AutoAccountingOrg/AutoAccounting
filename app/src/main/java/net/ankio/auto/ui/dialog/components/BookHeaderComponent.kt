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

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.ankio.auto.databinding.ComponentBookHeaderBinding
import net.ankio.auto.ui.dialog.BookSelectorDialog
import net.ankio.auto.utils.toBookCover
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
 * bookHeader.setBookName("我的账簿", lifecycleScope)
 * bookHeader.setOnBookClickListener {
 *     // 处理账簿选择逻辑
 * }
 * ```
 */
class BookHeaderComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private val binding: ComponentBookHeaderBinding =
        ComponentBookHeaderBinding.inflate(LayoutInflater.from(context), this)

    init {
        binding.bookImageClick.setOnClickListener {
            showBookSelector()
        }
    }

    /**
     * 显示账本选择对话框
     */
    private fun showBookSelector() {
        // 根据 lifecycleOwner 的类型创建对应的对话框
        val dialog = when (lifecycleOwner) {
            is android.app.Activity -> BookSelectorDialog.create(lifecycleOwner as android.app.Activity)
            is androidx.fragment.app.Fragment -> BookSelectorDialog.create(lifecycleOwner as androidx.fragment.app.Fragment)
            is androidx.lifecycle.LifecycleService -> BookSelectorDialog.create(lifecycleOwner as androidx.lifecycle.LifecycleService)
            else -> {
                // 如果无法确定类型，尝试从 context 获取 Activity
                val activity = context as? android.app.Activity
                if (activity != null) {
                    BookSelectorDialog.create(activity)
                } else {
                    return // 无法创建对话框
                }
            }
        }

        dialog.setCallback { selectedBook, _ ->
            // 更新账本名称
            billInfoModel.bookName = selectedBook.name
            // 刷新封面显示
            refresh()
        }.show()
    }

    private lateinit var lifecycleOwner: LifecycleOwner
    private lateinit var billInfoModel: BillInfoModel

    fun initBillInfo(billInfoModel: BillInfoModel, lifecycleOwner: LifecycleOwner) {
        this.lifecycleOwner = lifecycleOwner
        this.billInfoModel = billInfoModel
        refresh()

    }

    fun refresh() {
        lifecycleOwner.lifecycleScope.launch {
            billInfoModel.bookName.toBookCover(binding.bookImage)
        }
    }


}
