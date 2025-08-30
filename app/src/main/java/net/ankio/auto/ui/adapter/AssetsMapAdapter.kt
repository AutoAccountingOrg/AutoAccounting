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
import net.ankio.auto.databinding.AdapterMapBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.utils.setAssetIconByName
import org.ezbook.server.db.model.AssetsMapModel

/**
 * 资产映射适配器
 *
 * 负责展示资产映射列表项，通过链式调用配置各种事件处理
 */
class AssetsMapAdapter : BaseAdapter<AdapterMapBinding, AssetsMapModel>() {

    /** 编辑事件处理器 */
    private var onEditClick: ((AssetsMapModel, Int) -> Unit)? = null

    /** 删除事件处理器 */
    private var onDeleteClick: ((AssetsMapModel) -> Unit)? = null

    /**
     * 设置编辑点击事件处理器
     * @param handler 处理器函数，接收(item, position)参数
     * @return 适配器实例，支持链式调用
     */
    fun setOnEditClick(handler: (AssetsMapModel, Int) -> Unit) = apply {
        this.onEditClick = handler
    }

    /**
     * 设置删除点击事件处理器
     * @param handler 处理器函数，接收item参数
     * @return 适配器实例，支持链式调用
     */
    fun setOnDeleteClick(handler: (AssetsMapModel) -> Unit) = apply {
        this.onDeleteClick = handler
    }

    override fun onInitViewHolder(holder: BaseViewHolder<AdapterMapBinding, AssetsMapModel>) {
        val binding = holder.binding

        // 单击编辑事件
        binding.item.setOnClickListener {
            val item = holder.item
            if (item != null) {
                val position = indexOf(item)
                onEditClick?.invoke(item, position)
            }
        }

        // 长按删除事件
        binding.item.setOnLongClickListener {
            val item = holder.item
            if (item != null) {
                onDeleteClick?.invoke(item)
            }
            true
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
        binding.containmentChip.visibility = if (data.regex) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    override fun areItemsSame(oldItem: AssetsMapModel, newItem: AssetsMapModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsSame(oldItem: AssetsMapModel, newItem: AssetsMapModel): Boolean {
        return oldItem == newItem
    }
}

