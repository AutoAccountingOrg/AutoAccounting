package net.ankio.auto.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import androidx.core.app.NotificationCompat
import net.ankio.auto.R
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.PrefManager

class CoreService : Service() {

    override fun onBind(intent: Intent?) = null

    private val NOTIF_ID = 1000
    private val CHANNEL_ID = "core_service"

    // 服务列表，根据自动记账的类型决定服务。
    private val services = if (PrefManager.workMode === WorkMode.Xposed) {
        listOf<ICoreService>(
            FloatingService()
        )
    } else {
        listOf(
            ServerService(),  // 自动记账的服务模块
            OcrService(),     // OCR服务
            FloatingService() // 悬浮窗服务
        )
    }

    override fun onCreate() {
        super.onCreate()
        Logger.i("onCreate invoked，PrefManager.workMode = ${PrefManager.workMode}")
        createNotificationChannel()
        Logger.i("Notification channel created")
        startForeground(NOTIF_ID, buildNotification())
        Logger.i("Foreground started with notification ID $NOTIF_ID")

        services.forEach { service ->
            try {
                Logger.i("Initializing service: ${service.javaClass.simpleName}")
                service.onCreate(this)
            } catch (e: Exception) {
                Logger.e("Error initializing ${service.javaClass.simpleName}: ${e.message}", e)
            }
        }
    }

    private fun createNotificationChannel() {
        Logger.d("Creating notification channel: $CHANNEL_ID")
        val nm = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_LOW  // 低打扰
        ).apply {
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
            lightColor = 0
            lockscreenVisibility = Notification.VISIBILITY_SECRET
            description = getString(R.string.service_channel_description)
        }
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        Logger.d("Building notification for foreground service")
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon_auto)
            .setContentTitle(getString(R.string.service_notification_title))
            .setOngoing(true)
            .setShowWhen(false)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.i("onStartCommand received - intent=$intent, flags=$flags, startId=$startId")
        services.forEach { service ->
            try {
                Logger.i("Service onStartCommand: ${service.javaClass.simpleName}")
                service.onStartCommand(intent, flags, startId)
            } catch (e: Exception) {
                Logger.e(
                    "Error in onStartCommand for ${service.javaClass.simpleName}: ${e.message}",
                    e
                )
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Logger.i("onDestroy invoked")
        stopForeground(STOP_FOREGROUND_REMOVE)
        services.forEach { service ->
            try {
                Logger.i("Destroying service: ${service.javaClass.simpleName}")
                service.onDestroy()
            } catch (e: Exception) {
                Logger.e("Error destroying ${service.javaClass.simpleName}: ${e.message}", e)
            }
        }
        super.onDestroy()
    }

    companion object {
        fun start(activity: Activity, intent: Intent? = null) {
            val targetIntent = Intent(activity, CoreService::class.java).apply {
                intent?.extras?.let(::putExtras)
            }
            Logger.i("Starting CoreService via companion.start")
            try {
                activity.startForegroundService(targetIntent)
            } catch (e: Exception) {
                Logger.e("Failed to start service: ${e.message}", e)
            }
        }
    }
}