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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterMapBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.dialog.AssetsMapDialog
import net.ankio.auto.ui.scope.autoDisposeScope
import net.ankio.auto.ui.utils.ResourceUtils
import org.ezbook.server.db.model.AssetsMapModel

class AssetsMapAdapter(
    val dataItems: MutableList<AssetsMapModel>,
    val activity: Activity
) : BaseAdapter<AdapterMapBinding, AssetsMapModel>(AdapterMapBinding::class.java, dataItems) {


    override fun onInitViewHolder(holder: BaseViewHolder<AdapterMapBinding, AssetsMapModel>) {
        val binding = holder.binding
        // 单击编辑
        binding.item.setOnClickListener {
            val item = holder.item!!
            val position = indexOf(item)
            AssetsMapDialog(activity, item) { changedAssetsMap ->
                dataItems[position] = changedAssetsMap
                notifyItemChanged(position)
                binding.root.autoDisposeScope.launch {
                    AssetsMapModel.put(changedAssetsMap)
                }
            }.show(cancel = true)
        }
        // 长按删除
        binding.item.setOnLongClickListener {
            val item = holder.item!!
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.delete_title)
                .setMessage(activity.getString(R.string.delete_message, item.name))
                .setNegativeButton(R.string.cancel) { dialog, which ->
                    // 用户点击了取消按钮，不做任何操作
                    dialog.dismiss()
                }
                .setPositiveButton(R.string.delete) { _, _ ->
                    // 用户点击了删除按钮，执行删除操作
                    binding.root.autoDisposeScope.launch {
                        AssetsMapModel.remove(item.id)
                    }

                    val position = dataItems.indexOf(item)
                    dataItems.remove(item)
                    notifyItemRemoved(position)
                }
                .show()
            true
        }
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterMapBinding, AssetsMapModel>,
        data: AssetsMapModel,
        position: Int
    ) {
        val binding = holder.binding
        binding.root.autoDisposeScope.launch {
            val drawable = ResourceUtils.getAssetDrawableFromName(data.mapName)
            withContext(Dispatchers.Main) {
                binding.target.setIcon(drawable)
            }
        }

        binding.raw.text = data.name
        binding.target.setText(data.mapName)
    }
}

