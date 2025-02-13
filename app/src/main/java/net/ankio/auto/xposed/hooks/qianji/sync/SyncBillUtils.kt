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
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BillInfoModel

class SyncBillUtils {
    private val PREFS_NAME = "sync_bill_cache"
    private val SYNCED_BILLS_KEY = "synced_bills"
    private val SYNC_TIME_KEY = "sync_time"
    private val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000 // 一天的毫秒数

    private fun getSyncedBills(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSyncTime = prefs.getLong(SYNC_TIME_KEY, 0)
        val currentTime = System.currentTimeMillis()

        // 如果缓存超过一天，清空缓存
        return if (currentTime - lastSyncTime > ONE_DAY_MILLIS) {
            prefs.edit()
                .clear()
                .putLong(SYNC_TIME_KEY, currentTime)
                .apply()
            setOf()
        } else {
            prefs.getStringSet(SYNCED_BILLS_KEY, setOf()) ?: setOf()
        }
    }

    private fun addToSyncedBills(context: Context, billId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val syncedBills = getSyncedBills(context).toMutableSet()
        syncedBills.add(billId)
        prefs.edit()
            .putStringSet(SYNCED_BILLS_KEY, syncedBills)
            .putLong(SYNC_TIME_KEY, System.currentTimeMillis())
            .apply()
    }

    suspend fun sync(context: Context) = withContext(Dispatchers.IO) {
        if (!UserModel.isLogin()) {
            MessageUtils.toast("未登录无法自动记账")
            return@withContext
        }
        val bills = BillInfoModel.sync()
        if (bills.isEmpty()) {
            AppRuntime.log("No bills need to sync")
            return@withContext
        }

        val syncedBills = getSyncedBills(context)
        val newBills = bills.filter { !syncedBills.contains(it.id.toString()) }

        if (newBills.isEmpty()) {
            AppRuntime.log("All bills have been synced")
            return@withContext
        }

        AppRuntime.log("Sync ${newBills.size} bills")
        AutoConfig.load()
        newBills.forEach {

            if (!AutoConfig.assetManagement){
                if (it.type === BillType.Transfer){
                    //没开启资产管理不同步转账类型
                    return@forEach
                }
            }

            if (!AutoConfig.lending){
                if (it.type === BillType.ExpendLending || it.type === BillType.IncomeLending ||
                    it.type === BillType.ExpendRepayment || it.type === BillType.IncomeRepayment){
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
            BillInfoModel.status(it.id, true)
            addToSyncedBills(context, it.id.toString())
            delay(500)
        }
        withContext(Dispatchers.Main) {
            MessageUtils.toast("已将所有账单同步完成！")
        }
    }
}