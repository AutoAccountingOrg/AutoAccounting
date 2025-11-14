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
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import net.ankio.auto.R
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.service.CoreService
import net.ankio.auto.ui.api.BasePreferenceFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.EditorDialogBuilder
import net.ankio.auto.utils.PrefManager
import rikka.material.preference.MaterialSwitchPreference

/**
 * 交互设置页面
 * 包含：提醒设置、OCR识别、弹窗风格、记账面板
 */
class InteractionPreferenceFragment : BasePreferenceFragment() {

    override fun getTitleRes(): Int = R.string.setting_title_interaction

    override fun getPreferencesRes(): Int = R.xml.settings_interaction

    override fun createDataStore(): PreferenceDataStore = InteractionDataStore()

    /**
     * 设置自定义弹窗处理
     */
    override fun setupPreferences() {
        super.setupPreferences()

        // 浮窗超时时间设置
        findPreference<Preference>("floatTimeoutOff")?.apply {
            setOnPreferenceClickListener {
                showFloatTimeoutDialog()
                true
            }
            updateSummary()
        }

        // 浮窗位置设置 - 添加变化监听器，同步更新摘要
        findPreference<rikka.preference.SimpleMenuPreference>("floatGravityPosition")?.apply {
            updateFloatGravitySummary(this)
            setOnPreferenceChangeListener { _, newValue ->
                updateFloatGravitySummary(this, newValue as? String)
                true
            }
        }

        // 提醒位置设置 - 添加变化监听器，同步更新摘要
        findPreference<rikka.preference.SimpleMenuPreference>("toastPosition")?.apply {
            updateToastPositionSummary(this)
            setOnPreferenceChangeListener { _, newValue ->
                updateToastPositionSummary(this, newValue as? String)
                true
            }
        }

        // OCR 翻转触发开关变化时重启服务，使配置即时生效
        findPreference<MaterialSwitchPreference>("ocrFlipTrigger")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val enabled = (newValue as? Boolean) ?: return@setOnPreferenceChangeListener true
                // 先写入以避免重启与持久化的竞态
                PrefManager.ocrFlipTrigger = enabled
                if (WorkMode.isOcr()) {
                    CoreService.restart(requireActivity())
                }
                true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        findPreference<Preference>("floatTimeoutOff")?.updateSummary()
        // 更新位置设置的摘要显示
        findPreference<rikka.preference.SimpleMenuPreference>("floatGravityPosition")?.let {
            updateFloatGravitySummary(it)
        }
        findPreference<rikka.preference.SimpleMenuPreference>("toastPosition")?.let {
            updateToastPositionSummary(it)
        }

        if (!WorkMode.isOcr()) {
            findPreference<MaterialSwitchPreference>("ocrFlipTrigger")?.isEnabled = false
        }
    }

    /**
     * 显示浮窗超时时间设置对话框
     */
    private fun showFloatTimeoutDialog() {
        BaseSheetDialog.create<EditorDialogBuilder>(requireContext())
            .setInputType(InputType.TYPE_CLASS_NUMBER)
            .setTitleInt(R.string.setting_float_badge_timeout)
            .setMessage(PrefManager.floatTimeoutOff.toString())
            .setEditorPositiveButton(R.string.sure_msg) { result ->
                val timeout = result.toIntOrNull()
                if (timeout != null && timeout >= 0) {
                    PrefManager.floatTimeoutOff = timeout
                    findPreference<Preference>("floatTimeoutOff")?.updateSummary()
                }
            }
            .setNegativeButton(R.string.cancel_msg, null)
            .show()
    }

    /**
     * 更新浮窗超时设置的摘要
     */
    private fun Preference.updateSummary() {
        val timeoutValue = PrefManager.floatTimeoutOff
        summary = if (timeoutValue > 0) {
            getString(R.string.setting_timeout_seconds, timeoutValue)
        } else {
            getString(R.string.setting_float_badge_disabled)
        }
    }

