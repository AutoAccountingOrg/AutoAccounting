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
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.elevation.SurfaceColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.app.model.AppData
import net.ankio.auto.constant.DataType
import net.ankio.auto.constant.toDataType
import net.ankio.auto.database.Db
import net.ankio.auto.database.table.BillInfo
import net.ankio.auto.database.table.BillInfoGroup
import net.ankio.auto.databinding.AdapterDataBinding
import net.ankio.auto.databinding.AdapterOrderBinding
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.DateUtils
import net.ankio.auto.utils.Logger
import org.json.JSONObject


class OrderAdapter(
    private val dataItems: List<BillInfoGroup>,
) : RecyclerView.Adapter<OrderAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(AdapterOrderBinding.inflate(LayoutInflater.from(parent.context),parent,false),parent.context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataItems[position]
        holder.bind(item,position)
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
        private lateinit var layoutManager: LinearLayoutManager
        private val dataInnerItems=mutableListOf<BillInfo>()
        private val job = Job()
        // 创建一个协程作用域，绑定在 IO 线程
        private val scope = CoroutineScope(Dispatchers.IO + job)
        fun bind(item: BillInfoGroup,position: Int) {
            binding.groupCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(context))
            recyclerView = binding.recyclerView
            layoutManager = LinearLayoutManager(context)
            recyclerView.layoutManager = layoutManager

            adapter = OrderItemAdapter(dataInnerItems)
            recyclerView.adapter = adapter
            dataInnerItems.clear()
            scope.launch {
                dataInnerItems.addAll(Db.get().BillInfoDao().getTotal(item.ids.split(",").map { it.toInt() }))
                Logger.i("dataInnerItems:${item.ids}")
                withContext(Dispatchers.Main){
                    adapter.notifyDataSetChanged()
                }
            }
            binding.title.text = item.date

        }
        fun cancel(){
       //     job.cancel()
        }
    }
}

