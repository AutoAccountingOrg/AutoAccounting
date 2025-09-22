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

package net.ankio.auto.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.databinding.FragmentPluginHomeBinding
import net.ankio.auto.http.LocalNetwork
import net.ankio.auto.http.api.BillAPI
import net.ankio.auto.service.CoreService
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.activity.MainActivity
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.api.bindAs
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.fragment.components.BookCardComponent
import net.ankio.auto.ui.fragment.components.MonthlyCardComponent
import net.ankio.auto.ui.fragment.components.StatusCardComponent
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.intent.BillInfoIntent

class HomeFragment : BaseFragment<FragmentPluginHomeBinding>() {
    private val gson = Gson()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val statusCard: StatusCardComponent = binding.activeCard.bindAs()


        val monthlyCard: MonthlyCardComponent = binding.monthlyCard.bindAs()
        monthlyCard
            .setOnNavigateToAiSummary { periodData ->
                // 使用Bundle传递周期数据
                val bundle = Bundle()
                if (periodData != null) {
                    bundle.putString("period_data", gson.toJson(periodData))
                }
                // 使用目的地 ID 导航，避免当前目的地识别为 NavGraph 时解析不到 action
                findNavController().navigate(R.id.aiSummaryFragment, bundle)
            }
            .setOnNavigateToStatistics {
                // 跳转到统计页面
                findNavController().navigate(R.id.statisticFragment)
            }

        val bookCard: BookCardComponent = binding.bookCard.bindAs()
        bookCard.setOnRedirect { navigationId, bundle ->
            findNavController().navigate(navigationId, bundle)
        }
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.title_log -> {
                    // 使用目的地 ID 导航
                    findNavController().navigate(R.id.logFragment)
                    true
                }


                R.id.title_explore -> {
                    PrefManager.introIndex = 0
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                    true
                }
                else -> false
            }
        }

        // 检查并显示Canary版本警告
        checkAndShowCanaryWarning()
        checkServer()
        checkNoEditBills()
    }

    /**
     * 检查并显示Canary版本警告
     * 只在用户首次升级到Canary版本时显示警告
     */
    private fun checkAndShowCanaryWarning() {
        if (BuildConfig.VERSION_NAME.contains("Canary") &&
            PrefManager.lastCanaryWarningVersion != BuildConfig.VERSION_NAME
        ) {
            showCanaryRiskDialog()
            PrefManager.lastCanaryWarningVersion = BuildConfig.VERSION_NAME
        }
    }

    /**
     * 显示Canary版本风险提醒对话框
     */
    private fun showCanaryRiskDialog() {
        BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
            .setTitle(getString(R.string.canary_warning_title))
            .setMessage(getString(R.string.canary_warning_message))
            .setPositiveButton(getString(R.string.canary_warning_confirm)) { _, _ ->
                // 用户确认了解风险
            }
            .setNegativeButton(getString(R.string.canary_warning_enable_debug)) { _, _ ->
                // 直接开启调试模式，方便用户反馈问题
                PrefManager.debugMode = true
            }
            .show()
    }


    private fun checkServer() {
        if (WorkMode.isOcrOrLSPatch()) return

        launch {
            LocalNetwork.get<String>("/").onSuccess {
                if (it.data != BuildConfig.VERSION_NAME) {
                    Logger.d("server:${it.data}, ${BuildConfig.VERSION_NAME}")
                    // 版本不匹配，提示用户重启设备
                    showServerVersionMismatchDialog(it.data ?: "")
                }
            }.onFailure {
                // 连接失败，提示用户
                showServerConnectionFailedDialog()
            }
        }
    }

    /**
     * 显示服务器版本不匹配对话框
     */
    private fun showServerVersionMismatchDialog(serverVersion: String) {
        BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
            .setTitle(getString(R.string.server_error_restart_title))
            .setMessage(
                getString(
                    R.string.server_error_version_message,
                    serverVersion,
                    BuildConfig.VERSION_NAME
                )
            )
            .setPositiveButton(getString(R.string.server_error_btn_ok)) { _, _ ->
                // 用户确认知道了
            }
            .show()
    }

    /**
     * 显示服务器连接失败对话框
     */
    private fun showServerConnectionFailedDialog() {
        BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
            .setTitle(getString(R.string.server_error_restart_title))
            .setMessage(getString(R.string.server_error_connection_message))
            .setPositiveButton(getString(R.string.server_error_btn_ok)) { _, _ ->
                // 用户确认知道了
            }
            .show()
    }


    /**
     * 检查待编辑的账单并启动弹窗
     *
     * 功能：
     * - 获取所有状态为 Wait2Edit 的账单
     * - 如果有待编辑账单，通过 OverlayService 启动编辑弹窗
     * - 确保服务已启动，避免在服务未就绪时调用
     */
    private fun checkNoEditBills() {
        launch {
            // 获取待编辑账单列表
            val pendingBills = BillAPI.edit()

            if (pendingBills.isNotEmpty()) {
                Logger.d("发现 ${pendingBills.size} 个待编辑账单")

                pendingBills.forEach { bill ->
                    val intent = BillInfoIntent(bill, "edit")
                    CoreService.start(requireActivity(), intent.toIntent())
                }
            } else {
                Logger.d("没有待编辑的账单")
            }
        }
    }

}