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

import androidx.preference.PreferenceDataStore
import net.ankio.auto.R
import net.ankio.auto.ui.api.BasePreferenceFragment
import net.ankio.auto.utils.PrefManager

/**
 * 功能配置页面
 * 包含资产管理、多币种、报销、债务、多账本、手续费等功能开关
 */
class FeaturesPreferenceFragment : BasePreferenceFragment() {

    override fun getTitleRes(): Int = R.string.setting_title_features

    override fun getPreferencesRes(): Int = R.xml.settings_features

    override fun createDataStore(): PreferenceDataStore = FeaturesPreferenceDataStore()

    /**
     * 功能配置专用的数据存储类
     */
    inner class FeaturesPreferenceDataStore : PreferenceDataStore() {
        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            return when (key) {
                "featureAssetManage" -> PrefManager.featureAssetManage
                "featureMultiCurrency" -> PrefManager.featureMultiCurrency
                "featureReimbursement" -> PrefManager.featureReimbursement
                "featureDebt" -> PrefManager.featureDebt
                "featureMultiBook" -> PrefManager.featureMultiBook
                "featureFee" -> PrefManager.featureFee
                "featureTag" -> PrefManager.featureTag
                "featureLeading" -> PrefManager.featureLeading
                else -> defValue
            }
        }

        override fun putBoolean(key: String?, value: Boolean) {
            when (key) {
                "featureAssetManage" -> PrefManager.featureAssetManage = value
                "featureMultiCurrency" -> PrefManager.featureMultiCurrency = value
                "featureReimbursement" -> PrefManager.featureReimbursement = value
                "featureDebt" -> PrefManager.featureDebt = value
                "featureMultiBook" -> PrefManager.featureMultiBook = value
                "featureFee" -> PrefManager.featureFee = value
                "featureTag" -> PrefManager.featureTag = value
                "featureLeading" -> PrefManager.featureLeading = value
            }
        }
    }

    /**
     * 设置自定义偏好行为（可选重写）
     */
    override fun setupPreferences() {
        super.setupPreferences()
    }
}
