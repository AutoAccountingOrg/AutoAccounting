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

package net.ankio.auto.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import com.google.gson.JsonObject
import net.ankio.auto.App
import net.ankio.auto.BuildConfig
import net.ankio.auto.autoApp
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.http.api.JsAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.constant.DataType

class NotificationService : NotificationListenerService() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (PrefManager.workMode != WorkMode.Ocr) return
        //获取用户通知
        runCatching {
            val notification = sbn?.notification!!
            val app = sbn.packageName
            val title = runCatching {
                notification.extras.getString(Notification.EXTRA_TITLE) ?: ""
            }.getOrElse { "" }
            val text = runCatching {
                notification.extras.getString(Notification.EXTRA_BIG_TEXT)  // 首先尝试获取大文本
                    ?: notification.extras.getString(Notification.EXTRA_TEXT)  // 如果没有大文本，则获取普通文本
                    ?: ""  // 如果都没有，返回空字符串
            }.getOrElse { "" }

            checkNotification(app!!, title, text)
        }.onFailure {
            Logger.e("NotificationService: ${it.message}", it)
        }
    }

    /**
     * 监听断开
     */
    override fun onListenerDisconnected() {
        if (PrefManager.workMode != WorkMode.Ocr) return
        // 通知侦听器断开连接 - 请求重新绑定
        requestRebind(
            ComponentName(
                this,
                NotificationListenerService::class.java
            )
        )
    }

    private fun checkNotification(
        pkg: String,
        title: String,
        text: String,
    ) {
        val apps = PrefManager.appWhiteList

        if (title.isEmpty() && text.isEmpty()) {
            return
        }

        if (!apps.contains(pkg)) {
            return
        }


        if (pkg == "com.android.mms") {
            val json = JsonObject().apply {
                addProperty("sender", "")
                addProperty("body", text)
                addProperty("t", System.currentTimeMillis())
            }
            App.launch {
                val billResult =
                    JsAPI.analysis(DataType.DATA, Gson().toJson(json), "com.android.phone")
                Logger.d("识别结果：${billResult?.billInfoModel}")
            }
            // Analyze.start(DataType.DATA, Gson().toJson(json), "com.android.phone")
        } else {
            val json = JsonObject()
            json.addProperty("title", title)
            json.addProperty("text", text)
            json.addProperty("t", System.currentTimeMillis())
            App.launch {
                val billResult =
                    JsAPI.analysis(DataType.NOTICE, Gson().toJson(json), pkg)
                Logger.d("识别结果：${billResult?.billInfoModel}")
            }
            //  Analyze.start(DataType.NOTICE, Gson().toJson(json), pkg)
        }

    }

    companion object : IService {
        /**
         * 是否启用通知监听服务
         * @return
         */
        override fun hasPermission(): Boolean {
            val packageNames = NotificationManagerCompat.getEnabledListenerPackages(autoApp)
            return packageNames.contains(BuildConfig.APPLICATION_ID)
        }

        override fun startPermissionActivity(context: Context) {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }


    }
}