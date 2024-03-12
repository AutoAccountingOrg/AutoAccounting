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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import net.ankio.auto.database.table.BillInfo
import net.ankio.auto.databinding.AdapterOrderBinding


class OrderAdapter(
    private val dataItems: ArrayList<Pair<String, Array<BillInfo>>>,
) : RecyclerView.Adapter<OrderAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(AdapterOrderBinding.inflate(LayoutInflater.from(parent.context),parent,false),parent.context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //根据position获取Array<BillInfo>
        val item = dataItems[position]
        holder.bind(item.first, item.second)
    }

    private val job = Job()
    // 创建一个协程作用域，绑定在 IO 线程
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        job.cancel()
    }
    override fun getItemCount(): Int {
        return dataItems.size
    }


    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.cancel()
    }
    inner class ViewHolder(private val binding: AdapterOrderBinding,private val context:Context) : RecyclerView.ViewHolder(binding.root) {
        private lateinit var recyclerView: RecyclerView
        private lateinit var adapter: OrderItemAdapter
        private val dataInnerItems=mutableListOf<BillInfo>()

        fun bind(title:String,bills:Array<BillInfo>) {
            binding.groupCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(context))
            recyclerView = binding.recyclerView
            val layoutManager: LinearLayoutManager = object : LinearLayoutManager(context) {
                override fun canScrollVertically(): Boolean {
                    return false
                }
            }
            recyclerView.layoutManager = layoutManager

            adapter = OrderItemAdapter(dataInnerItems)
            recyclerView.adapter = adapter
            dataInnerItems.clear()
            dataInnerItems.addAll(bills)
            adapter.notifyDataSetChanged()
            binding.title.text = title

        }
        fun cancel(){
       //     job.cancel()
        }
    }
}

