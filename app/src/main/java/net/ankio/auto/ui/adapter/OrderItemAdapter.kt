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
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.app.BillUtils
import net.ankio.auto.database.table.Assets
import net.ankio.auto.database.table.BillInfo
import net.ankio.auto.database.table.Category
import net.ankio.auto.databinding.AdapterOrderItemBinding
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.DateUtils
import net.ankio.common.constant.BillType

class OrderItemAdapter(
    private val dataItems: List<BillInfo>,
    private val onItemChildClick:( (item: BillInfo, position: Int) -> Unit)?,
    private val onItemChildMoreClick:( (item: BillInfo, position: Int) -> Unit)?
) : BaseAdapter<OrderItemAdapter.ViewHolder>() {

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

        fun bind(item: BillInfo, position: Int) {
            binding.category.setText(item.cateName)
            scope.launch {
                Category.getDrawable(item.cateName,context).let {
                    withContext(Dispatchers.Main){
                        binding.category.setIcon(it,true)
                    }
                }
            }

            binding.date.text = DateUtils.getTime("HH:mm:ss",item.timeStamp)


            val type = when (item.type ) {
                BillType.Expend -> BillType.Expend
                BillType.ExpendReimbursement -> BillType.Expend
                BillType.ExpendLending -> BillType.Expend
                BillType.ExpendRepayment -> BillType.Expend
                BillType.Income ->   BillType.Income
                BillType.IncomeLending ->   BillType.Income
                BillType.IncomeRepayment ->   BillType.Income
                BillType.IncomeReimbursement ->   BillType.Income
                BillType.Transfer -> BillType.Transfer
            }

            val drawableRes = when (type.toInt()) {
                0 -> R.drawable.float_minus
                1 -> R.drawable.float_add
                2 -> R.drawable.float_round
                else -> R.drawable.float_minus
            }


            val tintRes = BillUtils.getColor(type.toInt())


            val drawable = AppCompatResources.getDrawable(context, drawableRes)
            val color = ContextCompat.getColor(context, tintRes)
            binding.money.setColor(color)

            binding.money.setIcon(drawable, true)

            binding.money.setText(BillUtils.getFloatMoney(item.money).toString())

            binding.remark.text = item.remark

            binding.payTools.setText(item.accountNameFrom)

            scope.launch {
                Assets.getDrawable(item.accountNameFrom,context).let {
                    binding.payTools.setIcon(it,false)
                }
            }

            binding.channel.text = item.channel
            scope.launch {
               AppUtils.getAppInfoFromPackageName(item.from,context)?.let {
             //      binding.fromApp.text = it.name
                   binding.fromApp.icon = it.icon
               }
            }
         //   binding.fromApp.setIcon()

            binding.root.setOnClickListener {
                onItemChildClick?.invoke(item,position)
            }

            if(BillUtils.noNeedFilter(item)){
                binding.moreBills.visibility = View.GONE
            }

            if(onItemChildMoreClick==null){
                binding.moreBills.visibility = View.GONE
            }

            binding.moreBills.setOnClickListener {
                onItemChildMoreClick?.invoke(item,position)
            }
        }
    }
}
