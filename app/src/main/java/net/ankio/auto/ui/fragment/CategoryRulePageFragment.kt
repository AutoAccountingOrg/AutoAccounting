/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

package net.ankio.auto.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentCategoryRulePageBinding
import net.ankio.auto.http.api.CategoryRuleAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.CategoryRuleAdapter
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.components.WrapContentLinearLayoutManager
import org.ezbook.server.db.model.CategoryRuleModel

/**
 * 分类规则页面Fragment
 *
 * 该Fragment是RuleManageFragment的子页面，负责展示和管理分类规则，包括：
 * - 分类规则列表展示
 * - 规则编辑功能
 * - 规则删除功能
 * - 添加新规则功能
 *
 * @author ankio
 */
class CategoryRulePageFragment :
    BasePageFragment<CategoryRuleModel, FragmentCategoryRulePageBinding>() {

    /**
     * 分类规则适配器
     */
    private lateinit var categoryRuleAdapter: CategoryRuleAdapter

    /**
     * 拖拽排序助手
     */
    private lateinit var itemTouchHelper: ItemTouchHelper

    /**
     * 标记是否发生了排序变化
     */
    private var sortChanged = false

    /**
     * 加载数据的主要方法
     * 从API获取分类规则数据列表
     *
     * @return 分类规则数据模型列表
     */
    override suspend fun loadData(): List<CategoryRuleModel> = CategoryRuleAPI.list(page, pageSize)

    /**
     * 创建数据适配器
     * 配置RecyclerView的布局管理器和适配器，并启用拖拽排序
     *
     * @return 配置好的CategoryRuleAdapter实例
     */
    override fun onCreateAdapter(): RecyclerView.Adapter<*> {
        val recyclerView = binding.statusPage.contentView!!
        recyclerView.layoutManager = WrapContentLinearLayoutManager(requireContext())

        // 创建适配器
        categoryRuleAdapter = CategoryRuleAdapter(
            requireActivity(),
            onClickEdit = { item, position ->
                // 点击编辑规则
                val bundle = Bundle().apply {
                    putString("data", item.toJson())
                    putLong("ruleId", item.id)
                }
                Logger.d("Navigate to category rule edit with item: ${item.id}")
                // 使用目的地 ID 导航，避免当前目的地为 NavGraph 时解析不到 action
                findNavController().navigate(R.id.categoryRuleEditFragment, bundle)
            }
        )

        // 设置拖拽排序
        setupItemTouchHelper(recyclerView)

        return categoryRuleAdapter
    }

    /**
     * 设置ItemTouchHelper，支持拖拽排序
     *
     * @param recyclerView RecyclerView实例
     */
    private fun setupItemTouchHelper(recyclerView: RecyclerView) {
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, // 支持上下拖拽
            0 // 不支持滑动删除
        ) {
            /**
             * 拖拽移动回调
             */
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.bindingAdapterPosition
                val toPosition = target.bindingAdapterPosition

                if (fromPosition != RecyclerView.NO_POSITION && toPosition != RecyclerView.NO_POSITION) {
                    Logger.d("拖拽移动：从位置 $fromPosition 到 $toPosition")
                    // 调用适配器的交换方法
                    categoryRuleAdapter.swapItems(fromPosition, toPosition)
                    sortChanged = true
                    return true
                }
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // 不支持滑动删除
            }

            /**
             * 拖拽状态改变：添加视觉反馈
             */
            override fun onSelectedChanged(
                viewHolder: RecyclerView.ViewHolder?,
                actionState: Int
            ) {
                super.onSelectedChanged(viewHolder, actionState)
                // 拖拽时提升视图并降低透明度
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.9f
                    viewHolder?.itemView?.elevation = 8f
                }
            }

            /**
             * 拖拽结束：恢复视图并保存排序
             */
            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                // 恢复视图状态
                viewHolder.itemView.alpha = 1.0f
                viewHolder.itemView.elevation = 0f
                // 拖拽结束后保存排序
                if (sortChanged) {
                    saveSortOrder()
                    sortChanged = false
                }
            }

            /**
             * 启用长按拖拽
             */
            override fun isLongPressDragEnabled(): Boolean = true
        }

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    /**
     * 保存排序顺序到服务器
     * 遍历所有规则，更新其 sort 字段并提交
     */
    private fun saveSortOrder() {
        launch {
            try {
                val items = categoryRuleAdapter.getItems()

                Logger.d("开始保存分类规则排序，共 ${items.size} 条")

                withContext(Dispatchers.IO) {
                    // 批量更新排序字段
                    items.forEachIndexed { index, rule ->
                        rule.sort = index
                        CategoryRuleAPI.put(rule)
                    }
                }

                Logger.d("分类规则排序保存成功")
            } catch (e: Exception) {
                Logger.e("保存分类规则排序失败: ${e.message}", e)
            }
        }
    }

    /**
     * Fragment视图创建完成后的初始化
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAddButton()
    }

    /**
     * 设置添加按钮功能
     */
    private fun setupAddButton() {
        binding.addButton.setOnClickListener {
            Logger.d("Navigate to create new category rule")
            // 使用目的地 ID 导航
            findNavController().navigate(R.id.categoryRuleEditFragment)
        }
    }
}
