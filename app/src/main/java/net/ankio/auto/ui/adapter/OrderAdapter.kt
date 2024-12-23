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
import net.ankio.auto.ui.models.OrderGroup

class OrderAdapter(resultData: MutableList<OrderGroup>) :
    BaseAdapter<AdapterOrderBinding, OrderGroup>(
        AdapterOrderBinding::class.java,
        resultData
    ) {

    private val adaptersCache = mutableMapOf<Int, OrderItemAdapter>()

    override fun onInitViewHolder(holder: BaseViewHolder<AdapterOrderBinding, OrderGroup>) {
        val layoutManager = LinearLayoutManager(holder.context)
        layoutManager.isSmoothScrollbarEnabled = true
        holder.binding.recyclerView.layoutManager = layoutManager
    }


    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterOrderBinding, OrderGroup>,
        data: OrderGroup,
        position: Int
    ) {
        val binding = holder.binding
        // 如果适配器已经存在则重用，否则创建新的适配器
        val adapter = adaptersCache.getOrPut(position) { OrderItemAdapter(data.bills.toMutableList()) }
        binding.recyclerView.adapter = adapter

        binding.title.text = data.date

        // 仅在数据发生变化时调用
        adapter.notifyDataSetChanged()
    }

}