    /**
     * 更新浮窗位置设置的摘要
     * @param preference 偏好设置项
     * @param position 位置值，如果为null则从PrefManager读取
     */
    private fun updateFloatGravitySummary(
        preference: rikka.preference.SimpleMenuPreference,
        position: String? = null
    ) {
        val currentPosition = position ?: PrefManager.floatGravityPosition
        preference.summary = when (currentPosition) {
            "left" -> getString(R.string.float_position_left)
            "right" -> getString(R.string.float_position_right)
            "top" -> getString(R.string.float_position_top)
            else -> getString(R.string.float_position_right)
        }
    }

    /**
     * 更新提醒位置设置的摘要
     * @param preference 偏好设置项
     * @param position 位置值，如果为null则从PrefManager读取
     */
    private fun updateToastPositionSummary(
        preference: rikka.preference.SimpleMenuPreference,
        position: String? = null
    ) {
        val currentPosition = position ?: PrefManager.toastPosition
        preference.summary = when (currentPosition) {
            "top" -> getString(R.string.toast_position_top)
            "center" -> getString(R.string.toast_position_center)
            "bottom" -> getString(R.string.toast_position_bottom)
            else -> getString(R.string.toast_position_bottom)
        }
    }

    /**
     * 交互设置数据存储类
     */
    class InteractionDataStore : PreferenceDataStore() {
        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            return when (key) {
                // 提醒设置
                "showSuccessPopup" -> PrefManager.showSuccessPopup
                "loadSuccess" -> PrefManager.loadSuccess
                "showDuplicatedPopup" -> PrefManager.showDuplicatedPopup
                // OCR识别
                "ocrFlipTrigger" -> PrefManager.ocrFlipTrigger
                "ocrShowAnimation" -> PrefManager.ocrShowAnimation
                // 弹窗风格
                "roundStyle" -> PrefManager.uiRoundStyle
                "isExpenseRed" -> PrefManager.isExpenseRed
                "isIncomeUp" -> PrefManager.isIncomeUp
                // 记账小面板
                // floatGravityPosition 已改为 String 类型，在 getString 中处理
                // 记账面板
                "confirmDeleteBill" -> PrefManager.confirmDeleteBill
                else -> defValue
            }
        }

        override fun putBoolean(key: String?, value: Boolean) {
            when (key) {
                // 提醒设置
                "showSuccessPopup" -> PrefManager.showSuccessPopup = value
                "loadSuccess" -> PrefManager.loadSuccess = value
                "showDuplicatedPopup" -> PrefManager.showDuplicatedPopup = value
                // OCR识别
                "ocrFlipTrigger" -> PrefManager.ocrFlipTrigger = value
                "ocrShowAnimation" -> PrefManager.ocrShowAnimation = value
                // 弹窗风格
                "roundStyle" -> PrefManager.uiRoundStyle = value
                "isExpenseRed" -> PrefManager.isExpenseRed = value
                "isIncomeUp" -> PrefManager.isIncomeUp = value
                // 记账小面板
                // floatGravityPosition 已改为 String 类型，在 putString 中处理
                // 记账面板
                "confirmDeleteBill" -> PrefManager.confirmDeleteBill = value
            }
        }

        override fun getString(key: String?, defValue: String?): String? {
            return when (key) {
                "floatTimeoutAction" -> PrefManager.floatTimeoutAction
                "floatClick" -> PrefManager.floatClick
                "floatLongClick" -> PrefManager.floatLongClick
                "floatGravityPosition" -> PrefManager.floatGravityPosition
                "toastPosition" -> PrefManager.toastPosition
                else -> defValue
            }
        }

        override fun putString(key: String?, value: String?) {
            when (key) {
                "floatTimeoutAction" -> PrefManager.floatTimeoutAction = value ?: "dismiss"
                "floatClick" -> PrefManager.floatClick = value ?: "edit"
                "floatLongClick" -> PrefManager.floatLongClick = value ?: "dismiss"
                "floatGravityPosition" -> PrefManager.floatGravityPosition = value ?: "right"
                "toastPosition" -> PrefManager.toastPosition = value ?: "bottom"
            }
        }
    }
}

