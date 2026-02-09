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

import android.text.InputType
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import net.ankio.auto.R
import net.ankio.auto.ui.api.BasePreferenceFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.AppDialog
import net.ankio.auto.ui.dialog.BookSelectorDialog
import net.ankio.auto.ui.dialog.CurrencySelectorDialog
import net.ankio.auto.ui.dialog.EditorDialogBuilder
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.constant.Currency
import rikka.material.preference.MaterialSwitchPreference

/**
 * 记账设置页面
 * 包含：记账应用、记录方式、账单识别、账单管理、分类管理、资产管理、账本配置
 */
class RecordingPreferenceFragment : BasePreferenceFragment() {

    override fun getTitleRes(): Int = R.string.setting_title_recording

    override fun getPreferencesRes(): Int = R.xml.settings_recording

    override fun createDataStore(): PreferenceDataStore = RecordingDataStore()

    /**
     * 设置自定义弹窗处理
     */
    override fun setupPreferences() {
        super.setupPreferences()

        // 记账应用设置
        findPreference<Preference>("bookApp")?.setOnPreferenceClickListener {
            showBookAppDialog()
            true
        }

        // 默认账本设置
        findPreference<Preference>("defaultBook")?.setOnPreferenceClickListener {
            showDefaultBookDialog()
            true
        }

        // 备注格式设置入口
        findPreference<Preference>("remarkFormat")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.remarkFormatFragment)
            true
        }

        // 延迟同步阈值设置
        findPreference<Preference>("delayedSyncThreshold")?.setOnPreferenceClickListener {
            showDelayedSyncThresholdDialog()
            true
        }

        // 自动去重时间阈值设置
        findPreference<Preference>("autoGroupTimeThreshold")?.setOnPreferenceClickListener {
            showAutoGroupTimeThresholdDialog()
            true
        }

        // 转账合并时间阈值设置
        findPreference<Preference>("autoTransferTimeThreshold")?.setOnPreferenceClickListener {
            showAutoTransferTimeThresholdDialog()
            true
        }

        // 本位币设置
        findPreference<Preference>("baseCurrency")?.setOnPreferenceClickListener {
            showBaseCurrencyDialog()
            true
        }

        // 常用币种设置
        findPreference<Preference>("selectedCurrencies")?.setOnPreferenceClickListener {
            showSelectedCurrenciesDialog()
            true
        }

        // 监听手动同步开关变化，实时更新延迟同步设置的可用性
        findPreference<MaterialSwitchPreference>("manualSync")?.setOnPreferenceChangeListener { _, newValue ->
            // 使用回调参数中的新值，并在DataStore更新后延迟更新UI
            val isManualSyncEnabled = newValue as Boolean
            updateDelayedSyncDependency(isManualSyncEnabled)
            true
        }

        // 更新显示的摘要文本
        updatePreferenceSummaries()

        // 设置依赖关系
        updateAssetMappingDependency()
        updateDelayedSyncDependency()
        updateBillRecognitionSummaries()
    }

    override fun onResume() {
        super.onResume()
        updatePreferenceSummaries()
        updateDelayedSyncDependency()
        updateBillRecognitionSummaries()
    }

    /**
     * 显示默认账本选择弹窗
     */
    private fun showDefaultBookDialog() {
        BaseSheetDialog.create<BookSelectorDialog>(requireContext())
            .setShowSelect(false)
            .setCallback { selectedBook, _ ->
                PrefManager.defaultBook = selectedBook.name
                updatePreferenceSummaries()
            }
            .show()
    }

    /**
     * 显示记账应用选择弹窗
     */
    private fun showBookAppDialog() {
        BaseSheetDialog.create<AppDialog>(requireContext())
            .setOnClose {
                updatePreferenceSummaries()
            }
            .show()
    }

    /**
     * 更新设置项的摘要显示
     */
    private fun updatePreferenceSummaries() {
        // 默认账本
        findPreference<Preference>("defaultBook")?.apply {
            summary = PrefManager.defaultBook.ifEmpty {
                getString(R.string.setting_default_book_summary)
            }
        }

        // 记账应用
        findPreference<Preference>("bookApp")?.apply {
            summary = PrefManager.bookApp.ifEmpty {
                getString(R.string.setting_book_app_summary)
            }
        }

        // 备注格式
        findPreference<Preference>("remarkFormat")?.apply {
            summary = PrefManager.noteFormat
        }

        // 本位币
        updateBaseCurrencySummary()

        // 常用币种
        updateSelectedCurrenciesSummary()
    }

    /**
     * 更新本位币摘要
     */
    private fun updateBaseCurrencySummary() {
        findPreference<Preference>("baseCurrency")?.apply {
            val code = PrefManager.baseCurrency
            val currency = runCatching { Currency.valueOf(code) }.getOrDefault(Currency.CNY)
            summary =
                getString(R.string.setting_base_currency_summary, currency.name(requireContext()))
        }
    }

    /**
     * 显示本位币选择弹窗 —— 复用 CurrencySelectorDialog 单选模式
     */
    private fun showBaseCurrencyDialog() {
        BaseSheetDialog.create<CurrencySelectorDialog>(requireContext())
            .setSingleSelectMode(true)
            .setBaseCurrency(PrefManager.baseCurrency)
            .setSelectedCodes(setOf(PrefManager.baseCurrency))
            .setCallback { models ->
                val code = models.firstOrNull()?.code ?: return@setCallback
                PrefManager.baseCurrency = code
                updateBaseCurrencySummary()
            }
            .show()
    }

    /**
     * 更新常用币种摘要
     */
    private fun updateSelectedCurrenciesSummary() {
        findPreference<Preference>("selectedCurrencies")?.apply {
            val count = PrefManager.getSelectedCurrencySet().size
            summary = getString(R.string.setting_selected_currencies_summary, count)
        }
    }

    /**
     * 显示常用币种多选弹窗 —— 使用 CurrencySelectorDialog
     */
    private fun showSelectedCurrenciesDialog() {
        BaseSheetDialog.create<CurrencySelectorDialog>(requireContext())
            .setBaseCurrency(PrefManager.baseCurrency)
            .setSelectedCodes(PrefManager.getSelectedCurrencySet())
            .setCallback { models ->
                // 确保本位币始终在常用列表中
                val codes = models.map { it.code }.toMutableSet().apply {
                    add(PrefManager.baseCurrency)
                }
                PrefManager.selectedCurrencies = codes.joinToString(",")
                updateSelectedCurrenciesSummary()
            }
            .show()
    }

    /**
     * 更新记住资产映射的依赖关系
     * 当资产管理功能关闭时，禁用记住资产映射
     */
    private fun updateAssetMappingDependency() {
        findPreference<MaterialSwitchPreference>("autoAssetMapping")?.apply {
            val isAssetManageEnabled = PrefManager.featureAssetManage
            isEnabled = isAssetManageEnabled

            if (!isAssetManageEnabled && PrefManager.autoAssetMapping) {
                PrefManager.autoAssetMapping = false
                isChecked = false
            }

            summary = if (isAssetManageEnabled) {
                getString(R.string.setting_auto_asset_mapping_summary)
            } else {
                getString(R.string.setting_auto_asset_mapping_disabled)
            }
        }
    }

    /**
     * 更新延迟同步设置的依赖关系和摘要
     * 手动同步开启时，禁用延迟同步设置
     * @param isManualSyncEnabled 手动同步是否开启（可选，不提供则从PrefManager读取）
     */
    private fun updateDelayedSyncDependency(isManualSyncEnabled: Boolean? = null) {
        findPreference<Preference>("delayedSyncThreshold")?.apply {
            // 手动同步开启时，禁用延迟同步设置
            val manualSync = isManualSyncEnabled ?: PrefManager.manualSync
            isEnabled = !manualSync

            val threshold = PrefManager.delayedSyncThreshold
            summary = if (threshold == 0) {
                getString(R.string.setting_delayed_sync_threshold_realtime)
            } else {
                getString(R.string.setting_delayed_sync_threshold_summary, threshold)
            }
        }
    }

    /**
     * 显示延迟同步阈值设置对话框
     */
    private fun showDelayedSyncThresholdDialog() {
        BaseSheetDialog.create<EditorDialogBuilder>(requireContext())
            .setInputType(InputType.TYPE_CLASS_NUMBER)
            .setTitleInt(R.string.setting_delayed_sync_threshold)
            .setMessage(PrefManager.delayedSyncThreshold.toString())
            .setEditorPositiveButton(R.string.sure_msg) { result ->
                val threshold = result.toIntOrNull()
                if (threshold != null && threshold >= 0) {
                    PrefManager.delayedSyncThreshold = threshold
                    updateDelayedSyncDependency()
                }
            }
            .setNegativeButton(R.string.cancel_msg, null)
            .show()
    }

    /**
     * 显示自动去重时间阈值设置对话框
     */
    private fun showAutoGroupTimeThresholdDialog() {
        BaseSheetDialog.create<EditorDialogBuilder>(requireContext())
            .setInputType(InputType.TYPE_CLASS_NUMBER)
            .setTitleInt(R.string.setting_auto_group_time_threshold)
            .setMessage(PrefManager.autoGroupTimeThreshold.toString())
            .setEditorPositiveButton(R.string.sure_msg) { result ->
                val threshold = result.toIntOrNull()
                if (threshold != null && threshold > 0) {
                    PrefManager.autoGroupTimeThreshold = threshold
                    updateBillRecognitionSummaries()
                }
            }
            .setNegativeButton(R.string.cancel_msg, null)
            .show()
    }

    /**
     * 显示转账合并时间阈值设置对话框
     */
    private fun showAutoTransferTimeThresholdDialog() {
        BaseSheetDialog.create<EditorDialogBuilder>(requireContext())
            .setInputType(InputType.TYPE_CLASS_NUMBER)
            .setTitleInt(R.string.setting_auto_transfer_time_threshold)
            .setMessage(PrefManager.autoTransferTimeThreshold.toString())
            .setEditorPositiveButton(R.string.sure_msg) { result ->
                val threshold = result.toIntOrNull()
                if (threshold != null && threshold > 0) {
                    PrefManager.autoTransferTimeThreshold = threshold
                    updateBillRecognitionSummaries()
                }
            }
            .setNegativeButton(R.string.cancel_msg, null)
            .show()
    }

    /**
     * 更新账单识别相关设置的摘要显示
     */
    private fun updateBillRecognitionSummaries() {
        // 自动去重时间阈值
        findPreference<Preference>("autoGroupTimeThreshold")?.apply {
            val threshold = PrefManager.autoGroupTimeThreshold
            summary = getString(R.string.setting_auto_group_time_threshold_summary, threshold)
        }

        // 转账合并时间阈值
        findPreference<Preference>("autoTransferTimeThreshold")?.apply {
            val threshold = PrefManager.autoTransferTimeThreshold
            summary = getString(R.string.setting_auto_transfer_time_threshold_summary, threshold)
        }
    }

    /**
     * 记账设置数据存储类
     */
    class RecordingDataStore : PreferenceDataStore() {
        override fun getInt(key: String?, defValue: Int): Int {
            return when (key) {
                "delayedSyncThreshold" -> PrefManager.delayedSyncThreshold
                "autoGroupTimeThreshold" -> PrefManager.autoGroupTimeThreshold
                "autoTransferTimeThreshold" -> PrefManager.autoTransferTimeThreshold
                else -> defValue
            }
        }

        override fun putInt(key: String?, value: Int) {
            when (key) {
                "delayedSyncThreshold" -> PrefManager.delayedSyncThreshold = value
                "autoGroupTimeThreshold" -> PrefManager.autoGroupTimeThreshold = value
                "autoTransferTimeThreshold" -> PrefManager.autoTransferTimeThreshold = value
            }
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            return when (key) {
                // 记账应用
                "manualSync" -> PrefManager.manualSync
                // 记录方式
                "autoRecordBill" -> PrefManager.autoRecordBill
                "landscapeDnd" -> PrefManager.landscapeDnd
                // 账单识别
                "autoGroup" -> PrefManager.autoGroup
                "autoTransferRecognition" -> PrefManager.autoTransferRecognition
                "aiBillRecognition" -> PrefManager.aiBillRecognition
                "ruleMatchIncludeDisabled" -> PrefManager.ruleMatchIncludeDisabled
                // 账单管理
                "showRuleName" -> PrefManager.showRuleName
                "featureFee" -> PrefManager.featureFee
                "featureTag" -> PrefManager.featureTag
                // 账单标记
                "billFlagNotCount" -> PrefManager.billFlagNotCount
                "billFlagNotBudget" -> PrefManager.billFlagNotBudget
                // 分类管理
                "autoCreateCategory" -> PrefManager.rememberCategory
                "aiCategoryRecognition" -> PrefManager.aiCategoryRecognition
                // 资产管理
                "featureAssetManage" -> PrefManager.featureAssetManage
                "featureMultiCurrency" -> PrefManager.featureMultiCurrency
                "featureReimbursement" -> PrefManager.featureReimbursement
                "featureDebt" -> PrefManager.featureDebt
                "autoAssetMapping" -> PrefManager.autoAssetMapping
                "aiAssetMapping" -> PrefManager.aiAssetMapping
                // 账本配置
                "featureMultiBook" -> PrefManager.featureMultiBook
                else -> defValue
            }
        }

        override fun putBoolean(key: String?, value: Boolean) {
            when (key) {
                // 记账应用
                "manualSync" -> {
                    PrefManager.manualSync = value
                    // 当手动同步开启时，重置延迟同步阈值为0（实时同步）
                    if (value && PrefManager.delayedSyncThreshold != 0) {
                        PrefManager.delayedSyncThreshold = 0
                    }
                }
                // 记录方式
                "autoRecordBill" -> PrefManager.autoRecordBill = value
                "landscapeDnd" -> PrefManager.landscapeDnd = value
                // 账单识别
                "autoGroup" -> PrefManager.autoGroup = value
                "autoTransferRecognition" -> PrefManager.autoTransferRecognition = value
                "aiBillRecognition" -> PrefManager.aiBillRecognition = value
                "ruleMatchIncludeDisabled" -> PrefManager.ruleMatchIncludeDisabled = value
                // 账单管理
                "showRuleName" -> PrefManager.showRuleName = value
                "featureFee" -> PrefManager.featureFee = value
                "featureTag" -> PrefManager.featureTag = value
                // 账单标记
                "billFlagNotCount" -> PrefManager.billFlagNotCount = value
                "billFlagNotBudget" -> PrefManager.billFlagNotBudget = value
                // 分类管理
                "autoCreateCategory" -> PrefManager.rememberCategory = value
                "aiCategoryRecognition" -> PrefManager.aiCategoryRecognition = value
                // 资产管理
                "featureAssetManage" -> PrefManager.featureAssetManage = value
                "featureMultiCurrency" -> PrefManager.featureMultiCurrency = value
                "featureReimbursement" -> PrefManager.featureReimbursement = value
                "featureDebt" -> PrefManager.featureDebt = value
                "autoAssetMapping" -> PrefManager.autoAssetMapping = value
                "aiAssetMapping" -> PrefManager.aiAssetMapping = value
                // 账本配置
                "featureMultiBook" -> PrefManager.featureMultiBook = value
            }
        }
    }
}

