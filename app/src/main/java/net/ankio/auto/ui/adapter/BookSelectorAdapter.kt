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
import net.ankio.auto.databinding.AdapterBookBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.utils.ResourceUtils
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BookNameModel

/**
 * 账本选择器适配器
 * 用于显示账本列表，支持三种模式：
 * 1. 普通模式：点击账本项
 * 2. 选择模式：显示收入/支出选择按钮
 * 3. 编辑模式：显示编辑/删除按钮
 *
 * @param showSelect 是否显示选择按钮（收入/支出）
 * @param showEdit 是否显示编辑按钮（编辑/删除）
 * @param onClick 点击事件回调，参数为(账本模型, 操作类型)
 */
class BookSelectorAdapter(
    private val showSelect: Boolean = false,
    private val showEdit: Boolean = false,
    private val onClick: (item: BookNameModel, type: String) -> Unit,
) : BaseAdapter<AdapterBookBinding, BookNameModel>() {


    /**
     * 初始化视图持有者
     * 根据不同模式设置视图可见性和点击事件
     */
    override fun onInitViewHolder(holder: BaseViewHolder<AdapterBookBinding, BookNameModel>) {
        val binding = holder.binding

        // 设置选择容器的可见性（收入/支出按钮容器）
        binding.selectContainer.visibility =
            if (showSelect) android.view.View.VISIBLE else android.view.View.GONE

        // 设置编辑容器的可见性（编辑/删除按钮容器）
        binding.editContainer.visibility =
            if (showEdit) android.view.View.VISIBLE else android.view.View.GONE

        val itemValue = binding.itemValue

        // 获取账本名称文本的布局参数
        val layoutParams = itemValue.layoutParams as ConstraintLayout.LayoutParams

        // 根据是否显示按钮调整文本的垂直偏移量
        // 如果显示按钮，文本位置向上偏移(0.33f)，否则居中(0.5f)
        layoutParams.verticalBias = if (showSelect || showEdit) 0.33f else 0.5f

        itemValue.layoutParams = layoutParams

        // 选择模式：设置收入和支出按钮的点击事件
        if (showSelect) {

            // 点击收入按钮
            binding.income.setOnClickListener {
                onClick(holder.item!!, BillType.Income.name)
            }
            // 点击支出按钮
            binding.expend.setOnClickListener {
                onClick(holder.item!!, BillType.Expend.name)
            }

        } else if (showEdit) {
            // 编辑模式：设置编辑和删除按钮的点击事件

            // 点击编辑按钮
            binding.editButton.setOnClickListener {
                onClick(holder.item!!, "edit")
            }
            // 点击删除按钮
            binding.deleteButton.setOnClickListener {
                onClick(holder.item!!, "delete")
            }

        } else {
            // 普通模式：整个项目可点击
            binding.root.setOnClickListener {
                onClick(holder.item!!, "item")
            }
        }

    }

    /**
     * 绑定数据到视图
     * 设置账本图标和名称
     *
     * @param holder 视图持有者
     * @param data 账本数据模型
     * @param position 位置索引
     */
    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterBookBinding, BookNameModel>,
        data: BookNameModel,
        position: Int
    ) {
        val binding = holder.binding
        // 异步加载账本图标
        holder.launch {
            ResourceUtils.getBookNameDrawable(data.name, holder.context, binding.book)
        }
        // 设置账本名称
        binding.itemValue.text = data.name
    }

    /**
     * 判断两个账本项是否为同一项
     * 用于 DiffUtil 计算差异
     */
    override fun areItemsSame(oldItem: BookNameModel, newItem: BookNameModel): Boolean {
        return oldItem.id == newItem.id
    }

    /**
     * 判断两个账本项的内容是否相同
     * 用于 DiffUtil 计算差异
     */
    override fun areContentsSame(oldItem: BookNameModel, newItem: BookNameModel): Boolean {
        return oldItem == newItem
    }
}

