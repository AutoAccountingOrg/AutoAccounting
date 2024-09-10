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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterOrderItemBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.scope.autoDisposeScope
import net.ankio.auto.ui.utils.ResourceUtils
import net.ankio.auto.utils.BillTool
import net.ankio.auto.utils.DateUtils
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BillInfoModel

class BillItemAdapter(list: MutableList<BillInfoModel>) : BaseAdapter<AdapterOrderItemBinding,BillInfoModel>(AdapterOrderItemBinding::class.java,list) {
    override fun onInitViewHolder(holder: BaseViewHolder<AdapterOrderItemBinding, BillInfoModel>) {

    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterOrderItemBinding, BillInfoModel>,
        data: BillInfoModel,
        position: Int
    ) {
        val binding = holder.binding
        binding.category.setText(data.cateName)
        binding.payTools.setText(data.accountNameFrom)
        holder.binding.root.autoDisposeScope.launch {
            ResourceUtils.getCategoryDrawableByName(data.cateName,holder.context).let {
                withContext(Dispatchers.Main){
                    binding.category.setIcon(it,true)
                }
            }

            ResourceUtils.getAssetDrawableFromName(data.accountNameFrom).let {
                withContext(Dispatchers.Main){
                    binding.payTools.setIcon(it)
                }
            }
        }
        BillTool.setTextViewPrice(data.money, BillType.Expend, binding.money)
        binding.date.text = DateUtils.stampToDate(data.time, "HH:mm:ss")

        if (data.remark.isEmpty()) {
            binding.remark.text = "无备注"
        } else {
            binding.remark.text = data.remark
        }

        if (data.syncFromApp){
            binding.sync.setImageResource(R.drawable.ic_sync)
        }else{
            binding.sync.setImageResource(R.drawable.ic_no_sync)
        }
    }

}