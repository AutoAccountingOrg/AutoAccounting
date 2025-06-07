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

package net.ankio.auto.ui.fragment.plugin.home

import androidx.lifecycle.Lifecycle
import com.google.android.material.elevation.SurfaceColors
import net.ankio.auto.databinding.CardRuleVersionBinding
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.R

class RuleVersionCardComponent(binding: CardRuleVersionBinding, private val lifecycle: Lifecycle) :
    BaseComponent<CardRuleVersionBinding>(binding, lifecycle) {

    override fun init() {
        super.init()
        binding.root.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(context))
        // 设置更新按钮点击事件
        binding.updateButton.setOnClickListener {
            updateRules()
        }
    }

    override fun resume() {
        super.resume()
        // 更新显示信息
        updateDisplay()
    }

    private fun updateDisplay() {

        val version = PrefManager.ruleVersion
        val lastUpdate = PrefManager.ruleUpdate

        binding.titleText.text = context.getString(R.string.rule_version_title, version)
        binding.subtitleText.text = context.getString(R.string.rule_version_last_update, lastUpdate)
    }

    private fun updateRules() {
        // TODO: 实现规则更新逻辑
        // 1. 显示加载状态
        binding.updateButton.isEnabled = false
        binding.updateButton.alpha = 0.5f

        // 2. 执行更新操作
        // 3. 更新完成后刷新显示
        updateDisplay()

        // 4. 恢复按钮状态
        binding.updateButton.isEnabled = true
        binding.updateButton.alpha = 1.0f
    }
} 