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
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.elevation.SurfaceColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterRuleBinding
import net.ankio.auto.http.api.CategoryRuleAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.api.bindAs
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.fragment.components.CategoryRuleEditComponent
import net.ankio.auto.ui.theme.DynamicColors
import org.ezbook.server.db.model.CategoryRuleModel

/**
 * 分类规则适配器 - 优化版本，使用 CategoryRuleEditComponent 处理复杂的规则展示逻辑
 *
 * 设计原则（遵循Linus好品味）：
 * 1. 单一职责：适配器只负责列表管理，规则展示委托给 CategoryRuleEditComponent
 * 2. 消除重复：移除所有与 CategoryRuleEditComponent 重复的逻辑
 * 3. 类型安全：正确的生命周期管理
 * 4. 简洁实现：3层嵌套以内，逻辑清晰
 *
 * 重构要点：
 * - 移除了重复的规则展示代码，使用 CategoryRuleEditComponent 替代
 * - 使用 include 标签集成 CategoryRuleEditComponent 布局
 * - 委托规则展示逻辑给专门的组件
 * - 保持原有功能：编辑和删除操作完全兼容
 * - 遵循"Never break userspace"原则：所有原有功能都保留
 *
 * 批量删除功能：
 * - 支持批量选择模式切换
 * - 使用 Set<Long> 维护选中项，避免污染数据模型
 * - 批量模式下禁用编辑和单个删除功能
 */
class CategoryRuleAdapter(
    val activity: FragmentActivity,
    val onClickEdit: (CategoryRuleModel, Int) -> Unit = { _, _ -> }
) : BaseAdapter<AdapterRuleBinding, CategoryRuleModel>() {

    /**
     * 批量选择模式标志
     */
    var isSelectionMode = false
        set(value) {
            field = value
            if (!value) {
                selectedIds.clear()
            }
            notifyDataSetChanged()
        }

    /**
     * 选中的规则ID集合
     */
    val selectedIds = mutableSetOf<Long>()

    /**
     * 选择状态变化回调
     */
    var onSelectionChanged: ((Int) -> Unit)? = null


    override fun onInitViewHolder(holder: BaseViewHolder<AdapterRuleBinding, CategoryRuleModel>) {
        val binding = holder.binding

        // 设置卡片背景色
        binding.groupCard.setCardBackgroundColor(DynamicColors.SurfaceColor1)

        // 卡片点击事件：批量模式下用于选择，普通模式无操作
        binding.groupCard.setOnClickListener {
            if (isSelectionMode) {
                val item = holder.item ?: return@setOnClickListener
                toggleSelection(item.id)
                updateSelectionUI(binding, item.id)
            }
        }

        // 编辑规则点击事件
        binding.editRule.setOnClickListener {
            if (!isSelectionMode) {
                val item = holder.item!!
                val position = indexOf(item)
                onClickEdit(item, position)
            }
        }

        // 删除规则点击事件 - 恢复原有的删除功能
        binding.deleteData.setOnClickListener {
            if (!isSelectionMode) {
                val item = holder.item!!

                BaseSheetDialog.create<BottomSheetDialogBuilder>(activity)
                    .setTitle(activity.getString(R.string.delete_data))
                    .setMessage(activity.getString(R.string.delete_msg))
                    .setPositiveButton(activity.getString(R.string.sure_msg)) { _, _ ->
                        launchInAdapter {
                            withContext(Dispatchers.IO) {
                                CategoryRuleAPI.remove(item.id)
                            }
                            removeItem(item)
                        }
                    }
                    .setNegativeButton(activity.getString(R.string.cancel_msg)) { _, _ -> }
                    .show()
            }
        }

        // 复选框点击事件
        binding.selectionCheckbox.setOnClickListener {
            val item = holder.item ?: return@setOnClickListener
            toggleSelection(item.id)
        }
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterRuleBinding, CategoryRuleModel>,
        data: CategoryRuleModel,
        position: Int
    ) {
        val binding = holder.binding

        // 显示自动创建标识
        binding.autoCreate.visibility = if (data.creator == "user") {
            View.GONE
        } else {
            View.VISIBLE
        }

        // 批量选择模式UI控制
        if (isSelectionMode) {
            binding.selectionCheckbox.visibility = View.VISIBLE
            binding.selectionCheckbox.isChecked = selectedIds.contains(data.id)
            binding.editRule.visibility = View.GONE
            binding.deleteData.visibility = View.GONE
            // 选中状态的背景高亮
            binding.groupCard.alpha = if (selectedIds.contains(data.id)) 0.85f else 1.0f
        } else {
            binding.selectionCheckbox.visibility = View.GONE
            binding.editRule.visibility = View.VISIBLE
            binding.deleteData.visibility = View.VISIBLE
            binding.groupCard.alpha = 1.0f
        }

        // 使用 CategoryRuleEditComponent 处理规则展示
        setupCategoryRuleEditComponent(holder, data)
    }

    /**
     * 设置 CategoryRuleEditComponent 来展示分类规则
     *
     * 功能说明：
     * 1. 创建 CategoryRuleEditComponent 实例
     * 2. 设置为只读模式，仅展示规则内容
     * 3. 委托所有复杂的规则展示逻辑给 CategoryRuleEditComponent
     */
    private fun setupCategoryRuleEditComponent(
        holder: BaseViewHolder<AdapterRuleBinding, CategoryRuleModel>,
        data: CategoryRuleModel
    ) {
        val binding = holder.binding
        // 获取 CategoryRuleEditComponent 的绑定
        val categoryRuleEditComponent =
            binding.categoryComponent.bindAs<CategoryRuleEditComponent>()
        // 设置规则模型，只读模式展示
        categoryRuleEditComponent.setRuleModel(data, readOnly = true)

    }

    override fun areItemsSame(oldItem: CategoryRuleModel, newItem: CategoryRuleModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsSame(oldItem: CategoryRuleModel, newItem: CategoryRuleModel): Boolean {
        return oldItem == newItem
    }

    /**
     * 切换选中状态
     */
    private fun toggleSelection(id: Long) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id)
        } else {
            selectedIds.add(id)
        }
        onSelectionChanged?.invoke(selectedIds.size)
    }

    /**
     * 更新选中状态UI
     */
    private fun updateSelectionUI(binding: AdapterRuleBinding, id: Long) {
        binding.selectionCheckbox.isChecked = selectedIds.contains(id)
        binding.groupCard.alpha = if (selectedIds.contains(id)) 0.85f else 1.0f
    }

    /**
     * 全选/取消全选
     */
    fun toggleSelectAll() {
        if (selectedIds.size == itemCount) {
            selectedIds.clear()
        } else {
            selectedIds.clear()
            selectedIds.addAll(getItems().map { it.id })
        }
        notifyDataSetChanged()
        onSelectionChanged?.invoke(selectedIds.size)
    }

    /**
     * 移除选中的项目
     */
    fun removeSelectedItems() {
        val itemsToRemove = getItems().filter { selectedIds.contains(it.id) }
        itemsToRemove.forEach { removeItem(it) }
        selectedIds.clear()
        onSelectionChanged?.invoke(0)
    }
}

