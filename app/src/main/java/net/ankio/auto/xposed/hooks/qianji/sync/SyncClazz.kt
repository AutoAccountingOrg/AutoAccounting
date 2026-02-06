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

package net.ankio.auto.xposed.hooks.qianji.sync

import android.content.Context
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.ankio.auto.xposed.core.api.HookerClazz
import net.ankio.auto.xposed.hooks.qianji.helper.BillDbHelper
import net.ankio.auto.xposed.hooks.qianji.models.QjBillModel
import net.ankio.dex.model.Clazz
import net.ankio.dex.model.ClazzField
import net.ankio.dex.model.ClazzMethod

/**
 * 同步调度器类的规则定义，用于在宿主中定位同步相关实现。
 */
class SyncClazz(private val obj: Any) {

    fun toObject() = obj

    companion object : HookerClazz() {
        /**
         * 反射匹配规则：通过方法签名与关键字符串锁定同步调度器类。
         */
        override var rule = Clazz(
            name = this::class.java.name,
            nameRule = "^\\w{0,2}\\..+",
            type = "class",
            methods = listOf(
                ClazzMethod(
                    name = "startSync",
                    returnType = "boolean",
                    parameters = listOf(
                        ClazzField(type = "android.content.Context"),
                    ),
                ),
                ClazzMethod(
                    name = "startPush",
                    returnType = "void",
                    parameters = listOf(
                        ClazzField(type = "android.content.Context"),
                    ),
                ),
                ClazzMethod(
                    name = "stop",
                    returnType = "void",
                ),
                ClazzMethod(
                    name = "getBookLastSyncTimes",
                    returnType = "com.google.gson.JsonObject",
                ),
                ClazzMethod(
                    name = "getUnPushCount",
                    returnType = "long",
                    parameters = listOf(
                        ClazzField(type = "boolean"),
                    ),
                ),
                ClazzMethod(
                    name = "deleteBook",
                    returnType = "void",
                    parameters = listOf(
                        ClazzField(type = "long"),
                    ),
                ),
                ClazzMethod(
                    name = "isSyncing",
                    returnType = "boolean",
                ),
            ),
        )

        fun getInstance(): SyncClazz {
            // 通过规则定位宿主同步类
            val syncClazz = clazz()
            // 获取 Kotlin Companion 单例对象
            val companion = XposedHelpers.getStaticObjectField(syncClazz, "Companion")
            // 调用 getInstance() 获取宿主单例
            val instance = XposedHelpers.callMethod(companion, "getInstance")

            return SyncClazz(instance)
        }
    }


    /**
     * 调用宿主同步调度器的 startPush(context)。
     * 说明：等价于宿主侧的 `ie.c.Companion.getInstance().startPush(context)`。
     */
    fun startPush(context: Context) {
        XposedHelpers.callMethod(obj, "startPush", context)
    }

    // TODO 这里有个时序的问题。自动记账在处理钱迹账单的时候，此时钱迹可能正在进行云端同步。此时若刚好在保存完成后进行同步，云端数据会覆盖本地数据，从而导致记账失败。所以这里延迟5秒重新保存并将账单设置为未同步的状态重新同步，这样会覆盖之前的同步数据。
    suspend fun startPushBill(context: Context, qjBillModel: QjBillModel) =
        withContext(Dispatchers.IO) {
            delay(5_000)
            qjBillModel.setUpdateTimeInSec(System.currentTimeMillis() / 1000)
            qjBillModel.setStatus(QjBillModel.STATUS_NOT_SYNC)
            BillDbHelper.newInstance().saveOrUpdateBill(qjBillModel)
            startPush(context)
        }

    fun isSyncing(): Boolean {
        return XposedHelpers.callMethod(obj, "isSyncing") as Boolean
    }

}