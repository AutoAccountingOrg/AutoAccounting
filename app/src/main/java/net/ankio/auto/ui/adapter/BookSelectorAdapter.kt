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

import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.coroutines.launch
import net.ankio.auto.databinding.AdapterBookBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.scope.autoDisposeScope
import net.ankio.auto.ui.utils.ResourceUtils
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BookNameModel

class BookSelectorAdapter(
    val dataItems: MutableList<BookNameModel>,
    private val showSelect: Boolean = false,
    private val onClick: (item: BookNameModel,type:BillType) -> Unit,
) : BaseAdapter<AdapterBookBinding,BookNameModel>(AdapterBookBinding::class.java, dataItems) {



    override fun onInitViewHolder(holder: BaseViewHolder<AdapterBookBinding, BookNameModel>) {
        val binding = holder.binding

        binding.selectContainer.visibility = if (showSelect) android.view.View.VISIBLE else android.view.View.GONE

        val itemValue = binding.itemValue

        val layoutParams =  itemValue.layoutParams as ConstraintLayout.LayoutParams

        // 修改垂直偏移量
        layoutParams.verticalBias = if (showSelect) 0.33f else 0.5f

        itemValue.layoutParams = layoutParams

        if (showSelect){

            binding.income.setOnClickListener{
                onClick(holder.item!!,BillType.Income)
            }
            binding.expend.setOnClickListener{
                onClick(holder.item!!,BillType.Expend)
            }

        }else{
            binding.root.setOnClickListener {
                onClick(holder.item!!,BillType.Income)
            }
        }

    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterBookBinding, BookNameModel>,
        data: BookNameModel,
        position: Int
    ) {
        val binding = holder.binding
        binding.root.autoDisposeScope.launch {
            ResourceUtils.getBookNameDrawable(data.name,holder.context,binding.book)
        }
        binding.itemValue.text = data.name
    }
}

