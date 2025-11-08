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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentSettingBinding
import net.ankio.auto.http.LicenseNetwork
import net.ankio.auto.http.license.ActivateAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.dialog.EditorDialogBuilder
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.PrefManager
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.theme.DynamicColors


class SettingFragment : BaseFragment<FragmentSettingBinding>() {

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
        // 简化：直接加载激活信息（内部自行切线程并串联逻辑）
        loadActivateInfo()
    }

    /**
     * 设置导航点击事件 - 统一处理所有设置项的导航
     */
    private fun setupNavigationClickListeners() {
        val navigationMap = mapOf(
            // 新的4大分类
            binding.settingCoreRecording to R.id.coreRecordingPreferenceFragment,
            binding.settingDataDisplay to R.id.dataDisplayPreferenceFragment,
            binding.settingSmartFeatures to R.id.smartFeaturesPreferenceFragment,
            binding.settingSystem to R.id.systemSettingsPreferenceFragment
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

        // 执行激活并处理结果（显示 Loading，IO 线程执行网络）
        val loading = LoadingUtils(requireContext())
        loading.show(R.string.loading)
        val result = try {
            withContext(Dispatchers.IO) { ActivateAPI.active(trimmedCode) }
        } catch (e: Exception) {
            Logger.e("激活码验证异常: ${e.message}", e)
            ToastUtils.error(
                getString(
                    R.string.pro_activate_failed,
                    e.message ?: getString(R.string.unknown_error)
                )
            )
            return
        } finally {
            loading.close()
        }

        if (result == null) {
            ToastUtils.info(R.string.pro_activate_success)
            // 激活成功后重新加载信息
            loadActivateInfo()
        } else {
            ToastUtils.error(getString(R.string.pro_activate_failed, result))
        }
    }

    /**
     * 加载激活信息 - 串行化流程，避免嵌套协程与多重状态对象
     */
    private fun loadActivateInfo() {
        // 单一入口：内部负责开启协程与线程切换
        launch {
            // 无 token：直接提示输入激活码
            if (PrefManager.token.isEmpty()) {
                withContext(Dispatchers.Main) {
                    applyActivationState(false, getString(R.string.pro_activate_click_to_enter))
                }
                return@launch
            }

            // 有 token：先显示 Loading
            withContext(Dispatchers.Main) {
                applyActivationState(false, getString(R.string.loading))
            }

            // 拉取信息（IO 线程）
            val info = withContext(Dispatchers.IO) { ActivateAPI.info() }
            if (info.isEmpty()) return@launch

            val errorMsg = info["error"]
            if (errorMsg != null) {
                Logger.e("激活信息接口返回错误: $errorMsg")
                withContext(Dispatchers.Main) {
                    applyActivationState(
                        false,
                        getString(R.string.pro_activate_info_failed, errorMsg)
                    )
                }
                return@launch
            }

            val count = info["count"] ?: "0"
            val time = info["time"] ?: getString(R.string.unknown)
            withContext(Dispatchers.Main) {
                applyActivationState(
                    true,
                    getString(R.string.pro_activate_info_format, count, time)
                )
            }
        }
    }

    /**
     * 应用激活状态到 UI - 极简且直接
     */
    private fun applyActivationState(isActivated: Boolean, displayText: String) {
        updateProCardState(isActivated)
        binding.proActivateInfo.text = displayText
    }

    // 移除文件缓存读取：避免多路径与状态分叉

    /**
     * 更新高级版卡片状态
     * @param isActivated 是否激活
     */
    private fun updateProCardState(isActivated: Boolean) {
        // 背景色已在XML中设置为SurfaceVariant，无需动态修改
        // 保持方法签名以兼容现有调用
    }
}

