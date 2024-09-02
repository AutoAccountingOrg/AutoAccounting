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
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterMapBinding
import net.ankio.auto.databinding.DialogInputBinding
import net.ankio.auto.databinding.DialogRegexInputBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.scope.autoDisposeScope
import org.ezbook.server.db.model.CategoryMapModel

class CategoryMapAdapter(
    val dataItems: MutableList<CategoryMapModel>,
    val activity: Activity
) : BaseAdapter<AdapterMapBinding,CategoryMapModel>(AdapterMapBinding::class.java,dataItems) {


    override fun onInitViewHolder(holder: BaseViewHolder<AdapterMapBinding, CategoryMapModel>) {
        val binding = holder.binding
        binding.item.setOnClickListener {
            val item = holder.item!!
            val position = holder.positionIndex
            val inputBinding = DialogInputBinding.inflate(LayoutInflater.from(activity))
            inputBinding.content.setText(item.mapName)

            MaterialAlertDialogBuilder(activity)
                .setTitle(activity.getString(R.string.category_map, item.name))
                .setView(inputBinding.root)
                .setPositiveButton(R.string.sure_msg) { dialog, which ->
                   item.mapName = inputBinding.content.text.toString()
                   binding.root.autoDisposeScope.launch {
                       CategoryMapModel.put(item)
                   }
                   notifyItemChanged(position)
                }
                .setNegativeButton(R.string.cancel_msg, null)
                .show()
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
        binding.target.setIcon(null)
    }
}

