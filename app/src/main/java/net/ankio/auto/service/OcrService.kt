package net.ankio.auto.service

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import net.ankio.auto.service.api.ICoreService
import net.ankio.auto.service.api.IService
import net.ankio.auto.service.ocr.OcrProcessor
import net.ankio.auto.service.ocr.ScreenCapture
import net.ankio.auto.service.ocr.FlipDetector
import net.ankio.auto.shell.Shell
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.constant.DataType
import org.ezbook.server.intent.IntentType

/**
 * OCR服务类，用于实现屏幕文字识别功能
 * 主要功能：
 * 1. 监听设备翻转事件（从朝下翻转到朝上）
 * 2. 截取屏幕内容
 * 3. 进行OCR文字识别
 * 4. 显示识别动画界面
 */
class OcrService : ICoreService() {

    // OCR处理器
    private lateinit var ocrProcessor: OcrProcessor

    // 翻转检测器，当设备从朝下翻转到朝上时触发OCR
    private val detector by lazy {
        FlipDetector(
            coreService.getSystemService(Context.SENSOR_SERVICE) as SensorManager,
        ) {
            onFlip()
        }
    }

    private val shell = Shell()

    /**
     * 服务创建时的初始化
     * 检查必要权限并初始化相关组件
     */
    override fun onCreate(coreService: CoreService) {
        super.onCreate(coreService)

        // 统一检查所有权限和依赖
        if (!initializeOcrService(coreService)) {
            return
        }

        serverStarted = true
        Logger.d("OCR服务初始化成功，等待翻转触发")
    }

    /**
     * 初始化OCR服务的所有组件
     * @return true 如果初始化成功，false 如果失败
     */
    private fun initializeOcrService(coreService: CoreService): Boolean {
        /* // 检查权限
         if (!hasPermission()) {
             Logger.e("缺少 UsageStats 权限")
             return false
         }

         if (!ScreenCapture.isReady()) {
             Logger.e("缺少截屏权限")
             return false
         }
 */
        // 初始化OCR处理器
        ocrProcessor = OcrProcessor(coreService)

        // 启动翻转检测
        if (!detector.start()) {
            Logger.e("设备不支持重力/加速度传感器")
            return false
        }

        return true
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

        // 释放截图资源
        // ScreenCapture.release()

        // 确保悬浮窗被清理
        stopOcrView()
    }

    /* -------------------------------- 业务逻辑 ------------------------------- */

    private var ocrDoing = false

    /**
     * 触发振动反馈
     * 在OCR识别开始时提供触觉反馈
     */
    private fun triggerVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ 使用VibratorManager
                val vibratorManager =
                    coreService.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                // Android 12以下使用传统Vibrator
                @Suppress("DEPRECATION")
                coreService.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            // 检查设备是否支持振动
            if (!vibrator.hasVibrator()) {
                Logger.d("设备不支持振动功能")
                return
            }

            // 创建振动效果
            // Android 8.0+ 使用VibrationEffect
            val vibrationEffect =
                VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(vibrationEffect)

            Logger.d("振动反馈已触发")
        } catch (e: Exception) {
            Logger.e("振动反馈失败: ${e.message}")
        }
    }

    /**
     * 处理设备翻转事件
     * 当设备从朝下翻转到朝上时触发OCR识别
     */
    private fun onFlip() {
        Logger.d("检测到设备翻转事件（朝下→朝上）")
        triggerOcr()
    }

    /**
     * 触发OCR识别
     * 支持多种触发方式：设备翻转、Intent、磁贴等
     */
    private fun triggerOcr() {
        if (ocrDoing) {
            Logger.d("OCR正在处理中，跳过本次请求")
            return
        }

        // 验证前台应用
        val packageName = validateForegroundApp() ?: return

        Logger.d("检测到白名单应用 [$packageName]，开始OCR")
        ocrDoing = true

        // 执行OCR流程
        executeOcrFlow(packageName)
    }

    /**
     * 验证前台应用是否在白名单中
     * @return 有效的包名，如果无效则返回null
     */
    private fun validateForegroundApp(): String? {
        val pkg = getTopPackagePostL(coreService) ?: run {
            Logger.d("无法获取前台应用")
            return null
        }

        if (pkg !in PrefManager.appWhiteList) {
            Logger.d("前台应用 $pkg 不在监控白名单")
            return null
        }

        return pkg
    }

    /**
     * 执行OCR识别的完整流程
     */
    private fun executeOcrFlow(packageName: String) {
        // 触发振动反馈
        triggerVibration()


        App.launch {
            val startTime = System.currentTimeMillis()
            try {
                // 显示OCR界面
                startOcrView(coreService)

                // 执行截图和识别
                val ocrResult = performOcrCapture()

                // 处理识别结果
                if (ocrResult != null) {
                    send2JsEngine(ocrResult, packageName)
                }

                val totalTime = System.currentTimeMillis() - startTime
                Logger.d("OCR处理完成，总耗时: ${totalTime}ms")

            } catch (e: Exception) {
                Logger.e("OCR处理异常: ${e.message}")
            } finally {
                stopOcrView()
                ocrDoing = false
            }
        }

    }

    /**
     * 执行屏幕截图和OCR识别
     * @return 识别出的文本，如果失败则返回null
     */
    private suspend fun performOcrCapture(): String? {

        val captureStartTime = System.currentTimeMillis()
        // 优先尝试使用 Shizuku 走系统截图通道获取 Bitmap；失败则回退 MediaProjection
        val image = shell.exec("screencap -p " + coreService.externalCacheDir + "/screen.png")
        Logger.d(image)
        /*   if (image == null){
               Logger.d("截图失败")
           }
           val captureTime = System.currentTimeMillis() - captureStartTime
           Logger.d("截图耗时: ${captureTime}ms")

           if (image == null) {
               Logger.e("截图失败")
               return null
           }

           val text = ocrProcessor.recognize(image)
           return if (text.isNotBlank()) text else null*/
        return null
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
            try {
                // 使用 removeViewImmediate 避免异步移除导致的 Surface 残留/队列死亡
                if (view.isAttachedToWindow) {
                    windowManager?.removeViewImmediate(view)
                }
            } catch (e: Exception) {
                Logger.w("移除OCR悬浮窗失败: ${e.message}")
            } finally {
                floatView = null
                windowManager = null
            }
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
        Logger.d("识别结果：${billResult.billInfoModel}")
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
                    !it.packageName.startsWith("com.android") &&
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




