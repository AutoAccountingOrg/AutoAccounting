/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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

package net.ankio.auto.xposed.hooks.qianji.sync

import io.github.oshai.kotlinlogging.KotlinLogging
import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.MessageUtils
import net.ankio.auto.xposed.hooks.qianji.models.UserModel
import net.ankio.auto.xposed.hooks.qianji.tools.QianJiUri
import net.ankio.auto.http.api.BillAPI
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BillInfoModel

class SyncBillUtils {
    private val logger = KotlinLogging.logger(this::class.java.name)

    companion object {
        private const val PREFS_NAME = "sync_status"
        private const val LAST_SYNC_TIME_KEY = "last_sync_time"
        private const val SYNC_STATE_TIME_KEY = "sync_state_time"
        private const val MIN_INTERVAL = 5000L // 5秒的间隔时间
        private const val SYNC_TIMEOUT = 30000L // 30秒的超时时间
    }

    private fun getLastSyncTime(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(LAST_SYNC_TIME_KEY, 0L)
    }

    private fun updateLastSyncTime(context: Context, time: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(LAST_SYNC_TIME_KEY, time)
            .apply()
    }

    private fun getSyncState(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val syncStateTime = prefs.getLong(SYNC_STATE_TIME_KEY, 0L)
        val currentTime = System.currentTimeMillis()

        // 如果上次同步状态时间超过30秒，认为同步已经结束
        return if (currentTime - syncStateTime < SYNC_TIMEOUT) {
            true
        } else {
            // 清除过期的同步状态
            prefs.edit().remove(SYNC_STATE_TIME_KEY).apply()
            false
        }
    }

    private fun updateSyncState(context: Context, isSyncing: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        if (isSyncing) {
            editor.putLong(SYNC_STATE_TIME_KEY, System.currentTimeMillis())
        } else {
            editor.remove(SYNC_STATE_TIME_KEY)
        }
        editor.apply()
    }

    suspend fun sync(context: Context) = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        if (getSyncState(context)) {
            logger.info { "同步任务正在进行中，忽略本次调用" }
            return@withContext
        }
        if (currentTime - getLastSyncTime(context) < MIN_INTERVAL) {
            logger.info { "调用过于频繁，请稍后再试" }
            return@withContext
        }

        try {
            updateSyncState(context, true)
            updateLastSyncTime(context, currentTime)

            if (!UserModel.isLogin()) {
                MessageUtils.toast("未登录无法自动记账")
                return@withContext
            }
            val bills = BillAPI.sync()
            if (bills.isEmpty()) {
                logger.info { "No bills need to sync" }
                return@withContext
            }

            logger.info { "Sync ${bills.size} bills" }
            AutoConfig.load()
            bills.forEach {

                if (!AutoConfig.assetManagement) {
                    if (it.type === BillType.Transfer) {
                        //没开启资产管理不同步转账类型
                        return@forEach
                    }
                }

                if (!AutoConfig.lending) {
                    if (it.type === BillType.ExpendLending || it.type === BillType.IncomeLending ||
                        it.type === BillType.ExpendRepayment || it.type === BillType.IncomeRepayment
                    ) {
                        //没开启债务不同步报销类型
                        return@forEach
                    }
                }

                val bill = QianJiUri.toQianJi(it)
                val intent = Intent(Intent.ACTION_VIEW, bill)
                intent.putExtra("billInfo", Gson().toJson(it))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                withContext(Dispatchers.Main) {
                    context.startActivity(intent)
                }
                BillAPI.status(it.id, true)
                delay(500)
            }
            withContext(Dispatchers.Main) {
                MessageUtils.toast("已将所有账单同步完成！")
            }
        } finally {
            updateSyncState(context, false)
        }
    }
}