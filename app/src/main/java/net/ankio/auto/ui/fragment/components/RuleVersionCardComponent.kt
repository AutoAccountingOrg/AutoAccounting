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

package net.ankio.auto.ui.fragment.components

import com.google.android.material.elevation.SurfaceColors
import net.ankio.auto.R
import net.ankio.auto.databinding.CardRuleVersionBinding
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.ui.utils.RuleUpdateHelper
import net.ankio.auto.utils.PrefManager

/**
 * 规则版本卡片组件
 *
 * 负责管理规则版本的显示、检查和更新功能。该组件提供以下主要功能：
 * 1. 显示当前规则版本和最后更新时间
 * 2. 检查规则更新并提示用户
 * 3. 下载并安装新的规则包
 * 4. 同步分类映射数据
 *
 * @param binding 卡片视图绑定对象
 */
class RuleVersionCardComponent(
    binding: CardRuleVersionBinding
) : BaseComponent<CardRuleVersionBinding>(binding) {


    /**
     * 初始化组件
     * 设置UI样式、事件监听器和自动检查更新
     */
    override fun onComponentCreate() {
        super.onComponentCreate()
        // 设置卡片背景颜色为Material Design的表面颜色
        binding.root.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(context))

        // 设置更新按钮点击事件
        binding.updateButton.setOnClickListener {
            launch {
                setUpdateButtonEnabled(false)
                try {
                    RuleUpdateHelper.checkAndShow(context, true) {
                        updateDisplay()
                    }
                } finally {
                    setUpdateButtonEnabled(true)
                }
            }
        }

        // 长按更新按钮重置版本号并强制更新
        binding.updateButton.setOnLongClickListener {
            PrefManager.ruleVersion = ""
            launch {
                setUpdateButtonEnabled(false)
                try {
                    RuleUpdateHelper.checkAndShow(context, true) {
                        updateDisplay()
                    }
                } finally {
                    setUpdateButtonEnabled(true)
                }
            }
            true
        }

        // 如果开启了自动检查更新，则执行更新检查
        if (RuleUpdateHelper.isAutoCheckEnabled()) {
            launch {
                setUpdateButtonEnabled(false)
                try {
                    RuleUpdateHelper.checkAndShow(context, false) {
                        updateDisplay()
                    }
                } finally {
                    setUpdateButtonEnabled(true)
                }
            }
        }
    }

    /**
     * 组件恢复时更新显示内容
     */
    override fun onComponentResume() {
        super.onComponentResume()
        updateDisplay()
    }

    /**
     * 更新显示内容
     * 显示当前规则版本和最后更新时间
     */
    private fun updateDisplay() {
        if (!uiReady()) return
        binding.titleText.text =
            context.getString(R.string.rule_version_title, PrefManager.ruleVersion)
        binding.subtitleText.text =
            context.getString(R.string.rule_version_last_update, PrefManager.ruleUpdate)
    }

    /**
     * 设置更新按钮的启用状态
     *
     * @param enabled 是否启用按钮
     */
    private fun setUpdateButtonEnabled(enabled: Boolean) {
        if (!uiReady()) return
        binding.updateButton.isEnabled = enabled
        binding.updateButton.alpha = if (enabled) 1.0f else 0.5f
    }


}
