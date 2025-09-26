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
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentRuleManageBinding
import net.ankio.auto.ui.api.BaseFragment

/**
 * 规则管理主Fragment
 *
 * 该Fragment作为ViewPager容器，整合以下功能：
 * - Tab1: 数据规则管理 (RuleDataPageFragment)
 * - Tab2: 分类规则管理 (CategoryRulePageFragment)
 * - MaterialToolbar与TabLayout融合显示
 * - 统一的工具栏菜单管理
 *
 * @author ankio
 */
class RuleManageFragment : BaseFragment<FragmentRuleManageBinding>() {

    /** ViewPager适配器 */
    private lateinit var pagerAdapter: RulePagerAdapter

    // TabLayoutMediator 引用，确保 onDestroyView 时解除绑定
    private var tabMediator: TabLayoutMediator? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeRuleTabs()
    }


    /**
     * 初始化规则Tab页面
     */
    private fun initializeRuleTabs() {
        // 先解除旧绑定，避免重复附着导致泄漏
        tabMediator?.detach()
        tabMediator = null
        binding.viewPager.adapter = null

        // 设置ViewPager适配器
        pagerAdapter = RulePagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        // 连接TabLayout和ViewPager2（保存引用，便于销毁时detach）
        tabMediator = TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_data_rules)
                1 -> getString(R.string.tab_category_rules)
                else -> ""
            }
        }.apply { attach() }
    }

    override fun onDestroyView() {
        // 解除 TabLayoutMediator 绑定并清空适配器，防止内存泄漏
        try {
            tabMediator?.detach()
        } catch (_: Exception) {
        }
        tabMediator = null
        try {
            binding.viewPager.adapter = null
        } catch (_: Exception) {
        }
        super.onDestroyView()
    }


    /**
     * 规则Tab页面适配器
     */
    private class RulePagerAdapter(
        private val parentFragment: Fragment
    ) : FragmentStateAdapter(parentFragment) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> RuleDataPageFragment() // 数据规则页面
                1 -> CategoryRulePageFragment() // 分类规则页面
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
    }
}
