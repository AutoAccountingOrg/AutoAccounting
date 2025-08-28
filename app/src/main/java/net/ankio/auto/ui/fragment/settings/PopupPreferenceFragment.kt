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
import net.ankio.auto.constant.FloatEvent
import net.ankio.auto.ui.api.BasePreferenceFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.EditorDialogBuilder
import net.ankio.auto.utils.PrefManager

/**
 * 弹窗设置页面
 * 包含悬浮窗显示、超时、点击事件等设置
 */
class PopupPreferenceFragment : BasePreferenceFragment() {

    override fun getTitleRes(): Int = R.string.setting_title_popup

    override fun getPreferencesRes(): Int = R.xml.settings_popup

    override fun createDataStore(): PreferenceDataStore = PopupPreferenceDataStore()

    /**
     * 设置自定义弹窗处理
     */
    override fun setupPreferences() {
        super.setupPreferences()

        // 悬浮角标显示设置 - 使用自定义数字输入弹窗（0=不显示悬浮角标）
        findPreference<Preference>("floatTimeoutOff")?.setOnPreferenceClickListener {
            showTimeoutEditDialog()
            true
        }

        // 更新显示的摘要文本
        updatePreferenceSummaries()
    }

    /**
     * 显示悬浮角标设置编辑弹窗
     * 0 = 不显示悬浮角标，大于0 = 悬浮角标显示时间（秒）
     */
    private fun showTimeoutEditDialog() {
        BaseSheetDialog.create<EditorDialogBuilder>(requireContext())
            .setInputType(InputType.TYPE_CLASS_NUMBER)
            .setTitleInt(R.string.setting_float_badge_timeout)
            .setMessage(PrefManager.floatTimeoutOff.toString())
            .setEditorPositiveButton(R.string.sure_msg) { result ->
                val timeout = result.toIntOrNull()
                if (timeout != null && timeout >= 0) {
                    PrefManager.floatTimeoutOff = timeout
                    updatePreferenceSummaries()
                }
            }
            .setNegativeButton(R.string.cancel_msg, null)
            .show()
    }

    /**
     * 更新设置项的摘要显示 - 显示当前选择的值
     */
    private fun updatePreferenceSummaries() {
        // 悬浮角标显示设置 - 显示当前设置的秒数或"不显示"状态
        findPreference<Preference>("floatTimeoutOff")?.apply {
            val timeoutValue = PrefManager.floatTimeoutOff
            summary = if (timeoutValue > 0) {
                getString(R.string.setting_timeout_seconds, timeoutValue)
            } else {
                getString(R.string.setting_float_badge_disabled)
            }
        }

        // 悬浮窗超时动作 - 显示当前选择的动作
        findPreference<Preference>("floatTimeoutAction")?.apply {
            val actionValue = PrefManager.floatTimeoutAction
            summary = getActionDisplayName(actionValue)
        }

        // 悬浮窗点击动作 - 显示当前选择的动作
        findPreference<Preference>("floatClick")?.apply {
            val clickValue = PrefManager.floatClick
            summary = getActionDisplayName(clickValue)
        }

        // 悬浮窗长按动作 - 显示当前选择的动作
        findPreference<Preference>("floatLongClick")?.apply {
            val longClickValue = PrefManager.floatLongClick
            summary = getActionDisplayName(longClickValue)
        }
    }

    /**
     * 根据动作值获取显示名称
     */
    private fun getActionDisplayName(actionValue: String): String {
        return when (actionValue) {
            FloatEvent.POP_EDIT_WINDOW.name -> getString(R.string.float_action_edit)
            FloatEvent.NO_ACCOUNT.name -> getString(R.string.float_action_dismiss)
            FloatEvent.AUTO_ACCOUNT.name -> getString(R.string.float_action_auto_record)
            else -> actionValue.ifEmpty { getString(R.string.setting_not_set) }
        }
    }

    /**
     * 弹窗设置专用的数据存储类
     */
    inner class PopupPreferenceDataStore : PreferenceDataStore() {

        override fun getString(key: String?, defValue: String?): String {
            return when (key) {
                // 悬浮窗超时动作设置
                "floatTimeoutAction" -> PrefManager.floatTimeoutAction
                // 悬浮窗点击动作设置
                "floatClick" -> PrefManager.floatClick
                // 悬浮窗长按动作设置
                "floatLongClick" -> PrefManager.floatLongClick
                else -> defValue ?: ""
            }
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            return when (key) {
                // 圆角弹窗风格设置
                "roundStyle" -> PrefManager.uiRoundStyle
                else -> defValue
            }
        }

        override fun putString(key: String?, value: String?) {
            val safeValue = value ?: ""
            when (key) {
                // 悬浮窗超时动作设置
                "floatTimeoutAction" -> PrefManager.floatTimeoutAction = safeValue
                // 悬浮窗点击动作设置
                "floatClick" -> PrefManager.floatClick = safeValue
                // 悬浮窗长按动作设置
                "floatLongClick" -> PrefManager.floatLongClick = safeValue
            }
            // 关键修复：每次preference值改变后立即更新summary显示
            updatePreferenceSummaries()
        }

        override fun putBoolean(key: String?, value: Boolean) {
            when (key) {
                // 圆角弹窗风格设置
                "roundStyle" -> PrefManager.uiRoundStyle = value
            }
        }

    }
}
