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

import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import net.ankio.auto.R
import net.ankio.auto.ui.api.BasePreferenceFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.EditorDialogBuilder
import net.ankio.auto.utils.PrefManager
import rikka.material.preference.MaterialSwitchPreference

/**
 * AI助理设置页面
 * 包含：AI配置、提示词管理、AI功能
 */
class AIAssistantPreferenceFragment : BasePreferenceFragment() {

    override fun getTitleRes(): Int = R.string.setting_title_ai_assistant

    override fun getPreferencesRes(): Int = R.xml.settings_ai_assistant

    override fun createDataStore(): PreferenceDataStore = AIAssistantDataStore()

    /**
     * 设置自定义弹窗处理
     */
    override fun setupPreferences() {
        super.setupPreferences()

        // AI配置入口
        findPreference<Preference>("aiConfig")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.aiConfigFragment)
            true
        }

        // AI总结提示词设置
        findPreference<Preference>("aiSummaryPrompt")?.apply {
            setOnPreferenceClickListener {
                showAiSummaryPromptDialog()
                true
            }
            updateAiSummaryPromptSummary()
        }

        // AI账单识别提示词设置
        findPreference<Preference>("aiBillRecognitionPrompt")?.apply {
            setOnPreferenceClickListener {
                showAiBillRecognitionPromptDialog()
                true
            }
            updateAiBillRecognitionPromptSummary()
        }

        // AI资产映射提示词设置
        findPreference<Preference>("aiAssetMappingPrompt")?.apply {
            setOnPreferenceClickListener {
                showAiAssetMappingPromptDialog()
                true
            }
            updateAiAssetMappingPromptSummary()
        }

        // AI分类识别提示词设置
        findPreference<Preference>("aiCategoryRecognitionPrompt")?.apply {
            setOnPreferenceClickListener {
                showAiCategoryRecognitionPromptDialog()
                true
            }
            updateAiCategoryRecognitionPromptSummary()
        }
    }

    override fun onResume() {
        super.onResume()
        findPreference<Preference>("aiSummaryPrompt")?.updateAiSummaryPromptSummary()
        findPreference<Preference>("aiBillRecognitionPrompt")?.updateAiBillRecognitionPromptSummary()
        findPreference<Preference>("aiAssetMappingPrompt")?.updateAiAssetMappingPromptSummary()
        findPreference<Preference>("aiCategoryRecognitionPrompt")?.updateAiCategoryRecognitionPromptSummary()
        if (!PrefManager.featureAiAvailable) {
            findPreference<Preference>("aiSummaryPrompt")?.isEnabled = false
            findPreference<Preference>("aiBillRecognitionPrompt")?.isEnabled = false
            findPreference<Preference>("aiAssetMappingPrompt")?.isEnabled = false
            findPreference<Preference>("aiCategoryRecognitionPrompt")?.isEnabled =
                false
        }
    }

    /**
     * 显示AI总结提示词编辑对话框
     */
    private fun showAiSummaryPromptDialog() {
        BaseSheetDialog.create<EditorDialogBuilder>(requireContext())
            .setTitleInt(R.string.setting_ai_summary_prompt)
            .setMultiLine(minLines = 10, maxLines = 20)
            .setMessage(PrefManager.aiSummaryPrompt)
            .setEditorPositiveButton(R.string.btn_confirm) { prompt ->
                PrefManager.aiSummaryPrompt = prompt
                findPreference<Preference>("aiSummaryPrompt")?.updateAiSummaryPromptSummary()
            }
            .setNegativeButton(R.string.btn_cancel) { _, _ -> }
            .show()
    }

    /**
     * 显示AI账单识别提示词编辑对话框
     */
    private fun showAiBillRecognitionPromptDialog() {
        BaseSheetDialog.create<EditorDialogBuilder>(requireContext())
            .setTitleInt(R.string.setting_ai_bill_recognition_prompt)
            .setMultiLine(minLines = 10, maxLines = 20)
            .setMessage(PrefManager.aiBillRecognitionPrompt)
            .setEditorPositiveButton(R.string.btn_confirm) { prompt ->
                PrefManager.aiBillRecognitionPrompt = prompt
                findPreference<Preference>("aiBillRecognitionPrompt")?.updateAiBillRecognitionPromptSummary()
            }
            .setNegativeButton(R.string.btn_cancel) { _, _ -> }
            .show()
    }

    /**
     * 显示AI资产映射提示词编辑对话框
     */
    private fun showAiAssetMappingPromptDialog() {
        BaseSheetDialog.create<EditorDialogBuilder>(requireContext())
            .setTitleInt(R.string.setting_ai_asset_mapping_prompt)
            .setMultiLine(minLines = 10, maxLines = 20)
            .setMessage(PrefManager.aiAssetMappingPrompt)
            .setEditorPositiveButton(R.string.btn_confirm) { prompt ->
                PrefManager.aiAssetMappingPrompt = prompt
                findPreference<Preference>("aiAssetMappingPrompt")?.updateAiAssetMappingPromptSummary()
            }
            .setNegativeButton(R.string.btn_cancel) { _, _ -> }
            .show()
    }

    /**
     * 显示AI分类识别提示词编辑对话框
     */
    private fun showAiCategoryRecognitionPromptDialog() {
        BaseSheetDialog.create<EditorDialogBuilder>(requireContext())
            .setTitleInt(R.string.setting_ai_category_recognition_prompt)
            .setMultiLine(minLines = 10, maxLines = 20)
            .setMessage(PrefManager.aiCategoryRecognitionPrompt)
            .setEditorPositiveButton(R.string.btn_confirm) { prompt ->
                PrefManager.aiCategoryRecognitionPrompt = prompt
                findPreference<Preference>("aiCategoryRecognitionPrompt")?.updateAiCategoryRecognitionPromptSummary()
            }
            .setNegativeButton(R.string.btn_cancel) { _, _ -> }
            .show()
    }

    /**
     * 更新AI总结提示词摘要
     */
    private fun Preference.updateAiSummaryPromptSummary() {
        summary = if (PrefManager.aiSummaryPrompt.isNotEmpty()) {
            PrefManager.aiSummaryPrompt.take(100)
                .let { if (it.length < PrefManager.aiSummaryPrompt.length) "$it..." else it }
        } else {
            getString(R.string.setting_ai_summary_prompt_summary)
        }
    }

    /**
     * 更新AI账单识别提示词摘要
     */
    private fun Preference.updateAiBillRecognitionPromptSummary() {
        summary = if (PrefManager.aiBillRecognitionPrompt.isNotEmpty()) {
            PrefManager.aiBillRecognitionPrompt.take(100)
                .let { if (it.length < PrefManager.aiBillRecognitionPrompt.length) "$it..." else it }
        } else {
            getString(R.string.setting_ai_bill_recognition_prompt_summary)
        }
    }

    /**
     * 更新AI资产映射提示词摘要
     */
    private fun Preference.updateAiAssetMappingPromptSummary() {
        summary = if (PrefManager.aiAssetMappingPrompt.isNotEmpty()) {
            PrefManager.aiAssetMappingPrompt.take(100)
                .let { if (it.length < PrefManager.aiAssetMappingPrompt.length) "$it..." else it }
        } else {
            getString(R.string.setting_ai_asset_mapping_prompt_summary)
        }
    }

    /**
     * 更新AI分类识别提示词摘要
     */
    private fun Preference.updateAiCategoryRecognitionPromptSummary() {
        summary = if (PrefManager.aiCategoryRecognitionPrompt.isNotEmpty()) {
            PrefManager.aiCategoryRecognitionPrompt.take(100)
                .let { if (it.length < PrefManager.aiCategoryRecognitionPrompt.length) "$it..." else it }
        } else {
            getString(R.string.setting_ai_category_recognition_prompt_summary)
        }
    }

    /**
     * AI助理设置数据存储类
     */
    class AIAssistantDataStore : PreferenceDataStore() {
        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            return when (key) {
                // AI功能
                "aiBillRecognition" -> PrefManager.aiBillRecognition
                "aiCategoryRecognition" -> PrefManager.aiCategoryRecognition
                "aiAssetMapping" -> PrefManager.aiAssetMapping
                "aiMonthlySummary" -> PrefManager.aiMonthlySummary
                else -> defValue
            }
        }

        override fun putBoolean(key: String?, value: Boolean) {
            when (key) {
                // AI功能
                "aiBillRecognition" -> PrefManager.aiBillRecognition = value
                "aiCategoryRecognition" -> PrefManager.aiCategoryRecognition = value
                "aiAssetMapping" -> PrefManager.aiAssetMapping = value
                "aiMonthlySummary" -> PrefManager.aiMonthlySummary = value
            }
        }
    }
}

