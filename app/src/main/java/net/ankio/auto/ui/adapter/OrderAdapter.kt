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

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.elevation.SurfaceColors
import kotlinx.coroutines.launch
import net.ankio.auto.database.table.BillInfo
import net.ankio.auto.databinding.AdapterOrderBinding
import net.ankio.auto.ui.dialog.BillMoreDialog
import net.ankio.auto.ui.dialog.FloatEditorDialog
import net.ankio.auto.utils.AutoAccountingServiceUtils

class OrderAdapter(
    private val dataItems: ArrayList<Pair<String, Array<BillInfo>>>,
) : BaseAdapter<OrderAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        return ViewHolder(
            AdapterOrderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
            parent.context,
        )
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        // 根据position获取Array<BillInfo>
        val item = dataItems[position]
        holder.bind(item.first, item.second)
    }

    override fun getItemCount(): Int {
        return dataItems.size
    }

    inner class ViewHolder(private val binding: AdapterOrderBinding, private val context: Context) :
        RecyclerView.ViewHolder(binding.root) {
        private lateinit var recyclerView: RecyclerView
        private lateinit var adapter: OrderItemAdapter
        private val dataInnerItems = mutableListOf<BillInfo>()

        fun bind(
            title: String,
            bills: Array<BillInfo>,
        ) {
            binding.groupCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(context))
            recyclerView = binding.recyclerView
            val layoutManager: LinearLayoutManager =
                object : LinearLayoutManager(context) {
                    override fun canScrollVertically(): Boolean {
                        return false
                    }
                }
            recyclerView.layoutManager = layoutManager

            adapter =
                OrderItemAdapter(
                    dataInnerItems,
                    onItemChildClick = { item, position ->

                        scope.launch {
                            AutoAccountingServiceUtils.config(context).let {
                                FloatEditorDialog(context, item, it, onlyShow = true).show(false, true)
                            }
                        }
                    },
                    onItemChildMoreClick = { item, position ->

                        BillMoreDialog(context, item).show(false, true)
                    },
                )

            recyclerView.adapter = adapter
            dataInnerItems.clear()
            dataInnerItems.addAll(bills)
            adapter.notifyDataSetChanged()
            binding.title.text = title
        }
    }
}
