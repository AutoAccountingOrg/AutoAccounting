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
import android.view.View
import net.ankio.auto.databinding.AdapterMapBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.utils.setAssetIconByName
import org.ezbook.server.db.model.AssetsMapModel

class BillAssetsMapAdapter(
    val context: Context
) : BaseAdapter<AdapterMapBinding, AssetsMapModel>() {

    private var onClickListener: ((AssetsMapModel) -> Unit)? = null
    fun setOnClickListener(listener: (AssetsMapModel) -> Unit) {
        onClickListener = listener
    }

    override fun onInitViewHolder(holder: BaseViewHolder<AdapterMapBinding, AssetsMapModel>) {
        val binding = holder.binding
        // 单击编辑
        binding.item.setOnClickListener {
            val item = holder.item!!
            onClickListener?.invoke(item)
        }
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterMapBinding, AssetsMapModel>,
        data: AssetsMapModel,
        position: Int
    ) {
        val binding = holder.binding
        launchInAdapter {
            binding.target.imageView().setAssetIconByName(data.mapName)
        }

        binding.raw.text = data.name
        binding.target.setText(data.mapName)
        binding.containmentChip.visibility = View.GONE
    }

    override fun areItemsSame(oldItem: AssetsMapModel, newItem: AssetsMapModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsSame(oldItem: AssetsMapModel, newItem: AssetsMapModel): Boolean {
        return oldItem == newItem
    }
}

