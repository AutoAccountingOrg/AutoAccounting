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


package net.ankio.auto.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentBillBinding
import net.ankio.auto.http.api.BillAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.BillAdapter
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.components.WrapContentLinearLayoutManager
import net.ankio.auto.ui.dialog.BillEditorDialog
import net.ankio.auto.ui.dialog.BillMoreDialog
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.models.OrderGroup
import net.ankio.auto.ui.utils.ListPopupUtilsGeneric
import net.ankio.auto.utils.BillTool
import net.ankio.auto.utils.DateUtils
import org.ezbook.server.constant.BillState


open class BillFragment : BasePageFragment<OrderGroup, FragmentBillBinding>() {
    // 同步状态筛选：由用户在状态按钮中选择，默认空表示保持原有行为（由后端决定）
    private var syncType = mutableListOf<String>()
    private var currentYear: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    private var currentMonth: Int =
        java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
    override suspend fun loadData(): List<OrderGroup> {
        val list = BillAPI.list(page, pageSize, syncType, currentYear, currentMonth)
        Logger.i("加载账单: ${list.size}条")

        val groupedData = list.groupBy {
            DateUtils.stampToDate(it.time, "yyyy-MM-dd")
        }.map { (date, bills) ->
            OrderGroup(date, bills)
        }

        return if (list.isEmpty()) emptyList() else groupedData
    }

    private val adapter = BillAdapter()

    override fun onCreateAdapter(): RecyclerView.Adapter<*> {
        val recyclerView = binding.statusPage.contentView!!
        recyclerView.layoutManager = WrapContentLinearLayoutManager(requireContext())

        adapter.setOnItemClickListener { item, position, itemAdapter ->
            BaseSheetDialog.create<BillEditorDialog>(requireContext())
                .setBillInfo(item)
                .setOnConfirm { updatedBill ->
                    Logger.i("编辑账单: ${updatedBill.id}")
                    itemAdapter.updateItem(position, updatedBill)

                }
                .show()
        }

        // 设置长按事件
        adapter.setOnItemLongClickListener { item, position, itemAdapter ->
            if (!net.ankio.auto.utils.PrefManager.confirmDeleteBill) {
                Logger.i("删除账单: ${item.id}")
                itemAdapter.removeItem(item)
                launch { BillAPI.remove(item.id) }
            } else {
                BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
                    .setTitleInt(R.string.delete_title)
                    .setMessage(R.string.delete_bill_message)
                    .setPositiveButton(R.string.sure_msg) { _, _ ->
                        Logger.i("删除账单: ${item.id}")
                        itemAdapter.removeItem(item)
                        launch {
                            BillAPI.remove(item.id)
                        }
                    }
                    .setNegativeButton(R.string.cancel_msg) { _, _ -> }
                    .show()
            }
        }

        // 设置更多按钮点击事件
        adapter.setOnMoreClickListener { item, itemAdapter ->
            BaseSheetDialog.create<BillMoreDialog>(requireContext())
                .setBillInfo(item)
                .setOnReload {
                    it.dismiss()
                    reload()
                }
                .show()
        }

        setupFilters()
        return adapter
    }


    private fun setupFilters() {
        // 月份按钮：显示当前年月，点击弹出月份选择器
        binding.inputMonthButton.text = formatMonthLabel(currentYear, currentMonth)
        binding.inputMonthChevron.setOnClickListener {
            BaseSheetDialog.create<net.ankio.auto.ui.dialog.DateTimePickerDialog>(requireContext())
                .setTitle(getString(R.string.select_month))
                .setYearMonthOnly(true)
                .setOnDateTimeSelected { year, month, _, _, _ ->
                    currentYear = year
                    currentMonth = month
                    // 更新月份按钮显示文本
                    binding.inputMonthButton.text = formatMonthLabel(currentYear, currentMonth)
                    reload()
                }.setOnDismiss {
                    binding.inputMonthChevron.isChecked = false
                }
                .show()
        }

        // 同步状态按钮：点击弹出简单菜单，供用户选择
        val statusItems = linkedMapOf(
            getString(R.string.filter_type_all) to null,
            getString(R.string.item_synced) to BillState.Synced,
            getString(R.string.item_wait_sync) to BillState.Edited,
            getString(R.string.item_wait_edit) to BillState.Wait2Edit
        )
        binding.inputStatusButton.text = statusItems.keys.first()
        binding.inputStatusChevron.setOnClickListener { anchorView ->
            ListPopupUtilsGeneric.create<Map.Entry<String, BillState?>>(requireContext())
                .setAnchor(anchorView)
                .setList(statusItems.entries.associateBy({ it.key }, { it }))
                .setOnItemClick { _, key, entry ->
                    // 更新筛选集合
                    syncType.clear()
                    entry.value?.let { state -> syncType.add(state.name) } ?: run {
                        // 全部：包含三种状态
                        syncType.add(BillState.Synced.name)
                        syncType.add(BillState.Edited.name)
                        syncType.add(BillState.Wait2Edit.name)
                    }
                    // 更新按钮文案并刷新
                    binding.inputStatusButton.text = key
                    reload()
                }.setOnDismiss {
                    binding.inputStatusChevron.isChecked = false
                }
                .show()
        }
    }

    /** 将年月格式化为 "YYYY-MM" */
    private fun formatMonthLabel(year: Int, month: Int): String {
        val mm = if (month < 10) "0$month" else "$month"
        return "$year-$mm"
    }


    override fun onResume() {
        super.onResume()
        reload()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefreshLayout.isEnabled = false

        // 顶部菜单事件监听
        binding.topAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.item_sync -> {
                    Logger.i("账单同步")
                    launch {
                        BillTool.syncBills()
                    }
                    true
                }

                R.id.item_clear -> {
                    showClearDataDialog()
                    true
                }

                else -> false
            }
        }
    }

    // 抽取删除数据的逻辑
    private fun showClearDataDialog() {
        BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
            .setTitle(getString(R.string.delete_data))
            .setMessage(getString(R.string.delete_msg))
            .setPositiveButton(getString(R.string.sure_msg)) { _, _ ->
                Logger.i("清空账单数据")
                launch {
                    BillAPI.clear()
                    reload()
                }
            }
            .setNegativeButton(requireActivity().getString(R.string.cancel_msg)) { _, _ -> }
            .show()
    }
}