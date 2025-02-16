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
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import net.ankio.auto.App
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.intent.FloatingIntent
import net.ankio.auto.intent.IntentType
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.hooks.auto.AutoHooker
import net.ankio.auto.xposed.hooks.common.CommonHooker
import org.ezbook.server.Server

class AppService : Service() {
    private val floatingWindowService = FloatingWindowService(this)
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        initServer()
        floatingWindowService.onCreate()
    }

    private fun initServer() {
        if (BuildConfig.FLAVOR != "lspatch") {
            return
        }
        AppRuntime.manifest = AutoHooker()
        AppRuntime.modulePath =
            packageManager.getApplicationInfo(BuildConfig.APPLICATION_ID, 0).sourceDir
        AppRuntime.classLoader = classLoader
        AppRuntime.application = App.app
        Server.packageName = BuildConfig.APPLICATION_ID
        Server.versionName = BuildConfig.VERSION_NAME
        Server.debug = BuildConfig.DEBUG
        CommonHooker.init()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 创建通知通道
        createNotificationChannel()
        // 启动前台服务
        startForeground(1, createNotification())
        if (intent == null || intent.getStringExtra("intentType") != IntentType.FloatingIntent.name) {
            return START_STICKY
        }
        val floatingIntent = FloatingIntent.parse(intent)
        return floatingWindowService.onStartCommand(floatingIntent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
        floatingWindowService.onDestroy()
    }
    val channelId = "foreground_service_channel"
    private fun createNotificationChannel() {

        val channel = NotificationChannel(
            channelId,
            applicationContext.getString(R.string.app_name),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
            description = getString(R.string.service_channel_description)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.icon_auto)
            .setSilent(true)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentTitle(getString(R.string.service_notification_title))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

}