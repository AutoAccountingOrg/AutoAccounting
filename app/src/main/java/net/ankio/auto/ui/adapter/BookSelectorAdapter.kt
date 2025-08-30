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
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterBookBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.utils.load
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BookNameModel


/**
 * 账本选择器适配器
 * 用于显示账本列表，支持三种模式：
 * 1. 普通模式：点击账本项
 * 2. 选择模式：显示收入/支出选择按钮
 * 3. 编辑模式：显示编辑/设置默认/删除按钮
 *
 * 设计原则（遵循Linus好品味）：
 * 1. 简洁构造：无参数构造函数
 * 2. 链式配置：通过链式调用设置模式和回调
 * 3. 资源管理：销毁时自动清理回调防止内存泄漏
 *
 * @param mode 显示模式，默认为NORMAL
 */
class BookSelectorAdapter(
    private val mode: Mode = Mode.NORMAL
) : BaseAdapter<AdapterBookBinding, BookNameModel>() {

    /**
     * 账本选择器适配器模式枚举
     */
    enum class Mode {
        NORMAL,    // 普通模式：点击整个项目
        SELECT,    // 选择模式：显示收入/支出按钮  
        EDIT       // 编辑模式：显示编辑/删除按钮
    }

    /** 点击事件回调 */
    private var onClick: ((BookNameModel, String) -> Unit)? = null

    /**
     * 设置点击事件监听器
     * @param listener 点击回调，参数为(账本模型, 操作类型)
     *                 操作类型包括: "item", "Income", "Expend", "edit", "setDefault", "delete"
     * @return 当前适配器实例，支持链式调用
     */
    fun setOnItemClickListener(listener: (BookNameModel, String) -> Unit) = apply {
        this.onClick = listener
    }

    /**
     * 初始化视图持有者
     * 根据不同模式设置视图可见性和点击事件
     */
    override fun onInitViewHolder(holder: BaseViewHolder<AdapterBookBinding, BookNameModel>) {
        val binding = holder.binding

        // 根据模式设置容器可见性
        when (mode) {
            Mode.SELECT -> {
                binding.selectContainer.visibility = android.view.View.VISIBLE
                binding.editContainer.visibility = android.view.View.GONE
                setupSelectMode(binding, holder)
            }

            Mode.EDIT -> {
                binding.selectContainer.visibility = android.view.View.GONE
                binding.editContainer.visibility = android.view.View.VISIBLE
                setupEditMode(binding, holder)
            }

            Mode.NORMAL -> {
                binding.selectContainer.visibility = android.view.View.GONE
                binding.editContainer.visibility = android.view.View.GONE
                setupNormalMode(binding, holder)
            }
        }

        // 调整文本垂直位置
        adjustTextPosition(binding)
    }

    /**
     * 设置选择模式的点击事件
     */
    private fun setupSelectMode(
        binding: AdapterBookBinding,
        holder: BaseViewHolder<AdapterBookBinding, BookNameModel>
    ) {
        binding.income.setOnClickListener {
            holder.item?.let { item -> onClick?.invoke(item, BillType.Income.name) }
        }
        binding.expend.setOnClickListener {
            holder.item?.let { item -> onClick?.invoke(item, BillType.Expend.name) }
        }
    }

    /**
     * 设置编辑模式的点击事件
     */
    private fun setupEditMode(
        binding: AdapterBookBinding,
        holder: BaseViewHolder<AdapterBookBinding, BookNameModel>
    ) {
        binding.editButton.setOnClickListener {
            holder.item?.let { item -> onClick?.invoke(item, "edit") }
        }
        binding.defaultButton.setOnClickListener {
            holder.item?.let { item -> onClick?.invoke(item, "setDefault") }
        }
        binding.deleteButton.setOnClickListener {
            holder.item?.let { item -> onClick?.invoke(item, "delete") }
        }
    }

    /**
     * 设置普通模式的点击事件
     */
    private fun setupNormalMode(
        binding: AdapterBookBinding,
        holder: BaseViewHolder<AdapterBookBinding, BookNameModel>
    ) {
        binding.root.setOnClickListener {
            holder.item?.let { item -> onClick?.invoke(item, BillType.Expend.name) }
        }
    }

    /**
     * 调整文本垂直位置
     */
    private fun adjustTextPosition(binding: AdapterBookBinding) {
        val itemValue = binding.itemValue
        val layoutParams = itemValue.layoutParams as ConstraintLayout.LayoutParams

        // 如果显示按钮，文本位置向上偏移，否则居中
        layoutParams.verticalBias = if (mode != Mode.NORMAL) 0.33f else 0.5f
        itemValue.layoutParams = layoutParams
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

        // 设置账本图标和名称
        binding.book.load(data.icon, R.drawable.default_book)
        binding.itemValue.text = data.name

        // 编辑模式下控制设置默认按钮的可见性
        if (mode == Mode.EDIT) {
            val isDefaultBook = data.name == PrefManager.defaultBook
            binding.defaultButton.visibility = if (isDefaultBook) {
                android.view.View.GONE
            } else {
                android.view.View.VISIBLE
            }
        }
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

    /**
     * 适配器被销毁时清理资源
     * 防止内存泄漏
     */
    override fun onDetachedFromRecyclerView(recyclerView: androidx.recyclerview.widget.RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        // 清理回调引用，防止内存泄漏
        onClick = null
    }
}

