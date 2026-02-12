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
import org.ezbook.server.constant.DefaultData
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

        // AI功能总开关：仅控制可用性，不影响AI配置入口
        findPreference<MaterialSwitchPreference>("featureAiAvailable")?.setOnPreferenceChangeListener { _, newValue ->
            val isEnabled = newValue as? Boolean ?: return@setOnPreferenceChangeListener true
            updateAiFeatureDependencies(isEnabled)
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

        // 初始同步AI功能可用性
        updateAiFeatureDependencies()
    }

    override fun onResume() {
        super.onResume()
        findPreference<Preference>("aiSummaryPrompt")?.updateAiSummaryPromptSummary()
        findPreference<Preference>("aiBillRecognitionPrompt")?.updateAiBillRecognitionPromptSummary()
        findPreference<Preference>("aiAssetMappingPrompt")?.updateAiAssetMappingPromptSummary()
        findPreference<Preference>("aiCategoryRecognitionPrompt")?.updateAiCategoryRecognitionPromptSummary()
        updateAiFeatureDependencies()
    }

    /**
     * 更新AI功能与提示词入口的可用性
     */
    private fun updateAiFeatureDependencies(isAvailable: Boolean = PrefManager.featureAiAvailable) {
        // AI能力不可用时，禁用AI功能与提示词入口，避免误操作
        findPreference<MaterialSwitchPreference>("aiBillRecognition")?.isEnabled = isAvailable
        findPreference<MaterialSwitchPreference>("aiVisionRecognition")?.isEnabled = isAvailable
        findPreference<MaterialSwitchPreference>("aiCategoryRecognition")?.isEnabled = isAvailable
        findPreference<MaterialSwitchPreference>("aiAssetMapping")?.isEnabled = isAvailable
        findPreference<MaterialSwitchPreference>("aiMonthlySummary")?.isEnabled = isAvailable
        findPreference<Preference>("aiSummaryPrompt")?.isEnabled = isAvailable
        findPreference<Preference>("aiBillRecognitionPrompt")?.isEnabled = isAvailable
        findPreference<Preference>("aiAssetMappingPrompt")?.isEnabled = isAvailable
        findPreference<Preference>("aiCategoryRecognitionPrompt")?.isEnabled = isAvailable
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
                // 如果提示词为空，则使用默认值
                PrefManager.aiSummaryPrompt = prompt.ifBlank { DefaultData.AI_SUMMARY_PROMPT }
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
                // 如果提示词为空，则使用默认值
                PrefManager.aiBillRecognitionPrompt = prompt.ifBlank { DefaultData.AI_BILL_RECOGNITION_PROMPT }
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
                // 如果提示词为空，则使用默认值
                PrefManager.aiAssetMappingPrompt = prompt.ifBlank { DefaultData.AI_ASSET_MAPPING_PROMPT }
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
                // 如果提示词为空，则使用默认值
                PrefManager.aiCategoryRecognitionPrompt = prompt.ifBlank { DefaultData.AI_CATEGORY_RECOGNITION_PROMPT }
                findPreference<Preference>("aiCategoryRecognitionPrompt")?.updateAiCategoryRecognitionPromptSummary()
            }
            .setNegativeButton(R.string.btn_cancel) { _, _ -> }
            .show()
    }

    /**
     * 更新AI总结提示词摘要
     */
    private fun Preference.updateAiSummaryPromptSummary() {
        val prompt = PrefManager.aiSummaryPrompt.ifBlank { DefaultData.AI_SUMMARY_PROMPT }
        summary = prompt.take(100).let { if (it.length < prompt.length) "$it..." else it }
    }

    /**
     * 更新AI账单识别提示词摘要
     */
    private fun Preference.updateAiBillRecognitionPromptSummary() {
        val prompt = PrefManager.aiBillRecognitionPrompt.ifBlank { DefaultData.AI_BILL_RECOGNITION_PROMPT }
        summary = prompt.take(100).let { if (it.length < prompt.length) "$it..." else it }
    }

    /**
     * 更新AI资产映射提示词摘要
     */
    private fun Preference.updateAiAssetMappingPromptSummary() {
        val prompt = PrefManager.aiAssetMappingPrompt.ifBlank { DefaultData.AI_ASSET_MAPPING_PROMPT }
        summary = prompt.take(100).let { if (it.length < prompt.length) "$it..." else it }
    }

    /**
     * 更新AI分类识别提示词摘要
     */
    private fun Preference.updateAiCategoryRecognitionPromptSummary() {
        val prompt = PrefManager.aiCategoryRecognitionPrompt.ifBlank { DefaultData.AI_CATEGORY_RECOGNITION_PROMPT }
        summary = prompt.take(100).let { if (it.length < prompt.length) "$it..." else it }
    }

    /**
     * AI助理设置数据存储类
     */
    class AIAssistantDataStore : PreferenceDataStore() {
        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            return when (key) {
                // AI功能
                "featureAiAvailable" -> PrefManager.featureAiAvailable
                "aiBillRecognition" -> PrefManager.aiBillRecognition
                "aiVisionRecognition" -> PrefManager.aiVisionRecognition
                "aiCategoryRecognition" -> PrefManager.aiCategoryRecognition
                "aiAssetMapping" -> PrefManager.aiAssetMapping
                "aiMonthlySummary" -> PrefManager.aiMonthlySummary
                else -> defValue
            }
        }

        override fun putBoolean(key: String?, value: Boolean) {
            when (key) {
                // AI功能
                "featureAiAvailable" -> PrefManager.featureAiAvailable = value
                "aiBillRecognition" -> PrefManager.aiBillRecognition = value
                "aiVisionRecognition" -> PrefManager.aiVisionRecognition = value
                "aiCategoryRecognition" -> PrefManager.aiCategoryRecognition = value
                "aiAssetMapping" -> PrefManager.aiAssetMapping = value
                "aiMonthlySummary" -> PrefManager.aiMonthlySummary = value
            }
        }
    }
}

