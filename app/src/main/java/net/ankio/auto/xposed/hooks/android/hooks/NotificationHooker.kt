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

package net.ankio.auto.xposed.hooks.android.hooks

import android.app.Notification
import com.google.gson.Gson
import com.google.gson.JsonObject
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.http.api.SettingAPI
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AnalysisUtils
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.CoroutineUtils
import org.ezbook.server.constant.DataType
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import org.ezbook.server.tools.MD5HashTable

/**
 * 通知 Hook：拦截系统通知入队和发布，对白名单应用进行去重后上报。
 * Hook 点：enqueueNotificationInternal（入队）、notifyCallNotificationEventListenerOnPosted（发布）
 */
class NotificationHooker : PartHooker() {
    private val hashTable = MD5HashTable()
    private val gson = Gson()

    override fun hook() {
        hookEnqueueNotification()
        hookNotifyPosted()
    }

    /** Hook enqueueNotificationInternal：通知入队入口，Android 8.0+ 稳定存在 */
    private fun hookEnqueueNotification() {
        runCatching {
            val nmsClass =
                Hooker.loader("com.android.server.notification.NotificationManagerService")
            Hooker.allMethodsEqBefore(nmsClass, "enqueueNotificationInternal") { param, _ ->
                runCatching {
                    val pkg = param.args.getOrNull(0) as? String
                    if (pkg == null) {
                        d(" enqueueNotificationInternal pkg is null, args=${param.args.size}")
                        return@runCatching
                    }
                    val notification = param.args.filterIsInstance<Notification>().firstOrNull()
                    if (notification == null) {
                        d(" enqueueNotificationInternal notification not found, pkg=$pkg")
                        return@runCatching
                    }
                    processNotification(pkg, notification)
                }.onFailure { e("hookEnqueueNotification failed: ${it.message}", it) }
                null
            }
            d(" enqueueNotificationInternal hooked")
        }.onFailure { e("Failed to hook enqueueNotificationInternal: ${it.message}", it) }
    }

    /** Hook notifyCallNotificationEventListenerOnPosted：通知发布给监听器时调用 */
    private fun hookNotifyPosted() {
        runCatching {
            val nmsClass =
                Hooker.loader("com.android.server.notification.NotificationManagerService")
            Hooker.allMethodsEqBefore(
                nmsClass,
                "notifyCallNotificationEventListenerOnPosted"
            ) { param, _ ->
                runCatching {
                    val record = param.args.firstOrNull()
                    if (record == null) {
                        d(" notifyPosted record is null")
                        return@runCatching
                    }
                    val sbn = XposedHelpers.getObjectField(
                        record,
                        "sbn"
                    ) as? android.service.notification.StatusBarNotification
                    if (sbn == null) {
                        d(" notifyPosted sbn is null")
                        return@runCatching
                    }
                    val notification = sbn.notification
                    if (notification == null) {
                        d(" notifyPosted notification is null, pkg=${sbn.packageName}")
                        return@runCatching
                    }
                    processNotification(sbn.packageName, notification)
                }.onFailure { e("hookNotifyPosted failed: ${it.message}", it) }
                null
            }
            d(" notifyCallNotificationEventListenerOnPosted hooked")
        }.onFailure {
            e(
                "Failed to hook notifyCallNotificationEventListenerOnPosted: ${it.message}",
                it
            )
        }
    }

    /**
     * 处理通知：提取内容、去重、异步上报。
     * extras 文本优先级：大文本 > 普通文本 > 子文本
     */
    private fun processNotification(pkg: String, notification: Notification) {
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
            ?: "").toString()

        val hash = MD5HashTable.md5("$pkg$title$text")
        if (hashTable.contains(hash)) {
            d(" skip duplicate content, hash=$hash, pkg=$pkg")
            return
        }
        hashTable.put(hash)

        d("Notification: app=$pkg, title=$title, text=$text")
        CoroutineUtils.withIO { reportNotification(pkg, title, text) }
    }

    /** 白名单过滤后上报，短信走 DATA 类型，其余走 NOTICE */
    private suspend fun reportNotification(pkg: String, title: String, text: String) {
        if (title.isEmpty() && text.isEmpty()) {
            d(" skip empty content, pkg=$pkg")
            return
        }
        val apps = SettingAPI.get(Setting.LISTENER_APP_LIST, DefaultData.APP_FILTER).split(",")
        if (pkg !in apps) {
            d(" pkg=$pkg not in whitelist, apps=$apps")
            return
        }

        val (targetPkg, dataType, json) = when (pkg) {
            "com.android.mms" -> Triple("com.android.phone", DataType.DATA, JsonObject().apply {
                addProperty("sender", "")
                addProperty("body", text)
                addProperty("t", System.currentTimeMillis())
            })

            else -> Triple(pkg, DataType.NOTICE, JsonObject().apply {
                addProperty("title", title)
                addProperty("text", text)
                addProperty("t", System.currentTimeMillis())
            })
        }
        d(" report targetPkg=$targetPkg dataType=$dataType json=$json")
        AnalysisUtils.analysisData(targetPkg, dataType, gson.toJson(json))
    }
}