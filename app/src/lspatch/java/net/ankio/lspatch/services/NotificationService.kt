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

package net.ankio.lspatch.services

import android.app.Notification
import android.content.ComponentName
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import com.google.gson.JsonObject
import net.ankio.auto.App
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.exceptions.ServiceCheckException
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.lspatch.js.Analyze
import org.ezbook.server.constant.DataType
import org.ezbook.server.constant.Setting


class NotificationService : NotificationListenerService() {
    private var apps = mutableListOf<String>()
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        //获取用户通知
        runCatching {
           val notification = sbn?.notification
           val app = sbn?.packageName
           val title = notification?.extras?.getString(Notification.EXTRA_TITLE)?:""
           val text = notification?.extras?.getString(Notification.EXTRA_TEXT)?:""
           //处理通知
           apps  = runCatching { ConfigUtils.getString(Setting.LISTENER_APP_LIST, "").split(",").toMutableList()
           }.getOrElse { mutableListOf() }

           checkNotification(app!!, title, text)
       }.onFailure {
           Logger.e("NotificationService: ${it.message}", it)
       }
    }

    /**
     * 监听断开
     */
    override fun onListenerDisconnected() {
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
        if (title.isEmpty() && text.isEmpty()) {
            return
        }

        if (!apps.contains(pkg)) {
            return
        }


        val json = JsonObject()
        json.addProperty("title", title)
        json.addProperty("text", text)
        json.addProperty("t",System.currentTimeMillis())

        Logger.i("NotificationHooker: $json")

        Analyze.start(DataType.NOTICE, Gson().toJson(json), pkg)
    }

    companion object {
        /**
         * 是否启用通知监听服务
         * @return
         */
        private fun isNLServiceEnabled(): Boolean {
            val packageNames = NotificationManagerCompat.getEnabledListenerPackages(App.app)
            return packageNames.contains(BuildConfig.APPLICATION_ID)
        }

        fun checkPermission() {
            //检查是否有权限
            if (!isNLServiceEnabled()) {
                throw ServiceCheckException(
                    App.app.getString(R.string.permission_not_granted_notification),
                    App.app.getString(R.string.permission_not_granted_notification_desc),
                    App.app.getString(R.string.permission_not_granted_notification_btn)
                    ,
                    action = {
                    //请求权限
                    it.startActivity(
                        Intent(
                            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                })
            }
        }
    }
}