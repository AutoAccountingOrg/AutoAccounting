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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentSettingBinding
import net.ankio.auto.http.LicenseNetwork
import net.ankio.auto.http.license.ActivateAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.dialog.EditorDialogBuilder
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.Throttle
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.utils.toThemeColor


class SettingFragment : BaseFragment<FragmentSettingBinding>() {

    /**
     * 激活状态数据类 - 统一管理激活相关状态
     */
    private data class ActivationState(
        val isActivated: Boolean = false,
        val displayText: String = "",
        val isError: Boolean = false
    )

    // 缓存键删除：保持逻辑单一

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.item_purchase -> CustomTabsHelper.launchUrl(
                    requireActivity(),
                    LicenseNetwork.url.toUri()
                )

                else -> false
            }
        }

        // 设置激活信息卡片点击事件
        binding.proCard.setOnClickListener {
            showActivationDialog()
        }

        // 设置导航点击事件 - 消除重复代码
        setupNavigationClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // 简化：直接加载激活信息
        launch { loadActivateInfo() }
    }

    /**
     * 设置导航点击事件 - 统一处理所有设置项的导航
     */
    private fun setupNavigationClickListeners() {
        val navigationMap = mapOf(
            // 使用目的地 ID 导航，避免当前目的地为 NavGraph 时解析不到 action
            binding.settingBill to R.id.billPreferenceFragment,
            binding.settingPopup to R.id.popupPreferenceFragment,
            binding.settingFeatures to R.id.featuresPreferenceFragment,
            binding.settingAi to R.id.aiPreferenceFragment,
            binding.settingAppearance to R.id.appearancePreferenceFragment,
            binding.settingBackup to R.id.backupPreferenceFragment,
            binding.settingOthers to R.id.othersPreferenceFragment
        )

        navigationMap.forEach { (view, actionId) ->
            view.setOnClickListener { findNavController().navigate(actionId) }
        }
    }

    // 移除多余的UI切换包装，直接在需要处切到主线程

    /**
     * 显示激活码输入对话框
     */
    private fun showActivationDialog() {
        BaseSheetDialog.create<EditorDialogBuilder>(requireContext())
            .setTitleInt(R.string.pro_activate_dialog_title)
            .setMessage("")
            .setEditorPositiveButton(R.string.btn_confirm) { activationCode ->
                launch {
                    handleActivation(activationCode)
                }
            }
            .setNegativeButton(R.string.btn_cancel) { _, _ ->
                // 用户取消，不做任何操作
            }
            .show()
    }

    /**
     * 处理激活码验证 - 统一错误处理和UI更新
     */
    private suspend fun handleActivation(activationCode: String) {
        val trimmedCode = activationCode.trim()

        // 输入验证
        if (trimmedCode.isEmpty()) {
            ToastUtils.error(R.string.pro_activate_code_format_error)
            return
        }

        // 执行激活并处理结果
        runCatching { ActivateAPI.active(trimmedCode) }
            .onSuccess { result ->

                if (result == null) {
                    ToastUtils.info(R.string.pro_activate_success)
                    // 激活成功后重新加载信息
                    launch { loadActivateInfo() }
                } else {
                    ToastUtils.error(getString(R.string.pro_activate_failed, result))
                }

            }
            .onFailure { e ->
                Logger.e("激活码验证异常: ${e.message}", e)

                ToastUtils.error(
                    getString(
                        R.string.pro_activate_failed,
                        e.message ?: getString(R.string.unknown_error)
                    )
                )

            }
    }

    /**
     * 加载激活信息 - 使用状态数据类简化逻辑
     */
    private suspend fun loadActivateInfo() {
        // 无 token：直接提示输入激活码
        if (PrefManager.token.isEmpty()) {
            withContext(Dispatchers.Main) {
                applyActivationState(
                    ActivationState(
                        isActivated = false,
                        displayText = getString(R.string.pro_activate_click_to_enter),
                        isError = false
                    )
                )
            }
            return
        }

        // 有 token：先显示 Loading，再异步拉取最新信息
        withContext(Dispatchers.Main) {
            applyActivationState(
                ActivationState(
                    isActivated = false,
                    displayText = getString(R.string.loading),
                    isError = false
                )
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            ActivateAPI.clearInfoCache()
            val data = ActivateAPI.info()
            if (data.isNotEmpty()) {
                val fresh = createActivationStateFromInfo(data)
                withContext(Dispatchers.Main) { applyActivationState(fresh) }
            }
        }
    }

    /**
     * 根据API返回信息创建激活状态
     */
    private fun createActivationStateFromInfo(info: HashMap<String, String>): ActivationState {
        return if (info.containsKey("error")) {
            val errorMsg = info["error"] ?: getString(R.string.unknown_error)
            Logger.e("激活信息接口返回错误: $errorMsg")
            ActivationState(
                isActivated = false,
                displayText = getString(R.string.pro_activate_info_failed, errorMsg),
                isError = true
            )
        } else {
            val count = info["count"] ?: "0"
            val time = info["time"] ?: getString(R.string.unknown)
            ActivationState(
                isActivated = true,
                displayText = getString(R.string.pro_activate_info_format, count, time),
                isError = false
            )
        }
    }

    /**
     * 应用激活状态到UI - 统一的状态更新逻辑
     */
    private fun applyActivationState(state: ActivationState) {
        runCatching {
            updateProCardState(state.isActivated)
            binding.proActivateInfo.text = state.displayText
        }
    }

    // 移除文件缓存读取：避免多路径与状态分叉

    /**
     * 更新高级版卡片状态
     * @param isActivated 是否激活
     */
    private fun updateProCardState(isActivated: Boolean) {
        if (isActivated) {
            // 激活状态：显示渐变背景图片，隐藏普通背景
            // binding.proGradientBackground.visibility = View.VISIBLE
            binding.proCardContent.setBackgroundResource(android.R.color.transparent)
        } else {
            // 未激活状态：隐藏渐变背景图片，显示普通背景
            //  binding.proGradientBackground.visibility = View.GONE
            // 使用主题颜色作为背景
            val backgroundColor =
                com.google.android.material.R.attr.colorPrimaryContainer.toThemeColor()
            binding.proCardContent.setBackgroundColor(backgroundColor)
        }
    }
}

