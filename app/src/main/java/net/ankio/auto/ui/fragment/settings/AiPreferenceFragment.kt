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
import android.text.InputType
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import androidx.navigation.fragment.findNavController
import net.ankio.auto.R
import net.ankio.auto.ui.api.BasePreferenceFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.EditorDialogBuilder
import net.ankio.auto.utils.PrefManager

/**
 * AI功能设置页面 - Linus式极简设计
 *
 * 设计原则：
 * 1. 单一职责 - 只负责AI功能开关设置
 * 2. 简洁导航 - AI配置独立到专门页面
 * 3. 消除复杂性 - 移除布局操作，使用标准preference
 * 4. 向后兼容 - 保持所有原有功能不变
 *
 * 功能说明：
 * - AI功能开关（账单识别、分类识别、资产映射、月度摘要）
 * - AI摘要提示词设置
 * - 跳转到AI配置页面（模型、API设置）
 */
class AiPreferenceFragment : BasePreferenceFragment() {

    override fun getTitleRes(): Int = R.string.setting_title_ai

    override fun getPreferencesRes(): Int = R.xml.settings_ai

    override fun createDataStore(): PreferenceDataStore = AiPreferenceDataStore()

    /**
     * AI功能专用的数据存储类
     */
    inner class AiPreferenceDataStore : PreferenceDataStore() {

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            return when (key) {
                "aiBillRecognition" -> PrefManager.aiBillRecognition
                "aiCategoryRecognition" -> PrefManager.aiCategoryRecognition
                "aiAssetMapping" -> PrefManager.aiAssetMapping
                "aiMonthlySummary" -> PrefManager.aiMonthlySummary
                else -> defValue
            }
        }

        override fun putBoolean(key: String?, value: Boolean) {
            when (key) {
                "aiBillRecognition" -> PrefManager.aiBillRecognition = value
                "aiCategoryRecognition" -> PrefManager.aiCategoryRecognition = value
                "aiAssetMapping" -> PrefManager.aiAssetMapping = value
                "aiMonthlySummary" -> PrefManager.aiMonthlySummary = value
            }
        }


    }


    /**
     * 设置自定义偏好行为（重写BasePreferenceFragment方法）
     */
    override fun setupPreferences() {
        super.setupPreferences()

        // AI配置页面跳转
        findPreference<Preference>("aiConfig")?.setOnPreferenceClickListener {
            // 使用目的地 ID 导航，避免当前目的地为 NavGraph 时解析不到 action
            findNavController().navigate(R.id.aiConfigFragment)
            true
        }

        // AI摘要提示词设置 - 使用自定义编辑弹窗
        findPreference<Preference>("aiSummaryPrompt")?.setOnPreferenceClickListener {
            showSummaryPromptEditDialog()
            true
        }

        // 更新显示的摘要文本
        updatePreferenceSummaries()
    }

    /**
     * 显示AI摘要提示词编辑弹窗
     */
    private fun showSummaryPromptEditDialog() {
        BaseSheetDialog.create<EditorDialogBuilder>(requireContext())
            .setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE)
            .setTitleInt(R.string.setting_ai_summary_prompt)
            .setMessage(PrefManager.aiSummaryPrompt)
            .setEditorPositiveButton(R.string.sure_msg) { result ->
                PrefManager.aiSummaryPrompt = result
                updatePreferenceSummaries()
            }
            .setNegativeButton(R.string.cancel_msg, null)
            .show()
    }

    /**
     * 更新设置项的摘要显示
     */
    private fun updatePreferenceSummaries() {
        // AI摘要提示词 - 显示当前提示词内容的前50个字符
        findPreference<Preference>("aiSummaryPrompt")?.apply {
            val promptText = PrefManager.aiSummaryPrompt
            summary = if (promptText.isNotEmpty()) {
                if (promptText.length > 50) {
                    "${promptText.take(50)}..."
                } else {
                    promptText
                }
            } else {
                getString(R.string.setting_ai_summary_prompt_summary)
            }
        }
    }


}
