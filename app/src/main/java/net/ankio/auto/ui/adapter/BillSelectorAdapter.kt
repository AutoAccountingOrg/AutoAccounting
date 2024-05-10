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

import android.graphics.Color
import androidx.core.content.ContextCompat
import net.ankio.auto.app.BillUtils
import net.ankio.auto.databinding.AdapterBillBookBinding
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.DateUtils
import net.ankio.common.constant.BillType
import net.ankio.common.model.BillModel

class BillSelectorAdapter(
    override val dataItems: List<BillModel>,
    private val selectedItems: ArrayList<BillModel>,
) : BaseAdapter(dataItems, AdapterBillBookBinding::class.java) {
    override fun onInitView(holder: BaseViewHolder) {
        val binding = holder.binding as AdapterBillBookBinding
        binding.root.setOnClickListener {
            val item = holder.item as BillModel
            if (selectedItems.contains(item)) {
                selectedItems.remove(item)
                // 清除背景
                binding.root.setBackgroundColor(Color.TRANSPARENT)
            } else {
                selectedItems.add(item)
                binding.root.setBackgroundColor(AppUtils.getThemeAttrColor(com.google.android.material.R.attr.colorPrimaryContainer))
            }
        }
    }

    override fun onBindView(
        holder: BaseViewHolder,
        item: Any,
    ) {
        val binding = holder.binding as AdapterBillBookBinding
        val it = item as BillModel
        val context = holder.context

        if (selectedItems.contains(item)) {
            binding.root.setBackgroundColor(AppUtils.getThemeAttrColor(com.google.android.material.R.attr.colorPrimaryContainer))
        } else {
            binding.root.setBackgroundColor(Color.TRANSPARENT)
        }

        val prefix = if (item.type == BillType.Income) "-" else "+"

        binding.tvAmount.text = String.format("$prefix %.2f", item.amount) // 保留两位有效数字
        binding.tvTime.text = DateUtils.getTime(item.time)
        binding.tvRemark.text = item.remark

        val colorRes = BillUtils.getColor(item.type.toInt())
        val color = ContextCompat.getColor(context, colorRes)
        binding.tvAmount.setTextColor(color)
    }
}
