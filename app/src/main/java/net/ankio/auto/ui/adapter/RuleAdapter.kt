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
import android.view.View
import android.view.ViewGroup
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.elevation.SurfaceColors
import net.ankio.auto.R
import net.ankio.auto.database.table.Regular
import net.ankio.auto.databinding.AdapterRuleBinding


class RuleAdapter(
    private val dataItems: List<Regular>,
    private val onClickEdit: (item: Regular,position: Int)->Unit,
    private val onClickDelete: (item: Regular,position: Int)->Unit
    ) : BaseAdapter<RuleAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(AdapterRuleBinding.inflate(LayoutInflater.from(parent.context),parent,false),parent.context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataItems[position]
        holder.bind(item,position)
    }

    override fun getItemCount(): Int {
        return dataItems.size
    }

    inner class ViewHolder(private val binding: AdapterRuleBinding,private val context:Context) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Regular,position: Int) {
            binding.groupCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(context))

            if(!item.auto){
                binding.type.visibility = View.GONE
            }

            if(item.element==null || item.element!!.list.isEmpty()){
                return
            }
            val list = item.element!!.list.toMutableList()

            val lastElement = list.removeLast()
            val flexboxLayout = binding.flexboxLayout
            flexboxLayout.removedAllElement()
            flexboxLayout.textAppearance = com.google.android.material.R.style.TextAppearance_Material3_BodyLarge
            flexboxLayout.appendTextView(context.getString(R.string.if_condition_true))
            flexboxLayout.firstWaveTextViewPosition = flexboxLayout.size - 1
            for (hashMap in list) {
                if(hashMap.containsKey("jsPre")){
                    flexboxLayout.appendButton( if((hashMap["jsPre"] as String ).contains("and"))context.getString(R.string.and) else context.getString(R.string.or))
                }
                flexboxLayout.appendWaveTextview(hashMap["text"] as String, connector = false){ _, _ -> }
            }
            flexboxLayout.appendTextView(context.getString(R.string.condition_result_book))
            flexboxLayout.appendWaveTextview(lastElement["book"] as String){_,_-> }
            flexboxLayout.appendTextView(context.getString(R.string.condition_result_category))
            flexboxLayout.appendWaveTextview(lastElement["category"] as String){ _, _ -> }

            binding.deleteData.setOnClickListener {
                onClickDelete(item,position)

            }
            binding.editRule.setOnClickListener{
                onClickEdit(item,position)
            }
        }
    }
}
