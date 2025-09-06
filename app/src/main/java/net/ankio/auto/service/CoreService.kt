package net.ankio.auto.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import net.ankio.auto.R
import net.ankio.auto.constant.WorkMode
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
        Logger.i("服务创建，工作模式 = ${PrefManager.workMode}")
        // 根据工作模式初始化服务列表
        initializeServices()
        // 初始化所有子服务
        initializeChildServices()
    }

    private fun startForegroundNotification() {
        createNotificationChannel()
        // Android 14+ 要求前台服务在启动后尽快调用带类型的 startForeground
        // 这里根据清单声明的类型 dataSync | mediaProjection 传入匹配的类型掩码，避免系统认为类型不明确而超时
        val notification = buildNotification()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val fgsType = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            startForeground(notificationId, notification, fgsType)
        } else {
            startForeground(notificationId, notification)
        }
    }

    /**
     * 根据工作模式初始化服务列表
     * Xposed模式下只启用悬浮窗服务
     * 其他模式下启用所有服务：服务器服务、OCR服务和悬浮窗服务
     */
    private fun initializeServices() {
        services = if (PrefManager.workMode === WorkMode.Xposed) {
            listOf<ICoreService>(
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
                Logger.e("初始化服务 ${service.javaClass.simpleName} 失败: ${e.message}", e)
                failureCount++
            }
        }
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
     * 创建一个低优先级、静默的通知
     * @return 配置好的通知对象
     */
    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
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
        startForegroundNotification()
        Logger.i("收到启动命令 - intent=$intent, flags=$flags, startId=$startId")
        services.forEach { service ->
            try {
                service.onStartCommand(intent, flags, startId)
            } catch (e: Exception) {
                Logger.e(
                    "服务 ${service.javaClass.simpleName} 处理启动命令失败: ${e.message}",
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
        Logger.i("服务销毁，正在清理子服务")

        // 停止前台服务
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Logger.e("停止前台服务失败: ${e.message}", e)
        }

        // 销毁所有子服务
        if (::services.isInitialized) {
            var successCount = 0
            var failureCount = 0

            services.forEach { service ->
                try {
                    Logger.i("正在销毁服务: ${service.javaClass.simpleName}")
                    service.onDestroy()
                    successCount++
                } catch (e: Exception) {
                    Logger.e("销毁服务 ${service.javaClass.simpleName} 失败: ${e.message}", e)
                    failureCount++
                }
            }

            Logger.i("服务销毁完成: 成功 $successCount 个，失败 $failureCount 个")
        } else {
            Logger.w("服务未初始化，跳过销毁")
        }
        
        super.onDestroy()
    }

    companion object {
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
                Logger.e("启动服务时未知异常: ${e.message}", e)
                false
            }
        }
    }
}