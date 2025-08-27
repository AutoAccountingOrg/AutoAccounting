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
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.DialogBillMoreBinding
import net.ankio.auto.ui.adapter.OrderItemAdapter
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.components.WrapContentLinearLayoutManager
import net.ankio.auto.http.api.BillAPI
import org.ezbook.server.db.model.BillInfoModel

/**
 * 账单组详情对话框
 *
 * 用于显示账单组内的所有账单项，支持编辑和解除分组操作
 *
 * 使用方式：
 * ```kotlin
 * BaseSheetDialog.create<BillMoreDialog>(context)
 *     .setBillInfo(billInfoModel)
 *     .setOnReload { dialog -> refreshData() }
 *     .show()
 * ```
 */
class BillMoreDialog internal constructor(
    context: android.content.Context
) : BaseSheetDialog<DialogBillMoreBinding>(context) {

    private var billInfoModel: BillInfoModel? = null
    private var onReload: ((BillMoreDialog) -> Unit)? = null
    private val adapter = OrderItemAdapter(false)

    /**
     * 设置账单信息
     * @param billInfo 账单信息模型
     * @return 当前对话框实例，支持链式调用
     */
    fun setBillInfo(billInfo: BillInfoModel) = apply {
        this.billInfoModel = billInfo
    }

    /**
     * 设置重新加载回调
     * @param callback 重新加载回调函数
     * @return 当前对话框实例，支持链式调用
     */
    fun setOnReload(callback: (BillMoreDialog) -> Unit) = apply {
        this.onReload = callback
    }

    override fun onViewCreated(view: View?) {
        super.onViewCreated(view)

        val statusPage = binding.statusPage
        val recyclerView = statusPage.contentView!!
        val layoutManager = WrapContentLinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager

        // 设置点击事件 - 编辑账单
        adapter.setOnItemClickListener { item, position ->
            create<BillEditorDialog>(context)
                .setBillInfo(item)
                .setOnConfirm { updatedBill ->
                    adapter.updateItem(position, updatedBill)
                }
                .show()
        }

        // 设置长按事件 - 解除分组
        adapter.setOnItemLongClickListener { item, position ->
            create<BottomSheetDialogBuilder>(context)
                .setTitleInt(R.string.un_group_title)
                .setMessage(R.string.un_group_message)
                .setPositiveButton(R.string.sure_msg) { _, _ ->
                    launch {
                        BillAPI.unGroup(item.id)
                        onReload?.invoke(this@BillMoreDialog)
                    }
                }
                .setNegativeButton(R.string.cancel_msg) { _, _ -> }
                .show()
        }

        recyclerView.adapter = adapter
        loadBillData()
    }

    /**
     * 加载账单数据
     */
    private fun loadBillData() {
        val billInfo = billInfoModel ?: return

        binding.statusPage.showLoading()
        launch {
            try {
                val bills = BillAPI.getBillByGroup(billInfo.id)
                withContext(Dispatchers.Main) {
                    if (bills.isEmpty()) {
                        binding.statusPage.showEmpty()
                    } else {
                        adapter.updateItems(bills)
                        binding.statusPage.showContent()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.statusPage.showError()
                }
            }
        }
    }


}
