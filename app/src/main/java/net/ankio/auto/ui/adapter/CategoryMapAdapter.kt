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
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterMapBinding
import net.ankio.auto.databinding.DialogInputBinding
import net.ankio.auto.databinding.DialogRegexInputBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.dialog.BookSelectorDialog
import net.ankio.auto.ui.dialog.CategorySelectorDialog
import net.ankio.auto.ui.scope.autoDisposeScope
import net.ankio.auto.ui.utils.ResourceUtils
import net.ankio.auto.utils.BillTool
import org.ezbook.server.db.model.CategoryMapModel
import org.ezbook.server.tools.Category

class CategoryMapAdapter(
    val dataItems: MutableList<CategoryMapModel>,
    val activity: Activity
) : BaseAdapter<AdapterMapBinding,CategoryMapModel>(AdapterMapBinding::class.java,dataItems) {


    override fun onInitViewHolder(holder: BaseViewHolder<AdapterMapBinding, CategoryMapModel>) {
        val binding = holder.binding
        binding.root.setOnClickListener {
            val item = holder.item!!
            val position = holder.positionIndex

            BookSelectorDialog(activity, true) { book,type ->
               CategorySelectorDialog(activity,book.remoteId,type) { category1,category2 ->
                   binding.root.autoDisposeScope.launch {
                       item.mapName = BillTool.getCateName(category1?.name!!, category2?.name)
                       CategoryMapModel.put(item)
                      withContext(Dispatchers.Main){
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

        binding.root.autoDisposeScope.launch {
            ResourceUtils.getCategoryDrawableByName(data.mapName,activity).let {
              withContext(Dispatchers.Main){
                  binding.target.setIcon(it)
              }
            }
        }
    }
}

