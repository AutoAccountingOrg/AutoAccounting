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

package net.ankio.auto.xposed.core.utils

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.BuildConfig
import net.ankio.auto.xposed.core.logger.Logger
import net.ankio.auto.xposed.core.utils.MessageUtils.toast
import org.ezbook.server.constant.BillAction
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import org.ezbook.server.constant.SyncType
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.models.BillResultModel

class BillUtils {

    companion object {
        suspend fun handle(billResultModel: BillResultModel) {
            val billUtils = BillUtils()
            billUtils.handleUserNotification(
                billResultModel.billInfoModel,
                billResultModel.parentInfoModel
            )
        }
    }

    /**
     * 处理用户通知逻辑。
     * 决定是否弹出提示或执行其他与用户相关的操作。
     * @param billInfoModel 账单信息模型
     */
    private suspend fun handleUserNotification(
        billInfoModel: BillInfoModel,
        parent: BillInfoModel?
    ) {
        if (!billInfoModel.auto) {
            val dnd = DataUtils.configBoolean(Setting.LANDSCAPE_DND, true)
            withContext(Dispatchers.Main) {
                runCatching {
                    startAutoPanel(billInfoModel, parent, dnd)
                }
            }
        } else {
            sync2Book(AppRuntime.application!!)
            val showTip = DataUtils.configBoolean(Setting.SHOW_AUTO_BILL_TIP, true)
            if (showTip) {
                toast("已自动记录账单(￥${billInfoModel.money})。")
            }
        }
    }


    private val TAG = "BillUtils"

    /**
     * 同步账单到记账应用
     * @param context 应用上下文
     */
    private suspend fun sync2Book(context: Context) {
        // 获取记账应用包名，如果未设置则直接返回
        val packageName = DataUtils.configString(Setting.BOOK_APP_ID, "")
        if (packageName.isEmpty()) {
            Logger.log(TAG, "未设置记账应用")
            return
        }

        // 获取同步类型设置
        val syncType = getSyncType()

        // 如果是打开应用时同步，则直接返回
        if (syncType == SyncType.WhenOpenApp.name) {
            Logger.logD(TAG, "设置为打开应用时同步")
            return
        }

        // 获取待同步的账单
        val pendingBills = BillInfoModel.sync()
        if (pendingBills.isEmpty()) {
            Logger.logD(TAG, "无需同步：没有待同步账单")
            return
        }

        // 检查是否需要启动同步
        if (shouldStartSync(syncType, pendingBills.size)) {
            launchBookApp(context, packageName)
        }
    }

    /**
     * 获取同步类型设置
     * @return 同步类型，默认为打开应用时同步
     */
    private suspend fun getSyncType(): String {
        val type =
            DataUtils.configString(Setting.SYNC_TYPE, SyncType.WhenOpenApp.name)
        if (type !in SyncType.entries.map { it.name }) {
            return SyncType.WhenOpenApp.name
        }
        return type
    }

    /**
     * 检查是否应该开始同步
     * @param syncType 同步类型
     * @param pendingCount 待同步账单数量
     * @return 是否应该开始同步
     */
    private fun shouldStartSync(syncType: String, pendingCount: Int): Boolean =
        (syncType == SyncType.BillsLimit10.name && pendingCount >= 10) ||
                (syncType == SyncType.BillsLimit5.name && pendingCount >= 5) ||
                (syncType == SyncType.BillsLimit1.name && pendingCount >= 1)

    /**
     * 启动记账应用
     * @param context 应用上下文
     * @param packageName 记账应用包名
     */
    private suspend fun launchBookApp(context: Context, packageName: String) {
        runCatching {

            var activityName =
                DataUtils.configString(Setting.BOOK_APP_ACTIVITY, DefaultData.BOOK_APP_ACTIVITY)


            if (activityName.isEmpty()) {
                activityName = DefaultData.BOOK_APP_ACTIVITY
            }

            if (activityName == DefaultData.BOOK_APP_ACTIVITY && packageName !== DefaultData.BOOK_APP) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                }
                return
            }
            // val launchIntent = app.packageManager.getLaunchIntentForPackage(packageName)
            val intent = Intent().apply {
                setClassName(packageName, activityName) // 设置目标应用和目标 Activity
                putExtra("from", BuildConfig.APPLICATION_ID) // 添加额外参数
                putExtra("action", BillAction.SYNC_BILL) // 传递 action
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // 确保在新任务栈中启动
            }
            context.startActivity(intent)
        }.onFailure { error ->
            Logger.log(TAG, "启动记账应用失败：${error.message}")
        }
    }

    /**
     * 启动自动记账面板
     * @param billInfoModel 账单信息模型
     * @param parent 父账单信息
     * @param dnd 横屏勿扰
     */
    private suspend fun startAutoPanel(
        billInfoModel: BillInfoModel,
        parent: BillInfoModel?,
        dnd: Boolean = false
    ) {
        val isLandscape = isLandscapeMode()
        Logger.log(TAG, "横屏状态：$isLandscape, 是否横屏勿扰：$dnd")
        // 检查横屏状态并处理
        if (isLandscape && dnd) {
            toast("账单金额：${billInfoModel.money}，横屏状态下为您自动暂存。")
            return
        }

        // 创建并启动悬浮窗
        launchFloatingWindow(billInfoModel, parent)
    }

    /**
     * 检查当前设备是否处于横屏模式
     * @return Boolean 如果是横屏返回true，否则返回false
     */
    private fun isLandscapeMode(): Boolean =
        AppRuntime.application!!.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE


    /**
     * 启动悬浮窗口来显示账单信息
     * @param billInfoModel 要显示的账单信息模型
     * @param parent 父账单信息，可能为null，用于关联相关账单
     * @throws SecurityException 如果应用没有必要的权限
     */
    private suspend fun launchFloatingWindow(billInfoModel: BillInfoModel, parent: BillInfoModel?) {
        val intent =
            net.ankio.auto.intent.FloatingIntent(billInfoModel, true, "JsRoute", parent).toIntent()
        Logger.log(TAG, "拉起自动记账悬浮窗口：$intent")

        runCatching {
            AppRuntime.application!!.startActivity(intent)
        }.onFailure { throwable ->
            Logger.log(TAG, "自动记账悬浮窗拉起失败：$throwable")
            Logger.logE(TAG, throwable)
        }
    }
}