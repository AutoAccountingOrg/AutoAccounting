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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentBillBinding
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.ui.adapter.OrderAdapter
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.componets.WrapContentLinearLayoutManager
import net.ankio.auto.ui.dialog.BillMoreDialog
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.dialog.FloatEditorDialog
import net.ankio.auto.ui.models.OrderGroup
import net.ankio.auto.ui.utils.AssetsUtils
import net.ankio.auto.ui.utils.BookAppUtils
import net.ankio.auto.ui.utils.viewBinding
import net.ankio.auto.utils.DateUtils
import net.ankio.auto.xposed.core.utils.DataUtils
import org.ezbook.server.constant.BillState
import org.ezbook.server.constant.DataType
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import org.ezbook.server.constant.SyncType
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.db.model.SettingModel


open class OrderFragment : BasePageFragment<OrderGroup>() {
    override val binding: FragmentBillBinding by viewBinding(FragmentBillBinding::inflate)
    var syncType = mutableListOf<String>()
    override suspend fun loadData(callback: (resultData: List<OrderGroup>) -> Unit) {
        val list = BillInfoModel.list(page, pageSize, syncType)

        val groupedData = list.groupBy {
            DateUtils.stampToDate(it.time, "yyyy-MM-dd")
        }.map { (date, bills) ->
            OrderGroup(date, bills)
        }




        withContext(Dispatchers.Main) {

            //    adapter.updateItems(groupedData, page == 1)

            callback.invoke(if (list.isEmpty()) emptyList() else groupedData)
        }
    }

    val adapter = OrderAdapter()

    override fun onCreateAdapter() {
        val recyclerView = binding.statusPage.contentView!!
        recyclerView.layoutManager = WrapContentLinearLayoutManager(requireContext())

        adapter.setOnItemClickListener { item, position, itemAdapter ->
            lifecycleScope.launch {
                AssetsUtils.setMapAssets(requireContext(), false, item) {
                    FloatEditorDialog(requireContext(), item, false, onConfirmClick = {
                        itemAdapter.updateItem(position, it)
                    }).showInFragment(this@OrderFragment, false, true)
                }
            }

        }

// 设置长按事件
        adapter.setOnItemLongClickListener { item, position, itemAdapter ->
            BottomSheetDialogBuilder(requireContext())
                .setTitleInt(R.string.delete_title)
                .setMessage(R.string.delete_bill_message)
                .setPositiveButton(R.string.sure_msg) { _, _ ->
                    itemAdapter.removeItem(item)
                    lifecycleScope.launch {
                        BillInfoModel.remove(item.id)
                    }
                }
                .setNegativeButton(R.string.cancel_msg) { _, _ -> }
                .showInFragment(this, false, true)
        }

// 设置更多按钮点击事件
        adapter.setOnMoreClickListener { item, itemAdapter ->
            BillMoreDialog(requireContext(), item, onReload = {
                it.dismiss()
                reload()
            }).showInFragment(this, false, true)
        }
        recyclerView.adapter = adapter


        chipEvent()
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 顶部菜单事件监听
        binding.topAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.item_sync -> {
                    lifecycleScope.launch {
                        BookAppUtils.syncData()
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
        BottomSheetDialogBuilder(requireActivity())
            .setTitle(requireActivity().getString(R.string.delete_data))
            .setMessage(requireActivity().getString(R.string.delete_msg))
            .setPositiveButton(requireActivity().getString(R.string.sure_msg)) { _, _ ->
                lifecycleScope.launch {
                    BillInfoModel.clear()
                    reload()
                }
            }
            .setNegativeButton(requireActivity().getString(R.string.cancel_msg)) { _, _ -> }
            .showInFragment(this, false, true)
    }
}

