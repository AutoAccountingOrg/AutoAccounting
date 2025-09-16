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

import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import net.ankio.auto.R
import net.ankio.auto.ui.api.BasePreferenceFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.AppDialog
import net.ankio.auto.ui.dialog.BookSelectorDialog
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.BillTool
import net.ankio.auto.ui.utils.ToastUtils
import kotlinx.coroutines.launch
import org.ezbook.server.constant.BillType
import rikka.material.preference.MaterialSwitchPreference
import androidx.navigation.fragment.findNavController

/**
 * 账单设置页面
 * 包含自动记账、去重、通知、去重等核心功能设置
 */
class BillPreferenceFragment : BasePreferenceFragment() {

    override fun getTitleRes(): Int = R.string.setting_title_bill

    override fun getPreferencesRes(): Int = R.xml.settings_bill

    override fun createDataStore(): PreferenceDataStore = BillPreferenceDataStore()

    /**
     * 设置自定义弹窗处理
     */
    override fun setupPreferences() {
        super.setupPreferences()

        // 默认账本设置 - 使用自定义弹窗
        findPreference<Preference>("defaultBook")?.setOnPreferenceClickListener {
            showDefaultBookDialog()
            true
        }

        // 记账应用设置 - 使用自定义弹窗
        findPreference<Preference>("bookApp")?.setOnPreferenceClickListener {
            showBookAppDialog()
            true
        }

        // 更新显示的摘要文本
        updatePreferenceSummaries()

        // 设置依赖关系 - 自动资产映射依赖于资产管理功能
        updateAssetMappingDependency()

        // 备注格式设置入口
        findPreference<Preference>("remarkFormat")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.remarkFormatFragment)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        updatePreferenceSummaries()
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
                // 应用选择后自动更新PrefManager.bookApp，这里只需要刷新显示
                updatePreferenceSummaries()
            }
            .show()
    }

    /**
     * 更新设置项的摘要显示 - 显示当前选择的值
     */
    private fun updatePreferenceSummaries() {
        // 默认账本 - 显示当前选择的账本名称
        findPreference<Preference>("defaultBook")?.apply {
            summary = if (PrefManager.defaultBook.isNotEmpty()) {
                PrefManager.defaultBook
            } else {
                getString(R.string.setting_default_book_summary)
            }
        }

        // 记账应用 - 显示当前选择的应用包名
        findPreference<Preference>("bookApp")?.apply {
            summary = if (PrefManager.bookApp.isNotEmpty()) {
                PrefManager.bookApp
            } else {
                getString(R.string.setting_book_app_summary)
            }
        }

        // 备注格式 - 显示当前模板
        findPreference<Preference>("remarkFormat")?.apply {
            summary = PrefManager.noteFormat
        }
    }

    /**
     * 更新自动资产映射的依赖关系
     * 当资产管理功能关闭时，禁用自动资产映射
     */
    private fun updateAssetMappingDependency() {
        findPreference<MaterialSwitchPreference>("autoAssetMapping")?.apply {
            val isAssetManageEnabled = PrefManager.featureAssetManage
            isEnabled = isAssetManageEnabled

            // 如果资产管理功能关闭，自动关闭资产映射功能
            if (!isAssetManageEnabled && PrefManager.autoAssetMapping) {
                PrefManager.autoAssetMapping = false
                isChecked = false
            }

            // 更新摘要说明
            summary = if (isAssetManageEnabled) {
                getString(R.string.setting_auto_asset_mapping_summary)
            } else {
                getString(R.string.setting_auto_asset_mapping_disabled)
            }
        }
    }

    /**
     * 账单设置专用的数据存储类
     */
    class BillPreferenceDataStore : PreferenceDataStore() {
        // 字符串设置项现在通过自定义弹窗处理，不再需要DataStore处理
        override fun getString(key: String?, defValue: String?): String {
            return defValue ?: ""
        }

        override fun putString(key: String?, value: String?) {
            // 字符串设置项现在通过自定义弹窗直接操作PrefManager
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            return when (key) {
                // 自动记账设置 - 使用明确的语义属性
                "autoRecordBill" -> PrefManager.autoRecordBill
                "autoGroup" -> PrefManager.autoGroup
                "autoCreateCategory" -> PrefManager.rememberCategory
                "autoAssetMapping" -> PrefManager.autoAssetMapping
                "showDuplicatedPopup" -> PrefManager.showDuplicatedPopup
                // 删除确认
                "confirmDeleteBill" -> PrefManager.confirmDeleteBill
                // 手动同步：开启后保存时不触发同步
                "manualSync" -> PrefManager.manualSync
                // 显示设置
                "showRuleName" -> PrefManager.showRuleName
                // 通知设置
                "showSuccessPopup" -> PrefManager.showSuccessPopup
                "loadSuccess" -> PrefManager.loadSuccess
                "landscapeDnd" -> PrefManager.landscapeDnd
                // 功能开关（用于依赖检查）
                "featureAssetManage" -> PrefManager.featureAssetManage
                else -> defValue
            }
        }

        override fun putBoolean(key: String?, value: Boolean) {
            when (key) {
                // 自动记账设置 - 使用明确的语义属性
                "autoRecordBill" -> PrefManager.autoRecordBill = value
                "autoGroup" -> PrefManager.autoGroup = value
                "autoCreateCategory" -> PrefManager.rememberCategory = value
                "autoAssetMapping" -> PrefManager.autoAssetMapping = value
                "showDuplicatedPopup" -> PrefManager.showDuplicatedPopup = value
                // 删除确认
                "confirmDeleteBill" -> PrefManager.confirmDeleteBill = value
                // 手动同步：开启后保存时不触发同步
                "manualSync" -> PrefManager.manualSync = value
                // 显示设置
                "showRuleName" -> PrefManager.showRuleName = value
                // 通知设置
                "showSuccessPopup" -> PrefManager.showSuccessPopup = value
                "loadSuccess" -> PrefManager.loadSuccess = value
                "landscapeDnd" -> PrefManager.landscapeDnd = value
                // 功能开关（用于依赖检查）
                "featureAssetManage" -> PrefManager.featureAssetManage = value
            }
        }
    }
}
