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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
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
import net.ankio.auto.ui.utils.AssetsUtils
import net.ankio.auto.utils.BillTool
import net.ankio.auto.utils.DateUtils
import org.ezbook.server.constant.BillState


open class BillFragment : BasePageFragment<OrderGroup, FragmentBillBinding>() {
    private var syncType = mutableListOf<String>()
    override suspend fun loadData(): List<OrderGroup> {
        val list = BillAPI.list(page, pageSize, syncType)

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
            launch {
                AssetsUtils.setMapAssets(requireContext(), false, item) {
                    BaseSheetDialog.create<BillEditorDialog>(requireContext())
                        .setBillInfo(item)
                        .setOnConfirm { updatedBill ->
                            itemAdapter.updateItem(position, updatedBill)
                            // 保存到
                            this@BillFragment.launch {
                                Logger.d("更新账单：$updatedBill")
                                BillAPI.put(updatedBill)
                                BillTool.syncBill(updatedBill)
                            }

                        }
                        .show()
                }
            }

        }

        // 设置长按事件
        adapter.setOnItemLongClickListener { item, position, itemAdapter ->
            BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
                .setTitleInt(R.string.delete_title)
                .setMessage(R.string.delete_bill_message)
                .setPositiveButton(R.string.sure_msg) { _, _ ->
                    itemAdapter.removeItem(item)
                    launch {
                        BillAPI.remove(item.id)
                    }
                }
                .setNegativeButton(R.string.cancel_msg) { _, _ -> }
                .show()
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

        chipEvent()
        return adapter
    }


    private fun chipEvent() {
        binding.chipGroup.isSingleSelection = false
        binding.chipSynced.isChecked = true
        binding.chipWaitEdit.isChecked = true
        binding.chipWaitSync.isChecked = true
        binding.chipGroup.setOnCheckedStateChangeListener { group, checkedId ->
            syncType.clear()
            checkedId.forEach {
                when (it) {
                    R.id.chip_synced -> {
                        syncType.add(BillState.Synced.name)
                    }

                    R.id.chip_wait_edit -> {
                        syncType.add(BillState.Wait2Edit.name)
                    }

                    R.id.chip_wait_sync -> {
                        syncType.add(BillState.Edited.name)
                    }
                }
            }
            reload()
        }
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
                launch {
                    BillAPI.clear()
                    reload()
                }
            }
            .setNegativeButton(requireActivity().getString(R.string.cancel_msg)) { _, _ -> }
            .show()
    }
}