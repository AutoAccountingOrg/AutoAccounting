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
import kotlinx.coroutines.launch
import net.ankio.auto.databinding.DialogBookSelectBinding
import net.ankio.auto.ui.adapter.BookSelectorAdapter
import net.ankio.auto.utils.SpUtils
import net.ankio.auto.utils.server.model.BookName

class BookSelectorDialog(private val context: Context, val callback: (BookName) -> Unit) :
    BaseSheetDialog(context) {
    private lateinit var binding: DialogBookSelectBinding
    private val dataItems = mutableListOf<BookName>()
    private lateinit var adapter: BookSelectorAdapter

    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogBookSelectBinding.inflate(inflater)
        val layoutManager = LinearLayoutManager(context)
        binding.recyclerView.layoutManager = layoutManager

        cardView = binding.cardView
        cardViewInner = binding.recyclerView
        adapter =
            BookSelectorAdapter(dataItems) { item ->
                callback(item)
                this@BookSelectorDialog.dismiss()
            }
        // binding.recyclerView.setBackgroundColor(SurfaceColors.SURFACE_1.getColor(requireContext()))
        binding.recyclerView.adapter = adapter

        return binding.root
    }

    private fun getData(callback: (List<BookName>) -> Unit) {
        val defaultBook =
            BookName().apply {
                name = SpUtils.getString("defaultBook", "默认账本")
                id = 0
            }

        lifecycleScope.launch {
            val newData = BookName.get()

            val collection = newData.takeIf { it.isNotEmpty() } ?: listOf(defaultBook)

            callback(collection)
        }
    }

    override fun show(
        float: Boolean,
        cancel: Boolean,
    ) {
        getData {
            if (it.size == 1) {
                callback(it[0])
                return@getData
            }
            dataItems.addAll(it)
            super.show(float, cancel)
            adapter.notifyItemInserted(0)
        }
    }
}
