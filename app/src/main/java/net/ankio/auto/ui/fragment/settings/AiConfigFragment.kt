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

package net.ankio.auto.ui.fragment.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.navigation.fragment.findNavController
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentAiConfigBinding
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.api.bindAs
import net.ankio.auto.ui.utils.DisplayUtils

/**
 * AI配置页面 - Linus式极简设计
 *
 * 设计原则：
 * 1. 单一职责 - 只负责AI配置相关功能
 * 2. 简洁架构 - 直接使用AiComponent，无冗余布局操作
 * 3. 标准导航 - 使用标准的Fragment导航模式
 * 4. 生命周期管理 - 统一的资源清理
 *
 * 功能说明：
 * - AI服务商和模型配置
 * - API Token设置
 * - 模型列表刷新
 * - 申请页面跳转
 */
class AiConfigFragment : BaseFragment<FragmentAiConfigBinding>() {

    private var aiComponent: AiComponent? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置标题和返回按钮
        binding.toolbar.title = getString(R.string.setting_title_ai_config)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // 设置底部边距适配导航栏
        binding.root.updatePadding(
            bottom = DisplayUtils.getNavigationBarHeight(requireContext())
        )

        // 初始化AI配置组件
        aiComponent = binding.aiComponentContainer.bindAs<AiComponent>()
    }

    override fun onDestroyView() {
        // 清理Toolbar监听器，防止内存泄漏
        // 注意：必须在super.onDestroyView()之前调用，因为super会将binding置空
        binding.toolbar.setNavigationOnClickListener(null)
        // 清理AI组件资源
        aiComponent = null
        super.onDestroyView()
    }
}
