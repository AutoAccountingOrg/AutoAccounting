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
import androidx.recyclerview.widget.RecyclerView
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
     * 加载数据的主要方法
     * 从API获取分类规则数据列表
     *
     * @return 分类规则数据模型列表
     */
    override suspend fun loadData(): List<CategoryRuleModel> = CategoryRuleAPI.list(page, pageSize)

    /**
     * 创建数据适配器
     * 配置RecyclerView的布局管理器和适配器
     *
     * @return 配置好的CategoryRuleAdapter实例
     */
    override fun onCreateAdapter(): RecyclerView.Adapter<*> {
        val recyclerView = binding.statusPage.contentView!!
        recyclerView.layoutManager = WrapContentLinearLayoutManager(requireContext())
        return CategoryRuleAdapter(requireActivity()) { item, position ->
            // 点击编辑规则
            val bundle = Bundle().apply {
                putString("data", item.toJson())
                putLong("ruleId", item.id)
            }
            Logger.d("Navigate to category rule edit with item: ${item.id}")
            findNavController().navigate(
                R.id.action_ruleManageFragment_to_categoryRuleEditFragment,
                bundle
            )
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
            findNavController().navigate(R.id.action_ruleManageFragment_to_categoryRuleEditFragment)
        }
    }
}
