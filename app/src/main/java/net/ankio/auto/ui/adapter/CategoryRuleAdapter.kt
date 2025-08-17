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
import androidx.fragment.app.FragmentActivity
import com.google.android.material.elevation.SurfaceColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterRuleBinding
import net.ankio.auto.http.api.CategoryRuleAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.api.bindAs
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.fragment.category.CategoryComponent
import org.ezbook.server.db.model.CategoryRuleModel

/**
 * 分类规则适配器 - 优化版本，使用 CategoryComponent 处理复杂的规则展示逻辑
 *
 * 设计原则（遵循Linus好品味）：
 * 1. 单一职责：适配器只负责列表管理，规则展示委托给 CategoryComponent
 * 2. 消除重复：移除所有与 CategoryComponent 重复的逻辑
 * 3. 类型安全：正确的生命周期管理
 * 4. 简洁实现：3层嵌套以内，逻辑清晰
 *
 * 重构要点：
 * - 移除了重复的规则展示代码，使用 CategoryComponent 替代
 * - 使用 include 标签集成 CategoryComponent 布局
 * - 委托规则展示逻辑给专门的组件
 * - 保持原有功能：编辑和删除操作完全兼容
 * - 遵循"Never break userspace"原则：所有原有功能都保留
 */
class CategoryRuleAdapter(
    val activity: FragmentActivity,
    val onClickEdit: (CategoryRuleModel, Int) -> Unit = { _, _ -> }
) : BaseAdapter<AdapterRuleBinding, CategoryRuleModel>() {


    override fun onInitViewHolder(holder: BaseViewHolder<AdapterRuleBinding, CategoryRuleModel>) {
        val binding = holder.binding

        // 设置卡片背景色
        binding.groupCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(activity))

        // 编辑规则点击事件
        binding.editRule.setOnClickListener {
            val item = holder.item!!
            val position = indexOf(item)
            onClickEdit(item, position)
        }

        // 删除规则点击事件 - 恢复原有的删除功能
        binding.deleteData.setOnClickListener {
            val item = holder.item!!

            // 确保有FragmentActivity上下文才能显示对话框
            val fragmentActivity = activity as? FragmentActivity ?: return@setOnClickListener

            BottomSheetDialogBuilder(fragmentActivity)
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

        // 使用 CategoryComponent 处理规则展示
        setupCategoryComponent(holder, data)
    }

    /**
     * 设置 CategoryComponent 来展示分类规则
     *
     * 功能说明：
     * 1. 创建 CategoryComponent 实例
     * 2. 设置为只读模式，仅展示规则内容
     * 3. 委托所有复杂的规则展示逻辑给 CategoryComponent
     */
    private fun setupCategoryComponent(
        holder: BaseViewHolder<AdapterRuleBinding, CategoryRuleModel>,
        data: CategoryRuleModel
    ) {
        val binding = holder.binding
        // 获取 CategoryComponent 的绑定
        val categoryComponent =
            binding.categoryComponent.bindAs<CategoryComponent>(activity.lifecycle, activity)
        // 设置规则模型，只读模式展示
        categoryComponent.setRuleModel(data, readOnly = true)

        Logger.d("CategoryComponent 设置完成，展示规则: ${data.id}")
    }

    override fun areItemsSame(oldItem: CategoryRuleModel, newItem: CategoryRuleModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsSame(oldItem: CategoryRuleModel, newItem: CategoryRuleModel): Boolean {
        return oldItem == newItem
    }
}

