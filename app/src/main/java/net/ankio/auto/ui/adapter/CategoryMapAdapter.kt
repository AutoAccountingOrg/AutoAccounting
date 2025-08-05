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
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.databinding.AdapterMapBinding
import net.ankio.auto.http.api.CategoryMapAPI
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.dialog.BookSelectorDialog
import net.ankio.auto.ui.dialog.CategorySelectorDialog
import net.ankio.auto.ui.utils.setCategoryIcon
import net.ankio.auto.utils.BillTool
import org.ezbook.server.db.model.CategoryMapModel

class CategoryMapAdapter(
    val activity: Activity
) : BaseAdapter<AdapterMapBinding, CategoryMapModel>() {


    override fun onInitViewHolder(holder: BaseViewHolder<AdapterMapBinding, CategoryMapModel>) {
        val binding = holder.binding
        binding.root.setOnClickListener {
            val item = holder.item!!
            val position = indexOf(item)

            // 显示账本选择对话框
            BookSelectorDialog(
                showSelect = true,
                callback = { book, type ->
                    // 显示分类选择对话框
                    CategorySelectorDialog(
                        book = book.name,
                        type = type,
                        callback = { category1, category2 ->
                            launchInAdapter {
                                // 更新映射名称
                                item.mapName =
                                    BillTool.getCateName(category1?.name ?: "", category2?.name)
                                CategoryMapAPI.put(item)
                                withContext(Dispatchers.Main) {
                                    updateItem(position, item)
                                }
                            }
                        },
                        activity = activity
                    ).show()
                },
                activity = activity
            ).show()

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
        binding.target.setTint(true)
        binding.containmentChip.visibility = View.GONE


        launchInAdapter {
            binding.target.imageView().setCategoryIcon(data.mapName)
        }
    }

    override fun areItemsSame(oldItem: CategoryMapModel, newItem: CategoryMapModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsSame(oldItem: CategoryMapModel, newItem: CategoryMapModel): Boolean {
        return oldItem == newItem
    }
}

