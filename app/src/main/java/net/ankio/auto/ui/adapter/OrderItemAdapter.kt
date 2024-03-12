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

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.app.BillUtils
import net.ankio.auto.database.table.BillInfo
import net.ankio.auto.databinding.AdapterOrderItemBinding
import net.ankio.auto.utils.DateUtils

class OrderItemAdapter(
    private val dataItems: List<BillInfo>,
) : RecyclerView.Adapter<OrderItemAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(AdapterOrderItemBinding.inflate(LayoutInflater.from(parent.context),parent,false),parent.context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataItems[position]
        holder.bind(item,position)
    }

    override fun getItemCount(): Int {
        return dataItems.size
    }

    inner class ViewHolder(private val binding: AdapterOrderItemBinding, private val context: Context) : RecyclerView.ViewHolder(binding.root) {
        private val job = Job()
        // 创建一个协程作用域，绑定在 IO 线程
        private val scope = CoroutineScope(Dispatchers.IO + job)
        fun bind(item: BillInfo, position: Int) {
            binding.category.setText(item.cateName)
            scope.launch {
                BillInfo.getCategoryDrawable(item.cateName,context) {
                    binding.category.setIcon(it,true)
                }
            }
            binding.date.text = DateUtils.getTime("HH:mm:ss",item.timeStamp)


            val drawableRes = when (position) {
                0 -> R.drawable.float_minus
                1 -> R.drawable.float_add
                2 -> R.drawable.float_round
                else -> R.drawable.float_minus
            }


            val tintRes = BillUtils.getColor(position)


            val drawable = AppCompatResources.getDrawable(context, drawableRes)
            val color = ContextCompat.getColor(context, tintRes)
            binding.money.setColor(color)

            binding.money.setIcon(drawable, true)

            binding.money.setText(BillUtils.getFloatMoney(item.money).toString())

            binding.remark.text = item.remark

            binding.payTools.setText(item.accountNameFrom)

            scope.launch {
                BillInfo.getAccountDrawable(item.accountNameFrom,context) {
                    binding.payTools.setIcon(it,false)
                }
            }
        }
    }
}
