package net.ankio.auto.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import net.ankio.auto.R
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.PrefManager

/**
 * 核心服务类，作为应用程序的主要后台服务
 * 负责管理和协调其他子服务的生命周期
 * 包括：服务器服务、OCR服务和悬浮窗服务
 */
class CoreService : LifecycleService() {


    /** 通知ID，用于前台服务通知 */
    private val NOTIF_ID = 1000

    /** 通知渠道ID，用于创建通知渠道 */
    private val CHANNEL_ID = "core_service"

    /**
     * 服务列表，根据工作模式动态决定启用的服务
     * Xposed模式下只启用悬浮窗服务
     * 其他模式下启用所有服务：服务器服务、OCR服务和悬浮窗服务
     */
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

    /**
     * 服务创建时调用
     * 初始化通知渠道并启动前台服务
     * 同时初始化所有子服务
     */
    override fun onCreate() {
        super.onCreate()
        Logger.i("onCreate invoked，PrefManager.workMode = ${PrefManager.workMode}")
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())

        services.forEach { service ->
            try {
                Logger.i("Initializing service: ${service.javaClass.simpleName}")
                service.onCreate(this)
            } catch (e: Exception) {
                Logger.e("Error initializing ${service.javaClass.simpleName}: ${e.message}", e)
            }
        }
    }

    /**
     * 创建通知渠道
     * 配置为低优先级通知，不显示角标，无声音和震动
     */
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

    /**
     * 构建前台服务通知
     * 创建一个低优先级、静默的通知
     * @return 配置好的通知对象
     */
    private fun buildNotification(): Notification {
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

    /**
     * 服务启动命令处理
     * 将启动命令传递给所有子服务
     * @param intent 启动服务的Intent
     * @param flags 启动标志
     * @param startId 启动ID
     * @return START_STICKY 表示服务被系统杀死后会尝试重新创建
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Logger.i("onStartCommand received - intent=$intent, flags=$flags, startId=$startId")
        services.forEach { service ->
            try {
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

    /**
     * 服务销毁时调用
     * 停止前台服务并销毁所有子服务
     */
    override fun onDestroy() {
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
        /**
         * 启动核心服务的静态方法
         * @param activity 启动服务的Activity上下文
         * @param intent 可选的额外Intent参数
         */
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