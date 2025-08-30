/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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
import net.ankio.auto.databinding.AdapterReplacePreviewBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.model.ReplaceItem

/**
 * 替换预览适配器
 * 提供替换项的显示和交互功能，支持选择和删除操作
 *
 * 设计原则（遵循Linus好品味）：
 * 1. 简洁构造：无参数构造函数
 * 2. 链式配置：通过链式调用设置监听器
 * 3. 消除特殊情况：统一的选择和删除处理逻辑
 */
class ReplacePreviewAdapter : BaseAdapter<AdapterReplacePreviewBinding, ReplaceItem>() {

    private var onItemDelete: ((ReplaceItem, Int) -> Unit)? = null

    /**
     * 设置删除监听器
     * @param listener 删除回调，参数为(替换项, 位置)
     */
    fun setOnItemDeleteListener(listener: (ReplaceItem, Int) -> Unit) = apply {
        this.onItemDelete = listener
    }

    /**
     * 初始化视图持有者
     * 设置删除按钮的事件监听
     */
    override fun onInitViewHolder(holder: BaseViewHolder<AdapterReplacePreviewBinding, ReplaceItem>) {
        val binding = holder.binding

        // 设置删除按钮点击事件
        binding.btnDelete.setOnClickListener {
            holder.item?.let { item ->
                val position = holder.bindingAdapterPosition
                if (position != -1) {
                    onItemDelete?.invoke(item, position)
                }
            }
        }
    }

    /**
     * 绑定数据到视图
     * 显示替换预览文本
     *
     * @param holder 视图持有者
     * @param data 替换项数据
     * @param position 位置索引
     */
    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterReplacePreviewBinding, ReplaceItem>,
        data: ReplaceItem,
        position: Int
    ) {
        val binding = holder.binding

        // 设置替换预览文本
        binding.tvPreview.text = data.getPreviewText()
    }

    /**
     * 判断两个替换项是否为同一项
     * 用于 DiffUtil 计算差异
     */
    override fun areItemsSame(oldItem: ReplaceItem, newItem: ReplaceItem): Boolean {
        return oldItem.from == newItem.from && oldItem.to == newItem.to
    }

    /**
     * 判断两个替换项的内容是否相同
     * 用于 DiffUtil 计算差异
     */
    override fun areContentsSame(oldItem: ReplaceItem, newItem: ReplaceItem): Boolean {
        return oldItem == newItem
    }


}
