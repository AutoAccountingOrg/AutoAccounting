/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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


package net.ankio.auto.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentSettingBinding
import net.ankio.auto.http.license.ActivateAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.dialog.EditorDialogBuilder
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.Throttle

class SettingFragment : BaseFragment<FragmentSettingBinding>() {

    /**
     * 节流器，防止频繁调用激活信息接口
     * 设置300秒的冷却时间
     */
    private val throttleActivateInfo = Throttle.asFunction(300000) {
        lifecycle.coroutineScope.launch {
            try {
                loadActivateInfo()
            } catch (e: Exception) {
                Logger.e("获取激活信息失败: ${e.message}", e)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置激活信息卡片点击事件
        binding.proCard.setOnClickListener {
            if (PrefManager.token.isEmpty()) {
                showActivationDialog()
            }
        }

        binding.settingBill.setOnClickListener {
            /* navigate(R.id.action_settingFragment_to_settingDetailFragment, Bundle().apply {
                 putString("title", getString(R.string.setting_title_bill))
                 putInt("id", R.id.setting_bill)
             })*/
        }

        binding.settingPopup.setOnClickListener {
            /*  navigate(R.id.action_settingFragment_to_settingDetailFragment, Bundle().apply {
                 putString("title", getString(R.string.setting_title_popup))
                 putInt("id", R.id.setting_popup)
             })*/
        }

        binding.settingFeatures.setOnClickListener {
            /* navigate(R.id.action_settingFragment_to_settingDetailFragment, Bundle().apply {
                 putString("title", getString(R.string.setting_title_features))
                 putInt("id", R.id.setting_features)
             })*/
        }

        binding.settingAppearance.setOnClickListener {
            /*  navigate(R.id.action_settingFragment_to_settingDetailFragment, Bundle().apply {
                 putString("title", getString(R.string.setting_title_appearance))
                 putInt("id", R.id.setting_appearance)
             })*/
        }

        binding.settingExperimental.setOnClickListener {
            /* navigate(R.id.action_settingFragment_to_settingDetailFragment, Bundle().apply {
                 putString("title", getString(R.string.setting_title_experimental))
                 putInt("id", R.id.setting_experimental)
             })*/
        }

        binding.settingBackup.setOnClickListener {
            /* navigate(R.id.action_settingFragment_to_settingDetailFragment, Bundle().apply {
                 putString("title", getString(R.string.setting_title_backup))
                 putInt("id", R.id.setting_backup)
             })*/
        }

        binding.settingOthers.setOnClickListener {
            /* navigate(R.id.action_settingFragment_to_settingDetailFragment, Bundle().apply {
                 putString("title", getString(R.string.setting_title_others))
                 putInt("id", R.id.setting_others)
             })*/
        }
    }

    override fun onResume() {
        super.onResume()
        // 使用节流函数获取激活信息
        throttleActivateInfo()
    }

    /**
     * 显示激活码输入对话框
     */
    private fun showActivationDialog() {
        EditorDialogBuilder(requireActivity())
            .setTitleInt(R.string.pro_activate_dialog_title)
            .setMessage("")
            .setEditorPositiveButton(R.string.btn_confirm) { activationCode ->
                lifecycleScope.launch {
                    handleActivation(activationCode)
                }
            }
            .setNegativeButton(R.string.btn_cancel) { _, _ ->
                // 用户取消，不做任何操作
            }
            .show()
    }

    /**
     * 处理激活码验证
     */
    private suspend fun handleActivation(activationCode: String) {
        if (activationCode.trim().isEmpty()) {
            withContext(Dispatchers.Main) {
                ToastUtils.error(R.string.pro_activate_code_format_error)
            }
            return
        }

        try {
            // 调用激活API
            val result = ActivateAPI.active(activationCode.trim())

            withContext(Dispatchers.Main) {
                if (result == null) {
                    // 激活成功
                    ToastUtils.info(R.string.pro_activate_success)
                    // 重新加载激活信息
                    loadActivateInfo()
                } else {
                    // 激活失败，显示错误信息
                    ToastUtils.error(getString(R.string.pro_activate_failed, result))
                }
            }
        } catch (e: Exception) {
            Logger.e("激活码验证异常: ${e.message}", e)
            withContext(Dispatchers.Main) {
                ToastUtils.error(
                    getString(
                        R.string.pro_activate_failed,
                        e.message ?: getString(R.string.unknown_error)
                    )
                )
            }
        }
    }

    /**
     * 加载激活信息
     */
    private suspend fun loadActivateInfo() {
        try {
            // 检查 token 是否为空
            if (PrefManager.token.isEmpty()) {
                withContext(Dispatchers.Main) {
                    binding.proActivateInfo.text = getString(R.string.pro_activate_click_to_enter)
                }
                return
            }

            val info = ActivateAPI.info()

            // 切换到主线程更新UI
            withContext(Dispatchers.Main) {
                if (info.containsKey("error")) {
                    // 显示错误信息
                    val errorMsg = info["error"] ?: getString(R.string.unknown_error)
                    binding.proActivateInfo.text =
                        getString(R.string.pro_activate_info_failed, errorMsg)
                    Logger.e("激活信息接口返回错误: $errorMsg")
                } else {
                    // 显示正常信息
                    val count = info["count"] ?: "0"
                    val time = info["time"] ?: getString(R.string.unknown)
                    binding.proActivateInfo.text =
                        getString(R.string.pro_activate_info_format, count, time)
                }
            }
        } catch (e: Exception) {
            Logger.e("获取激活信息异常: ${e.message}", e)
            withContext(Dispatchers.Main) {
                binding.proActivateInfo.text = getString(R.string.pro_activate_network_error)
            }
        }
    }
}

