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

package net.ankio.auto.ui.adapter

import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.elevation.SurfaceColors
import kotlinx.coroutines.launch
import net.ankio.auto.database.table.BillInfo
import net.ankio.auto.databinding.AdapterOrderBinding
import net.ankio.auto.ui.dialog.BillMoreDialog
import net.ankio.auto.ui.dialog.FloatEditorDialog
import net.ankio.auto.utils.AutoAccountingServiceUtils

class OrderAdapter(
    override val dataItems: ArrayList<Pair<String, Array<BillInfo>>>,
) : BaseAdapter(dataItems, AdapterOrderBinding::class.java) {
    override fun onInitView(holder: BaseViewHolder) {
        val binding = holder.binding as AdapterOrderBinding
        val context = holder.context
        binding.groupCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(context))
    }

    override fun onBindView(
        holder: BaseViewHolder,
        item: Any,
        position: Int,
    ) {
        val binding = holder.binding as AdapterOrderBinding
        val context = holder.context
        val dataInnerItems = mutableListOf<BillInfo>()
        val layoutManager: LinearLayoutManager =
            object : LinearLayoutManager(context) {
                override fun canScrollVertically(): Boolean {
                    return false
                }
            }
        binding.recyclerView.layoutManager = layoutManager

        val adapter =
            OrderItemAdapter(
                dataInnerItems,
                onItemChildClick = { itemBill, _ ->
                    holder.scope.launch {
                        AutoAccountingServiceUtils.config(context).let {
                            FloatEditorDialog(context, itemBill, it, onlyShow = true).show(false, true)
                        }
                    }
                },
                onItemChildMoreClick = { itemBill, _ ->
                    BillMoreDialog(context, itemBill).show(false, true)
                },
            )

        val items = item as Pair<String, Array<BillInfo>>

        binding.recyclerView.adapter = adapter
        dataInnerItems.clear()
        dataInnerItems.addAll(items.second)
        adapter.notifyDataSetChanged()
        binding.title.text = items.first
    }
}
