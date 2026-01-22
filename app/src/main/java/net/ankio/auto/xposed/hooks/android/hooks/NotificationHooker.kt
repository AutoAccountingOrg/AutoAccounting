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
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AppRuntime
import org.ezbook.server.tools.MD5HashTable
import net.ankio.auto.xposed.core.utils.CoroutineUtils
import org.ezbook.server.constant.DataType
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import net.ankio.auto.http.api.SettingAPI
import net.ankio.auto.xposed.core.utils.AnalysisUtils


class NotificationHooker : PartHooker() {
    private val hashTable = MD5HashTable()

    override fun hook() {
        hookNotifyListenersPostedAndLogLocked()
    }


    /**
     * Hook enqueueNotificationInternal - 通知入队的统一入口
     * 这个方法在Android 8.0+版本稳定存在，是最可靠的hook点
     */
    private fun hookEnqueueNotification() {
        // TODO 部分情况下无法获取
        try {
            val nmsClass =
                Hooker.loader("com.android.server.notification.NotificationManagerService")
            Hooker.allMethodsEqBefore(nmsClass, "enqueueNotificationInternal") { param, method ->
                runCatching {
                    // enqueueNotificationInternal的第一个参数通常是包名(String)
                    // 需要从参数中找到NotificationRecord或StatusBarNotification
                    extractAndProcessNotification(param)
                }.onFailure {
                    AppRuntime.manifest.e("hookEnqueueNotification failed: ${it.message}")
                }
                null
            }
        } catch (e: Exception) {
            AppRuntime.manifest.e("Failed to hook enqueueNotificationInternal: ${e.message}")
        }
    }

    /**
     * Hook notifyListenersPostedAndLogLocked - 通知发布的核心方法
     * 这个方法在通知真正发布给监听器时调用
     * 参数：(NotificationRecord r, NotificationRecord old, ...)
     */
    private fun hookNotifyListenersPostedAndLogLocked() {
        try {
            val nmsClass =
                Hooker.loader("com.android.server.notification.NotificationManagerService")
            Hooker.allMethodsEqBefore(
                nmsClass,
                "notifyCallNotificationEventListenerOnPosted"
            ) { param, method ->
                runCatching {
                    // 第一个参数是NotificationRecord，包含StatusBarNotification
                    val record = param.args.firstOrNull() ?: return@runCatching

                    // 安全地提取sbn字段
                    val sbnField = record.javaClass.declaredFields.find { it.name == "sbn" }
                    if (sbnField != null) {
                        sbnField.isAccessible = true
                        val sbn =
                            sbnField.get(record) as? android.service.notification.StatusBarNotification
                        if (sbn != null) {
                            processNotification(sbn)
                        }
                    }
                }.onFailure {
                    AppRuntime.manifest.e(
                        "hookNotifyListenersPostedAndLogLocked failed: ${it.message}",
                        it
                    )
                }
                null
            }
        } catch (e: Exception) {
            AppRuntime.manifest.e(
                "Failed to hook notifyListenersPostedAndLogLocked: ${e.message}",
                e
            )
        }
    }

    /**
     * 从方法参数中提取并处理通知
     * 尝试从各种可能的参数类型中获取StatusBarNotification
     */
    private fun extractAndProcessNotification(param: de.robv.android.xposed.XC_MethodHook.MethodHookParam) {
        // 遍历所有参数，找到StatusBarNotification或NotificationRecord
        for (arg in param.args) {
            when (arg) {
                is android.service.notification.StatusBarNotification -> {
                    processNotification(arg)
                    return
                }

                else -> {
                    // 尝试从NotificationRecord中提取sbn
                    runCatching {
                        val sbn = XposedHelpers.getObjectField(
                            arg,
                            "sbn"
                        ) as? android.service.notification.StatusBarNotification
                        if (sbn != null) {
                            processNotification(sbn)
                            return
                        }
                    }
                }
            }
        }
    }

    /**
     * 处理StatusBarNotification，提取通知内容并分析
     */
    private fun processNotification(sbn: android.service.notification.StatusBarNotification) {
        val app = sbn.packageName
        val notification = sbn.notification ?: return
        val extras = notification.extras

        // 提取标题和内容
        val originalTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""

        // 按优先级提取内容：大文本 > 普通文本 > 子文本
        val originalText = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
            ?: "").toString()

        // 去重：避免重复处理相同通知
        val hash = MD5HashTable.md5("$app$originalTitle$originalText")
        if (hashTable.contains(hash)) {
            AppRuntime.manifest.d("Duplicate notification ignored: $app")
            return
        }
        hashTable.put(hash)

        AppRuntime.manifest.d("Notification: app=$app, title=$originalTitle, text=$originalText")

        // 异步处理通知
        CoroutineUtils.withIO {
            checkNotification(app, originalTitle, originalText)
        }
    }

    /**
     * 检查通知
     */
    private suspend fun checkNotification(
        pkg: String,
        title: String,
        text: String,
    ) {
        if (title.isEmpty() && text.isEmpty()) {
            return
        }
        val apps = runCatching {
            SettingAPI.get(Setting.LISTENER_APP_LIST, DefaultData.APP_FILTER).split(",")
                .toMutableList()
        }.getOrElse { mutableListOf() }
        if (!apps.contains(pkg)) {
            return
        }




        if (pkg == "com.android.mms") {
            val json = JsonObject().apply {
                addProperty("sender","")
                addProperty("body",text)
                addProperty("t",System.currentTimeMillis())
            }
            AnalysisUtils.analysisData(
                "com.android.phone",
                DataType.DATA,
                Gson().toJson(json),

            )
        }else{
            val json = JsonObject()
            json.addProperty("title", title)
            json.addProperty("text", text)
            json.addProperty("t",System.currentTimeMillis())


            d("NotificationHooker: $json")
            AnalysisUtils.analysisData(pkg, DataType.NOTICE, Gson().toJson(json))
        }


    }


}