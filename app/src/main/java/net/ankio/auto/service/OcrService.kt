package net.ankio.auto.service

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.SensorManager
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import kotlinx.coroutines.delay
import net.ankio.auto.App
import net.ankio.auto.BuildConfig
import net.ankio.auto.autoApp
import net.ankio.auto.databinding.OcrViewBinding
import net.ankio.auto.http.api.JsAPI
import net.ankio.auto.service.ocr.OcrProcessor
import net.ankio.auto.service.ocr.ProjectionGateway
import net.ankio.auto.service.ocr.ScreenShotHelper
import net.ankio.auto.service.ocr.ShakeDetector
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.constant.DataType
import org.ezbook.server.intent.IntentType

/**
 * OCR服务类，用于实现屏幕文字识别功能
 * 主要功能：
 * 1. 监听设备摇动事件
 * 2. 截取屏幕内容
 * 3. 进行OCR文字识别
 * 4. 显示识别动画界面
 */
class OcrService : ICoreService() {

    // 屏幕截图助手
    private lateinit var shotHelper: ScreenShotHelper

    private lateinit var ocrProcessor: OcrProcessor

    // 摇动检测器，使用节流函数防止频繁触发
    private val detector by lazy {
        ShakeDetector(
            coreService.getSystemService(Context.SENSOR_SERVICE) as SensorManager,
        ) {
            onShake()
        }
    }

    /**
     * 服务创建时的初始化
     * 检查必要权限并初始化相关组件
     */
    override fun onCreate(coreService: CoreService) {
        super.onCreate(coreService)

        if (!hasPermission()) {
            Logger.e("缺少 UsageStats 权限")
            return
        }

        if (!ProjectionGateway.isReady()) {
            Logger.e("缺少 截屏 权限")
            return
        }

        // 初始化屏幕截图助手
        shotHelper = ScreenShotHelper(coreService, ProjectionGateway.get(coreService))

        ocrProcessor = OcrProcessor(coreService)

        serverStarted = true

        // 启动摇动检测
        if (!detector.start()) {
            Logger.e("设备不支持加速度传感器")
            shotHelper.release()
            return
        }
        Logger.d("摇一摇监听中")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) {

        if (intent?.getStringExtra("intentType") == IntentType.OCR.name) {
            Logger.d("收到Intent启动OCR请求")
            //延迟1秒启动，等activity退出
            App.launch {
                delay(1000)
                triggerOcr()
            }
        }


    }

    /**
     * 服务销毁时的清理工作
     */
    override fun onDestroy() {
        detector.stop()
        shotHelper.release()
        ProjectionGateway.release()
    }

    /* -------------------------------- 业务逻辑 ------------------------------- */

    private var ocrDoing = false

    /**
     * 处理摇动事件
     */
    private fun onShake() {
        Logger.d("检测到摇动事件")
        triggerOcr()
    }

    /**
     * 触发OCR识别
     * 支持多种触发方式：摇动、Intent、磁贴等
     * 1. 获取当前前台应用
     * 2. 显示OCR动画界面
     * 3. 截取屏幕并进行OCR识别
     * 4. 将识别结果发送给JS引擎处理
     */
    private fun triggerOcr() {
        if (ocrDoing) {
            Logger.d("OCR正在处理中，跳过本次请求")
            return
        }

        val pkg = getTopPackagePostL(coreService) ?: run {
            Logger.d("无法获取前台应用")
            return
        }

        if (pkg !in PrefManager.appWhiteList) {
            Logger.d("前台应用 $pkg 不在监控白名单，忽略。")
            return
        }

        Logger.d("检测到白名单应用 [$pkg]，开始截屏 OCR")
        ocrDoing = true

        // 使用全局协程管理器
        App.launch {
            try {
                // 在主线程显示OCR动画
                startOcrView(coreService)

                // 在IO线程进行截图和OCR识别
                val image = shotHelper.capture()
                if (image != null) {
                    val text = ocrProcessor.recognize(image)
                    if (text.isNotBlank()) {
                        send2JsEngine(text, pkg)
                    }
                }

                stopOcrView()

                Logger.d("OCR处理完成")
            } catch (e: Exception) {
                Logger.e("OCR处理异常: ${e.message}")
                // 确保在异常情况下也能关闭动画
                stopOcrView()
            } finally {
                ocrDoing = false
            }
        }
    }

    // 悬浮窗相关变量
    private var floatView: View? = null
    private var windowManager: WindowManager? = null

    /**
     * 显示OCR识别动画界面
     * 创建一个全屏悬浮窗来显示识别动画
     */
    private fun startOcrView(context: Context) {
        // 已经显示则不再重复
        if (floatView != null) return

        // 检查悬浮窗权限
        if (!Settings.canDrawOverlays(context)) {
            Logger.e("不支持显示悬浮窗")
            return
        }

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // 配置悬浮窗参数
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.CENTER

        // 创建并显示悬浮窗
        floatView = OcrViewBinding.inflate(LayoutInflater.from(context)).root
        windowManager?.addView(floatView, layoutParams)
    }

    /**
     * 关闭OCR识别动画界面
     */
    private fun stopOcrView() {
        floatView?.let { view ->
            windowManager?.removeView(view)
            floatView = null
            windowManager = null
        }
    }

    /**
     * 将OCR识别结果发送给JS引擎处理
     * @param text OCR识别的文本内容
     * @param app 当前应用包名
     */
    private suspend fun send2JsEngine(text: String, app: String) {
        Logger.d("app=$app, text=$text")
        val billResult = JsAPI.analysis(DataType.DATA, text, app) ?: return
        // 发起记账intent
        // TODO 弹出处理窗口
    }

    /**
     * 获取当前前台应用包名
     * @return 返回最近使用的应用包名
     */
    private fun getTopPackagePostL(ctx: Context): String? {
        try {
            val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            val begin = end - 60_000  // 最近1分钟

            val list = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, begin, end)
                .filter {
                    !it.packageName.startsWith("com.android")
                    !it.packageName.startsWith(BuildConfig.APPLICATION_ID)
                            && it.lastTimeUsed > 0
                            && it.totalTimeInForeground > 0
                }
            if (list.isEmpty()) return null




            list.forEach { usageStats ->
                Logger.d("包名: ${usageStats.packageName}, 最后使用时间: ${usageStats.lastTimeUsed}, 前台时间: ${usageStats.totalTimeInForeground}")
            }

            val recent = list.maxByOrNull { it.lastTimeUsed }

            return recent?.packageName
        } catch (e: Exception) {
            Logger.e("获取前台应用失败: ${e.message}")
            return null
        }
    }

    companion object : IService {
        /** OCR启动Action常量 */
        const val ACTION_START_OCR = "net.ankio.auto.action.START_OCR"

        /** 服务启动状态 */
        var serverStarted = false
        
        /**
         * 检查是否有使用情况访问权限
         */
        override fun hasPermission(): Boolean {
            val ctx = autoApp
            val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), ctx.packageName
            )
            return mode == AppOpsManager.MODE_ALLOWED
        }

        /**
         * 启动权限设置页面
         */
        override fun startPermissionActivity(context: Context) {
            context.startActivity(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

        /**
         * 通过Intent启动OCR识别
         * @param context 上下文
         */
        fun startOcrByIntent(context: Context) {
            val intent = Intent(context, CoreService::class.java).apply {
                action = ACTION_START_OCR
            }
            context.startService(intent)
        }
    }
}




