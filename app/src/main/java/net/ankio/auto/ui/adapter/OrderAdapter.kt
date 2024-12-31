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
import org.ezbook.server.db.model.BillInfoModel

class OrderAdapter(resultData: MutableList<OrderGroup>) :
    BaseAdapter<AdapterOrderBinding, OrderGroup>(
        AdapterOrderBinding::class.java,
        resultData
    ) {

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
        // 直接创建新的适配器，避免缓存导致的问题
        val adapter = OrderItemAdapter(data.bills.toMutableList())
        adapter.setOnItemClickListener { billInfoModel, pos ->
            onItemClickListener?.invoke(billInfoModel, pos, adapter)
        }
        adapter.setOnItemLongClickListener { billInfoModel, pos ->
            onItemLongClickListener?.invoke(billInfoModel, pos, adapter)
        }
        adapter.setOnMoreClickListener {
            onMoreClickListener?.invoke(it, adapter)
        }
        binding.recyclerView.adapter = adapter
        binding.title.text = data.date

    }

    private var onItemClickListener: ((BillInfoModel, Int, OrderItemAdapter) -> Unit)? = null
    private var onItemLongClickListener: ((BillInfoModel, Int, OrderItemAdapter) -> Unit)? = null
    private var onMoreClickListener: ((BillInfoModel, OrderItemAdapter) -> Unit)? = null

    fun setOnItemClickListener(listener: (BillInfoModel, Int, OrderItemAdapter) -> Unit) {
        onItemClickListener = listener
    }

    fun setOnItemLongClickListener(listener: (BillInfoModel, Int, OrderItemAdapter) -> Unit) {
        onItemLongClickListener = listener
    }

    fun setOnMoreClickListener(listener: (BillInfoModel, OrderItemAdapter) -> Unit) {
        onMoreClickListener = listener
    }

}

