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

import android.view.View
import androidx.core.view.size
import com.google.android.material.elevation.SurfaceColors
import net.ankio.auto.R
import net.ankio.auto.database.table.Regular
import net.ankio.auto.databinding.AdapterRuleBinding

class RuleAdapter(
    override val dataItems: List<Regular>,
    private val onClickEdit: (item: Regular, position: Int) -> Unit,
    private val onClickDelete: (item: Regular, position: Int) -> Unit,
) : BaseAdapter(dataItems, AdapterRuleBinding::class.java) {
    override fun onBindView(
        holder: BaseViewHolder,
        item: Any,
        position: Int,
    ) {
        val binding = holder.binding as AdapterRuleBinding
        val context = binding.root.context
        val regular = item as Regular

        if (!regular.auto) {
            binding.type.visibility = View.GONE
        }

        if (regular.element == null || regular.element!!.list.isEmpty()) {
            return
        }
        val list = regular.element!!.list.toMutableList()

        val lastElement = list.removeLast()
        val flexboxLayout = binding.flexboxLayout
        flexboxLayout.removedAllElement()
        flexboxLayout.textAppearance =
            com.google.android.material.R.style.TextAppearance_Material3_BodyLarge
        flexboxLayout.appendTextView(context.getString(R.string.if_condition_true))
        flexboxLayout.firstWaveTextViewPosition = flexboxLayout.size - 1
        for (hashMap in list) {
            if (hashMap.containsKey("jsPre")) {
                flexboxLayout.appendButton(
                    if ((hashMap["jsPre"] as String).contains("and")) {
                        context.getString(
                            R.string.and,
                        )
                    } else {
                        context.getString(R.string.or)
                    },
                )
            }
            flexboxLayout.appendWaveTextview(
                hashMap["text"] as String,
                connector = false,
            ) { _, _ -> }
        }
        flexboxLayout.appendTextView(context.getString(R.string.condition_result_book))
        flexboxLayout.appendWaveTextview(lastElement["book"] as String) { _, _ -> }
        flexboxLayout.appendTextView(context.getString(R.string.condition_result_category))
        flexboxLayout.appendWaveTextview(lastElement["category"] as String) { _, _ -> }
    }

    override fun onInitView(holder: BaseViewHolder) {
        val binding = holder.binding as AdapterRuleBinding
        val context = binding.root.context
        val position = holder.adapterPosition
        val item = dataItems[position]

        binding.groupCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(context))
        binding.deleteData.setOnClickListener {
            onClickDelete(item, position)
        }
        binding.editRule.setOnClickListener {
            onClickEdit(item, position)
        }
    }
}
