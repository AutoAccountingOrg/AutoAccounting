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

import net.ankio.auto.databinding.AdapterOrderBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.components.WrapContentLinearLayoutManager
import net.ankio.auto.ui.models.OrderGroup
import org.ezbook.server.db.model.BillInfoModel

class BillAdapter :
    BaseAdapter<AdapterOrderBinding, OrderGroup>(

    ) {

    override fun onInitViewHolder(holder: BaseViewHolder<AdapterOrderBinding, OrderGroup>) {
        val layoutManager = WrapContentLinearLayoutManager(holder.context)
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
        val adapter = BillItemAdapter()
        adapter.setOnItemClickListener { billInfoModel, pos ->
            onItemClickListener?.invoke(billInfoModel, pos, adapter)
        }
        adapter.setOnItemLongClickListener { billInfoModel, pos ->
            onItemLongClickListener?.invoke(billInfoModel, pos, adapter)
        }
        adapter.setOnMoreClickListener {
            onMoreClickListener?.invoke(it, adapter)
        }
        adapter.updateItems(data.bills)
        binding.recyclerView.adapter = adapter
        binding.title.text = data.date

    }

    private var onItemClickListener: ((BillInfoModel, Int, BillItemAdapter) -> Unit)? = null
    private var onItemLongClickListener: ((BillInfoModel, Int, BillItemAdapter) -> Unit)? = null
    private var onMoreClickListener: ((BillInfoModel, BillItemAdapter) -> Unit)? = null

    fun setOnItemClickListener(listener: (BillInfoModel, Int, BillItemAdapter) -> Unit) {
        onItemClickListener = listener
    }

    fun setOnItemLongClickListener(listener: (BillInfoModel, Int, BillItemAdapter) -> Unit) {
        onItemLongClickListener = listener
    }

    fun setOnMoreClickListener(listener: (BillInfoModel, BillItemAdapter) -> Unit) {
        onMoreClickListener = listener
    }

    override fun areItemsSame(oldItem: OrderGroup, newItem: OrderGroup): Boolean {
        return oldItem.date == newItem.date && oldItem.bills == newItem.bills
    }

    override fun areContentsSame(oldItem: OrderGroup, newItem: OrderGroup): Boolean {
        return oldItem == newItem
    }

}

