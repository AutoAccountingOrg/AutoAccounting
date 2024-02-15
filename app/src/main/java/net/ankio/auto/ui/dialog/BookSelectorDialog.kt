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


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.database.Db
import net.ankio.auto.database.table.BookName
import net.ankio.auto.databinding.BookSelectDialogBinding
import net.ankio.auto.ui.adapter.BookSelectorAdapter


class BookSelectorDialog(context: Context,val callback: (BookName) -> Unit) : BaseSheetDialog(context) {

    private lateinit var binding:BookSelectDialogBinding

    override fun onCreateView(inflater: LayoutInflater): View {
        binding =  BookSelectDialogBinding.inflate(inflater)
        val layoutManager = LinearLayoutManager(context)
        binding.recyclerView.layoutManager = layoutManager
        val dataItems = mutableListOf<BookName>()
        val adapter = BookSelectorAdapter(dataItems) { item, _ ->
            callback(item)
            this@BookSelectorDialog.dismiss()
        }
        //binding.recyclerView.setBackgroundColor(SurfaceColors.SURFACE_1.getColor(requireContext()))
        binding.recyclerView.adapter = adapter
        val defaultBook = BookName().apply {
            name = "默认账本"
            id = 0
        }

        lifecycleScope.launch {
            val newData = withContext(Dispatchers.IO) {
                Db.get().BookNameDao().loadAll()
            }

            val collection = newData.takeIf { it.isNotEmpty() } ?: listOf(defaultBook)

            withContext(Dispatchers.Main) {
                dataItems.addAll(collection)
                adapter.notifyItemInserted(0)
            }
        }

        return binding.root
    }


}