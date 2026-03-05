/*
 * Copyright (C) 2026 ankio(ankio@ankio.net)
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

package net.ankio.auto.ui.vm

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.service.CoreService
import net.ankio.auto.service.OcrService
import net.ankio.auto.App
import net.ankio.auto.storage.backup.BackupManager
import net.ankio.auto.update.AppUpdateHelper
import net.ankio.auto.update.RuleUpdateHelper
import net.ankio.auto.update.UpdateModel
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.Throttle
import net.ankio.auto.xposed.XposedModule
import java.util.Locale

/**
 * 首页 Activity 的 ViewModel，负责规则与应用更新检查、状态卡 UI 数据等。
 */
class HomeActivityVm : ViewModel() {

    /** 规则更新信息，非 null 时表示有新版本可展示 */
    val ruleUpdateModel = MutableLiveData<UpdateModel?>()

    /** 应用更新信息，非 null 时表示有新版本可展示 */
    val appUpdateModel = MutableLiveData<UpdateModel?>()

    /** 状态卡 UI 数据（规则版本、工作模式、激活状态等） */
    val statusData = MutableLiveData<StatusData>()

    /** Pref 全量同步节流：5 分钟，持久化以支持进程重启后节流 */
    private val prefSyncThrottle = Throttle.asFunction(
        intervalMs = 5 * 60 * 1000L,
        persistKey = "pref_sync"
    ) { App.launch { PrefManager.syncAllToBackend() } }

    /** 自动检查更新节流：30 分钟，持久化以支持进程重启后节流 */
    private val updateCheckThrottle = Throttle.asFunction(
        intervalMs = 30 * 60 * 1000L,
        persistKey = "auto_update_check"
    ) {
        if (RuleUpdateHelper.isAutoCheckEnabled()) checkRuleUpdate()
        if (AppUpdateHelper.isAutoCheckEnabled()) checkAppUpdate()
    }

    /** 页面 onResume 时调用，触发节流任务 */
    fun startSyncUpdateTask() {
        prefSyncThrottle()
        updateCheckThrottle()
    }

    fun startAutoBackup() {
        viewModelScope.launch {
            BackupManager.autoBackup()
        }
    }

    /** 检查规则更新，结果通过 [ruleUpdateModel] 通知 UI；fromUser 时无更新会 Toast */
    fun checkRuleUpdate(fromUser: Boolean = false) {
        if (fromUser) ToastUtils.info(R.string.check_update)
        viewModelScope.launch {
            val result = RuleUpdateHelper.check()
            if (result != null) {
                ruleUpdateModel.value = result
            } else if (fromUser) {
                ToastUtils.error(R.string.no_need_to_update)
            }
        }
    }

    /** 检查应用更新，结果通过 [appUpdateModel] 通知 UI；fromUser 时无更新会 Toast */
    fun checkAppUpdate(fromUser: Boolean = false) {
        if (fromUser) ToastUtils.info(R.string.check_update)
        viewModelScope.launch {
            val result = AppUpdateHelper.check()
            if (result != null) {
                appUpdateModel.value = result
            } else if (fromUser) {
                ToastUtils.error(R.string.no_need_to_update)
            }
        }
    }

    /** 刷新状态卡数据，供 StatusCardComponent 观察 */
    fun refreshStatus(context: Context) {
        val isActive = when (PrefManager.workMode) {
            WorkMode.Ocr -> OcrService.serverStarted
            WorkMode.LSPatch -> CoreService.isRunning(context)
            WorkMode.Xposed -> XposedModule.active()
        }
        val channelValue = PrefManager.appChannel.lowercase(Locale.getDefault())
        statusData.value = StatusData(
            isActive = isActive,
            versionName = BuildConfig.VERSION_NAME,
            workMode = PrefManager.workMode,
            debugMode = PrefManager.debugMode,
            ruleVersion = PrefManager.ruleVersion,
            ruleUpdate = PrefManager.ruleUpdate,
            channelValue = channelValue
        )
    }

    /** 消费规则更新事件，避免配置变更后重复弹窗 */
    fun consumeRuleUpdate() {
        ruleUpdateModel.value = null
    }

    /** 消费应用更新事件，避免配置变更后重复弹窗 */
    fun consumeAppUpdate() {
        appUpdateModel.value = null
    }
}

/** 状态卡展示所需的数据 */
data class StatusData(
    val isActive: Boolean,
    val versionName: String,
    val workMode: WorkMode,
    val debugMode: Boolean,
    val ruleVersion: String,
    val ruleUpdate: String,
    val channelValue: String
)
