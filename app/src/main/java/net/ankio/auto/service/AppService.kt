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
import net.ankio.auto.intent.IntentType
import net.ankio.auto.xposed.core.api.HookerManifest
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
        startForeground(1, createNotification())
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
        Server.packageName = App.app.packageName
        Server.versionName = BuildConfig.VERSION_NAME
        CommonHooker.init()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.getStringExtra("type") != IntentType.FloatingIntent.name) {
            return START_STICKY
        }
        return floatingWindowService.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
        floatingWindowService.onDestroy()
    }

    private fun createNotification(): Notification {
        val channelId = "foreground_service_channel"

        val channel = NotificationChannel(
            channelId, applicationContext.getString(R.string.app_name),
            NotificationManager.IMPORTANCE_MIN // 设为最低级别，避免打扰
        ).apply {
            setShowBadge(false) // 禁用桌面角标
            enableLights(false)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round) // 透明图标，最小化视觉影响
            .setSilent(true) // 不触发声音或震动
            .setOngoing(true) // 防止用户误删
            .setPriority(NotificationCompat.PRIORITY_MIN) // 设置最低优先级
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE) // 防止过度展示
            .build()
    }

}