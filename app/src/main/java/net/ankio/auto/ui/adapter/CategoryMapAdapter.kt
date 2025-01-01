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

import android.app.Activity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.databinding.AdapterMapBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.dialog.BookSelectorDialog
import net.ankio.auto.ui.dialog.CategorySelectorDialog
import net.ankio.auto.ui.utils.ResourceUtils
import net.ankio.auto.utils.BillTool
import org.ezbook.server.db.model.CategoryMapModel

class CategoryMapAdapter(
    val activity: Activity
) : BaseAdapter<AdapterMapBinding, CategoryMapModel>(AdapterMapBinding::class.java) {


    override fun onInitViewHolder(holder: BaseViewHolder<AdapterMapBinding, CategoryMapModel>) {
        val binding = holder.binding
        binding.root.setOnClickListener {
            val item = holder.item!!
            val position = indexOf(item)

            BookSelectorDialog(activity, true) { book, type ->
                CategorySelectorDialog(activity, book.remoteId, type) { category1, category2 ->
                    holder.launch {
                        item.mapName = BillTool.getCateName(category1?.name!!, category2?.name)
                        CategoryMapModel.put(item)
                        withContext(Dispatchers.Main) {
                            notifyItemChanged(position)
                        }
                    }
                }.show(cancel = true)
            }.show(cancel = true)

        }
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterMapBinding, CategoryMapModel>,
        data: CategoryMapModel,
        position: Int
    ) {
        val binding = holder.binding

        binding.raw.text = data.name
        binding.target.setText(data.mapName)

        holder.launch {
            ResourceUtils.getCategoryDrawableByName(data.mapName, activity).let {
                withContext(Dispatchers.Main) {
                    binding.target.setIcon(it, true)
                }
            }
        }
    }

    override fun areItemsSame(oldItem: CategoryMapModel, newItem: CategoryMapModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsSame(oldItem: CategoryMapModel, newItem: CategoryMapModel): Boolean {
        return oldItem == newItem
    }
}

