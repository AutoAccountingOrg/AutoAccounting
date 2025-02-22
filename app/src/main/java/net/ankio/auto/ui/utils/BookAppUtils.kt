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

package net.ankio.auto.ui.utils

import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.ankio.auto.App.Companion.app
import net.ankio.auto.BuildConfig
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import org.ezbook.server.constant.BillAction
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.SettingModel

object BookAppUtils {

    private suspend fun createIntent(action: BillAction) = withContext(Dispatchers.Main) {
        runCatching {
            var packageName = ConfigUtils.getString(Setting.BOOK_APP_ID, DefaultData.BOOK_APP)

            if (packageName.isEmpty()) {
                packageName = DefaultData.BOOK_APP
            }

            var activityName =
                SettingModel.get(Setting.BOOK_APP_ACTIVITY, DefaultData.BOOK_APP_ACTIVITY)

            if (activityName.isEmpty()) {
                activityName = DefaultData.BOOK_APP_ACTIVITY
            }

            Logger.i("createIntent: $packageName $activityName, action: $action")

            val noProactively = SettingModel.get(
                Setting.PROACTIVELY_MODEL,
                DefaultData.PROACTIVELY_MODEL.toString()
            ) == "false"

            if (activityName == DefaultData.BOOK_APP_ACTIVITY && packageName != DefaultData.BOOK_APP || noProactively) {
                val launchIntent = app.packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    app.startActivity(launchIntent)
                }
                return@withContext
            }
            // val launchIntent = app.packageManager.getLaunchIntentForPackage(packageName)
            val intent = Intent().apply {
                setClassName(packageName, activityName) // 设置目标应用和目标 Activity
                putExtra("from", BuildConfig.APPLICATION_ID) // 添加额外参数
                putExtra("action", action.name) // 传递 action
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // 清除栈顶
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // 确保在新任务栈中启动
            }
            app.startActivity(intent)
        }.onFailure {
            it.printStackTrace()
            Logger.e("createIntent error: ${it.message}")
        }
    }

    suspend fun syncData() {
        createIntent(BillAction.SYNC_BILL)
    }

    suspend fun syncBookCategoryAsset() {
        createIntent(BillAction.SYNC_BOOK_CATEGORY_ASSET)
    }

    suspend fun syncReimburseBill() {
        createIntent(BillAction.SYNC_REIMBURSE_BILL)
    }

    suspend fun syncRecentExpenseBill() {
        createIntent(BillAction.SYNC_RECENT_EXPENSE_BILL)
    }

}