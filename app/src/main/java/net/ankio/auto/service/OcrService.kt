package net.ankio.auto.service

import android.app.AppOpsManager
import android.app.KeyguardManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.SensorManager
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.autoApp
import net.ankio.auto.databinding.OcrViewBinding
import net.ankio.auto.http.api.JsAPI
import net.ankio.auto.service.utils.OcrProcessor
import net.ankio.auto.service.utils.ProjectionGateway
import net.ankio.auto.service.utils.ScreenShotHelper
import net.ankio.auto.service.utils.ShakeDetector
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.Throttle
import org.ezbook.server.constant.DataType

/**
 * OCR服务类，用于实现屏幕文字识别功能
 * 主要功能：
 * 1. 监听设备摇动事件
 * 2. 截取屏幕内容
 * 3. 进行OCR文字识别
 * 4. 显示识别动画界面
 */
class OcrService : ICoreService() {

    // 协程作用域，用于处理异步任务
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 屏幕截图助手
    private lateinit var shotHelper: ScreenShotHelper
    // private lateinit var allowedPkgs: Set<String>   // 配置白名单

    private lateinit var ocrProcessor: OcrProcessor

    // 摇动检测器，使用节流函数防止频繁触发
    private val detector by lazy {
        //val throttle = Throttle.asFunction(2000) { onShake() }
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

        // 启动摇动检测
        if (!detector.start()) {
            Logger.e("设备不支持加速度传感器")
            shotHelper.release()
            return
        }
        Logger.d("摇一摇监听中")

        ocrProcessor = OcrProcessor(coreService)

        serverStarted = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) {

    }

    /**
     * 服务销毁时的清理工作
     */
    override fun onDestroy() {
        detector.stop()
        shotHelper.release()
        ProjectionGateway.release()
        scope.cancel()
    }

    /* -------------------------------- 业务逻辑 ------------------------------- */

    private var ocrDoing = false

    /**
     * 处理摇动事件
     * 1. 获取当前前台应用
     * 2. 显示OCR动画界面
     * 3. 截取屏幕并进行OCR识别
     * 4. 将识别结果发送给JS引擎处理
     */
    private fun onShake() {
        Logger.d("onShake")
        if (ocrDoing) {
            Logger.d("ocrDoing skip")
            return
        }
        val pkg = getTopPackagePostL(coreService) ?: return
        if (pkg !in PrefManager.appWhiteList) {
            Logger.d("前台应用 $pkg 不在监控白名单，忽略。")
            return
        }
        Logger.d("检测到白名单应用 [$pkg]，开始截屏 OCR")
        ocrDoing = true
        scope.launch {
            withContext(Dispatchers.Main) {
                startOcrView(coreService)
            }
            val image = shotHelper.capture()
            if (image != null) {
                val text = ocrProcessor.recognize(image)
                if (text.isNotBlank()) send2JsEngine(text, pkg)
            }
            withContext(Dispatchers.Main) {
                stopOcrView()
            }
            Logger.d("处理结束")

            ocrDoing = false
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
     * @return 返回最近10秒内最活跃的应用包名
     */
    private fun getTopPackagePostL(ctx: Context): String? {
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val begin = end - 10_000                    // 最近 10 秒窗口
        val list = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, begin, end)
        if (list.isNullOrEmpty()) return null
        val recent = list.maxByOrNull { it.lastTimeUsed } ?: return null
        return recent.packageName
    }

    companion object : IService {
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

        public var serverStarted = false
    }
}




