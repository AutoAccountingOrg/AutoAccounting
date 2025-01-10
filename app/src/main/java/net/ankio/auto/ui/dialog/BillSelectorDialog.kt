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

//import net.ankio.auto.ui.adapter.BillSelectorAdapter
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.databinding.DialogCategorySelectBinding
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.BillSelectorAdapter
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.componets.StatusPage
import net.ankio.auto.ui.componets.WrapContentLinearLayoutManager
import net.ankio.auto.ui.utils.BookAppUtils
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.BookBillModel

class BillSelectorDialog(
    private val context: Context,
    private val selectedBills: MutableList<String> = ArrayList(),
    private val type: String,
    private val callback: (MutableList<String>) -> Unit,
) :
    BaseSheetDialog(context) {
    private lateinit var binding: DialogCategorySelectBinding
    private lateinit var statusPage: StatusPage
    private lateinit var adapter: BillSelectorAdapter
    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogCategorySelectBinding.inflate(inflater)
        val layoutManager = WrapContentLinearLayoutManager(context)
        statusPage = binding.statusPage
        //cardView = binding.cardView
        //cardViewInner = binding.cardViewInner
        val recyclerView = statusPage.contentView!!
        recyclerView.layoutManager = layoutManager
        adapter = BillSelectorAdapter(selectedBills)
        recyclerView.adapter = adapter

        recyclerView.setPadding(0, 0, 0, 0)

        binding.button.setOnClickListener {
            callback.invoke(selectedBills)
            dismiss()
        }
        statusPage.showLoading()

        lifecycleScope.launch {
            val proactively =
                ConfigUtils.getBoolean(Setting.PROACTIVELY_MODEL, DefaultData.PROACTIVELY_MODEL)
            if (proactively) {
                when (type) {
                    Setting.HASH_BAOXIAO_BILL -> BookAppUtils.syncReimburseBill() //先同步最近的报销账单
                    Setting.HASH_BILL -> BookAppUtils.syncRecentExpenseBill() //先同步最近的支付账单
                }
            }


            loadData(proactively)
        }


        return binding.root
    }


    private suspend fun loadData(proactively: Boolean) = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val timeout = 10000 // 10秒超时

        while (System.currentTimeMillis() - startTime < timeout) {
            val list = BookBillModel.list(type)
            Logger.d("list: $list")

            if (list.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    statusPage.showContent()
                    adapter.updateItems(list)
                }
                return@withContext
            }
            if (!proactively) {
                withContext(Dispatchers.Main) {
                    statusPage.showEmpty()
                }
                return@withContext
            }

            // 等待500毫秒后重试
            delay(500)
        }

        // 超时后显示空状态
        withContext(Dispatchers.Main) {
            statusPage.showEmpty()
        }
    }

}
