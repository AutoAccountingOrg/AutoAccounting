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

package net.ankio.auto.ui.adapter

import androidx.recyclerview.widget.LinearLayoutManager
import net.ankio.auto.databinding.AdapterOrderBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import org.ezbook.server.db.model.BillInfoModel

class OrderAdapter(resultData: MutableList<Pair<String, List<BillInfoModel>>>) :
    BaseAdapter<AdapterOrderBinding, Pair<String, List<BillInfoModel>>>(
        AdapterOrderBinding::class.java,
        resultData
    ) {
    override fun onInitViewHolder(holder: BaseViewHolder<AdapterOrderBinding, Pair<String, List<BillInfoModel>>>) {

    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterOrderBinding, Pair<String, List<BillInfoModel>>>,
        data: Pair<String, List<BillInfoModel>>,
        position: Int
    ) {
        val binding = holder.binding
        val layoutManager: LinearLayoutManager =
            object : LinearLayoutManager(holder.context) {
                override fun canScrollVertically(): Boolean {
                    return false
                }
            }
        binding.recyclerView.layoutManager = layoutManager

        val adapter = OrderItemAdapter(data.second.toMutableList())

        binding.recyclerView.adapter = adapter

        binding.title.text = data.first

    }
}