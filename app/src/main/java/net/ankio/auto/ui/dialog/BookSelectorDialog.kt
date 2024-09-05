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

//import net.ankio.auto.ui.adapter.BookSelectorAdapter
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import net.ankio.auto.databinding.DialogBookSelectBinding
import net.ankio.auto.ui.adapter.BookSelectorAdapter
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.componets.StatusPage
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BookNameModel

class BookSelectorDialog(private val context: Context,private val showSelect: Boolean = false, val callback: (BookNameModel,BillType) -> Unit) :
    BaseSheetDialog(context) {
    private lateinit var binding: DialogBookSelectBinding
    private val dataItems = mutableListOf<BookNameModel>()

    private lateinit var statusPage: StatusPage

    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogBookSelectBinding.inflate(inflater)
        statusPage = binding.statusPage
        val recyclerView = statusPage.contentView!!
        recyclerView.layoutManager = LinearLayoutManager(context)
        cardView = binding.cardView
        cardViewInner = recyclerView
        recyclerView.adapter =
            BookSelectorAdapter(dataItems,showSelect) { item, type ->
                callback(item,type)
                this@BookSelectorDialog.dismiss()
            }


        loadData()
        return binding.root
    }

    private fun loadData() {
        statusPage.showLoading()
        lifecycleScope.launch {
            dataItems.clear()
            var newData = BookNameModel.list()

            if (newData.isEmpty()) {
                newData = mutableListOf(BookNameModel().apply {
                    name = "默认账本"
                    id = 1
                    icon = ""
                })
            }
            dataItems.addAll(newData)

            statusPage.contentView!!.adapter?.notifyItemInserted(0)
            statusPage.showContent()
        }
    }


}
