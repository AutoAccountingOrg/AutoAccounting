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

//import net.ankio.auto.ui.adapter.OrderItemAdapter
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.DialogBillMoreBinding
import net.ankio.auto.ui.adapter.OrderItemAdapter
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.componets.WrapContentLinearLayoutManager
import org.ezbook.server.db.model.BillInfoModel

class BillMoreDialog(
    private val context: Context,
    private val billInfoModel: BillInfoModel,
    private val onReload: (BillMoreDialog) -> Unit
) :
    BaseSheetDialog(context) {
    private lateinit var binding: DialogBillMoreBinding
    private val dataItems = mutableListOf<BillInfoModel>()
    private val adapter = OrderItemAdapter(false)


    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        val statusPage = binding.statusPage
        val recyclerView = statusPage.contentView!!
        val layoutManager = WrapContentLinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager

        adapter.setOnItemClickListener { item, position ->
            FloatEditorDialog(context, item, false, onConfirmClick = {
                adapter.updateItem(position, it)
            }).show()
        }

// 设置长按事件
        adapter.setOnItemLongClickListener { item, position ->
            BottomSheetDialogBuilder(context)
                .setTitleInt(R.string.un_group_title)
                .setMessage(R.string.un_group_message)
                .setPositiveButton(R.string.sure_msg) { _, _ ->
                    lifecycleScope.launch {
                        BillInfoModel.unGroup(item.id)
                        onReload(this@BillMoreDialog)
                    }
                }
                .setNegativeButton(R.string.cancel_msg) { _, _ -> }
                .show()
        }
        recyclerView.adapter = adapter
        statusPage.showLoading()
        lifecycleScope.launch {
            val bills = BillInfoModel.getBillByGroup(billInfoModel.id)
            if (bills.isEmpty()) {
                withContext(Dispatchers.Main) {
                    statusPage.showEmpty()
                }
                return@launch
            }
            withContext(Dispatchers.Main) {
                adapter.updateItems(bills)
                statusPage.showContent()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogBillMoreBinding.inflate(inflater)
        return binding.root
    }

}
