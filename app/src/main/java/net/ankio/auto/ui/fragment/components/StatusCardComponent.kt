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

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.databinding.CardStatusBinding
import net.ankio.auto.service.CoreService
import net.ankio.auto.service.OcrService
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.ui.theme.DynamicColors
import net.ankio.auto.ui.utils.AppUpdateHelper
import net.ankio.auto.ui.utils.RuleUpdateHelper
import net.ankio.auto.ui.utils.toDrawable
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.xposed.XposedModule

class StatusCardComponent(binding: CardStatusBinding) :
    BaseComponent<CardStatusBinding>(binding) {

    override fun onComponentCreate() {
        super.onComponentCreate()

        // 整卡点击：检查规则更新
        binding.cardContent.setOnClickListener {
            launch {
                RuleUpdateHelper.checkAndShow(context, true) { updateRuleTexts() }
            }
        }

        // 长按：强制规则更新
        binding.cardContent.setOnLongClickListener {
            PrefManager.ruleVersion = ""
            launch {
                RuleUpdateHelper.checkAndShow(context, true) { updateRuleTexts() }
            }
            true
        }

        // 初始化状态显示
        updateActiveStatus()
        updateRuleTexts()
    }

    override fun onComponentResume() {
        super.onComponentResume()
        updateActiveStatus()
        updateRuleTexts()
        autoCheckUpdateIfNeeded()
    }

    /**
     * 当前模式是否处于"工作中"
     */
    private fun isCurrentModeActive(): Boolean {
        return when (PrefManager.workMode) {
            WorkMode.Ocr -> OcrService.serverStarted
            WorkMode.LSPatch -> CoreService.isRunning(context)
            WorkMode.Xposed -> XposedModule.active()
        }
    }

    /**
     * 统一更新激活状态显示（第一行：工作状态 + 模式标签）
     */
    private fun updateActiveStatus() {
        val isActive = isCurrentModeActive()
        val versionName = BuildConfig.VERSION_NAME

        // 工作状态文本
        val statusText = if (isActive) {
            context.getString(R.string.status_working)
        } else {
            context.getString(R.string.status_inactive)
        }
        binding.titleText.text = statusText

        binding.modeText.text = PrefManager.workMode.name

        if (isActive) {
            setActive(
                backgroundColor = DynamicColors.PrimaryContainer,
                textColor = DynamicColors.OnPrimaryContainer,
                drawable = R.drawable.home_active_success
            )
        } else {
            setActive(
                backgroundColor = DynamicColors.SurfaceColor3,
                textColor = DynamicColors.Primary,
                drawable = R.drawable.home_active_error
            )
        }

        // 第二行：App 版本
        binding.subtitleText.text = context.getString(R.string.app_version_colon, versionName)
    }

    /**
     * 更新第三、四行：规则版本与规则更新时间
     */
    private fun updateRuleTexts() {
        binding.ruleVersionText.text =
            context.getString(R.string.rule_version_colon, PrefManager.ruleVersion)
        binding.ruleUpdateText.text =
            context.getString(R.string.rule_update_colon, PrefManager.ruleUpdate)
    }

    private fun setActive(
        @ColorInt backgroundColor: Int,
        @ColorInt textColor: Int,
        @DrawableRes drawable: Int,
    ) {
        binding.cardContent.setBackgroundColor(backgroundColor)
        binding.iconView.setImageDrawable(drawable.toDrawable())
        binding.iconView.setColorFilter(textColor)
        binding.titleText.setTextColor(textColor)
        binding.modeText.setTextColor(DynamicColors.OnPrimary)
        binding.subtitleText.setTextColor(textColor)
        binding.ruleVersionText.setTextColor(textColor)
        binding.ruleUpdateText.setTextColor(textColor)

    }

    /**
     * 自动检查更新：规则 + 应用
     */
    private fun autoCheckUpdateIfNeeded() {
        // 规则自动检查
        if (RuleUpdateHelper.isAutoCheckEnabled()) {
            launch { RuleUpdateHelper.checkAndShow(context, false) { updateRuleTexts() } }
        }
        // 应用自动检查
        if (AppUpdateHelper.isAutoCheckEnabled()) {
            launch { AppUpdateHelper.checkAndShow(context, false) }
        }
    }
}

