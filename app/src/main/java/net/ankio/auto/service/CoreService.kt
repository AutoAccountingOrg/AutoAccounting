package net.ankio.auto.service

import android.app.Activity
import android.app.ActivityManager
import android.app.Notification
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import net.ankio.auto.R
import net.ankio.auto.constant.WorkMode
import org.ezbook.server.intent.IntentType
import net.ankio.auto.service.api.ICoreService
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.PrefManager

/**
 * 核心服务类，作为应用程序的主要后台服务
 * 负责管理和协调其他子服务的生命周期
 * 包括：服务器服务、OCR服务和悬浮窗服务
 */
class CoreService : LifecycleService() {

    /** 通知ID，用于前台服务通知 */
    private val notificationId = 1000

    /** 通知渠道ID，用于创建通知渠道 */
    private val channelId = "core_service"

    /** 服务列表，在onCreate中根据工作模式动态初始化 */
    private lateinit var services: List<ICoreService>

    /** 通知渠道是否已创建的标志 */
    private var isNotificationChannelCreated = false

    /**
     * 服务创建时调用
     * 初始化通知渠道并启动前台服务
     * 同时初始化所有子服务
     */
    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        Logger.i("CoreService created, workMode=${PrefManager.workMode}")
        // 根据工作模式初始化服务列表
        initializeServices()
        // 初始化所有子服务
        initializeChildServices()
    }

    private fun startForegroundNotification() {
        createNotificationChannel()
        val notification = buildNotification()
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ 要求前台服务指定类型掩码，与清单中 foregroundServiceType="specialUse" 匹配
                startForeground(
                    notificationId,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(notificationId, notification)
            }
        } catch (e: SecurityException) {
            Logger.e("Foreground service (specialUse) start failed: ${e.message}", e)
        }
    }

    /**
     * 根据工作模式初始化服务列表
     * Xposed模式下只启用悬浮窗服务
     * 其他模式下启用所有服务：服务器服务、OCR服务和悬浮窗服务
     */
    private fun initializeServices() {
        services = if (WorkMode.isXposed()) {
            listOf(
                OcrService(),
                OverlayService()
            )
        } else {
            listOf(
                BackgroundHttpService(),  // 自动记账的服务模块
                OcrService(),     // OCR服务
                OverlayService() // 悬浮窗服务
            )
        }
    }

    /**
     * 初始化所有子服务
     * 为每个服务提供错误处理和日志记录
     */
    private fun initializeChildServices() {
        var successCount = 0
        var failureCount = 0
        
        services.forEach { service ->
            try {
                service.onCreate(this)
                successCount++
            } catch (e: Exception) {
                Logger.e("Service init failed: ${service.javaClass.simpleName}, ${e.message}", e)
                failureCount++
            }
        }
        Logger.d("Child services initialized: success=$successCount, failure=$failureCount")
    }

    /**
     * 创建通知渠道
     * 配置为低优先级通知，不显示角标，无声音和震动
     * 避免重复创建通知渠道
     */
    private fun createNotificationChannel() {
        if (isNotificationChannelCreated) {
            return
        }

        val nm = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            channelId,
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
        isNotificationChannelCreated = true
    }

    /**
     * 构建前台服务通知
     * 创建一个低优先级、静默的通知，点击触发手动 OCR（通知不清除）
     */
    private fun buildNotification(): Notification {
        val ocrIntent = Intent(this, CoreService::class.java).apply {
            putExtra("intentType", IntentType.OCR.name)
            putExtra("manual", true)
        }
        val pendingIntent = PendingIntent.getService(
            this, 0, ocrIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.icon_auto)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentIntent(pendingIntent)
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
     * @return 启动返回标志：Xposed 模式返回 START_NOT_STICKY，其他模式返回 START_STICKY
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForegroundNotification()
        services.forEach { service ->
            try {
                service.onStartCommand(intent, flags, startId)
            } catch (e: Exception) {
                Logger.e(
                    "Service onStartCommand failed: ${service.javaClass.simpleName}, ${e.message}",
                    e
                )
            }
        }
        return if (WorkMode.isXposed()) START_NOT_STICKY else START_STICKY
    }

    /**
     * 服务销毁时调用
     * 停止前台服务并销毁所有子服务
     */
    override fun onDestroy() {
        Logger.i("CoreService destroying, cleaning up child services")

        // 停止前台服务
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Logger.e("Stop foreground failed: ${e.message}", e)
        }

        // 销毁所有子服务
        if (::services.isInitialized) {
            var successCount = 0
            var failureCount = 0

            services.forEach { service ->
                try {
                    Logger.d("Destroying service: ${service.javaClass.simpleName}")
                    service.onDestroy()
                    successCount++
                } catch (e: Exception) {
                    Logger.e(
                        "Service destroy failed: ${service.javaClass.simpleName}, ${e.message}",
                        e
                    )
                    failureCount++
                }
            }

            Logger.i("CoreService destroyed: success=$successCount, failure=$failureCount")
        }
        
        super.onDestroy()
    }

    companion object {
        /**
         * 检查CoreService是否正在运行
         */
        fun isRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
                if (CoreService::class.java.name == service.service.className) {
                    return true
                }
            }
            return false
        }

        /**
         * 停止核心服务
         */
        fun stop(context: Context): Boolean {
            return try {
                val intent = Intent(context, CoreService::class.java)
                context.stopService(intent)
                Logger.i("CoreService stopped")
                true
            } catch (e: Exception) {
                Logger.e("Stop service failed: ${e.message}", e)
                false
            }
        }

        /**
         * 启动核心服务的静态方法
         * @param activity 启动服务的Activity上下文
         * @param intent 可选的额外Intent参数
         * @return 是否成功启动服务
         */
        fun start(activity: Activity, intent: Intent? = null): Boolean {
            return try {
                val targetIntent = Intent(activity, CoreService::class.java).apply {
                    intent?.extras?.let(::putExtras)
                }
                activity.startForegroundService(targetIntent)
                true

            } catch (e: Exception) {
                Logger.e("Start service failed: ${e.message}", e)
                false
            }
        }

        /**
         * 重启核心服务
         * 如果服务正在运行则先停止，然后启动。
         * 等待与启动在后台线程执行，避免阻塞主线程导致页面切换卡顿。
         */
        fun restart(activity: Activity, intent: Intent? = null): Boolean {
            return try {
                if (isRunning(activity)) {
                    Logger.i("CoreService running, stopping first")
                    stop(activity)
                }

                Logger.i("Starting CoreService")
                start(activity, intent)

                true

            } catch (e: Exception) {
                Logger.e("Restart service failed: ${e.message}", e)
                false
            }
        }
    }
}