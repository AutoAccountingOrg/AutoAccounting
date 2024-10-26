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
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.databinding.DialogBookSelectBinding
import net.ankio.auto.request.RequestsUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.BackupFileSelectorAdapter
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.componets.StatusPage

class BackupSelectorDialog(
    private val context: Context,
    private val callback: (filename:String) -> Unit,
    private val requestsUtils: RequestsUtils,
    private val uri:String
) :
    BaseSheetDialog(context) {
    private lateinit var binding: DialogBookSelectBinding
    private val dataItems = mutableListOf<String>()
    private lateinit var statusPage: StatusPage
    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogBookSelectBinding.inflate(inflater)
        val layoutManager = LinearLayoutManager(context)
        statusPage = binding.statusPage
        cardView = binding.cardView
        cardViewInner = binding.statusPage
        val recyclerView = statusPage.contentView!!
        recyclerView.layoutManager = layoutManager

        recyclerView.adapter = BackupFileSelectorAdapter(dataItems){
            callback.invoke(it)
            dismiss()
        }

        recyclerView.setPadding(0, 0, 0, 0)


        lifecycleScope.launch {
            statusPage.showLoading()
            loadData()
        }


        return binding.root
    }


    private suspend fun loadData() {
        val (code,list) = requestsUtils.dir(uri)
        Logger.d("code:$code, list:$list")
        if (list.isEmpty()){
            withContext(Dispatchers.Main) {
                statusPage.showEmpty()
            }
            return
        }

        dataItems.addAll(list)
        withContext(Dispatchers.Main) {
            statusPage.showContent()
            statusPage.contentView!!.adapter?.notifyItemRangeInserted(0, list.size)
        }
    }

}
