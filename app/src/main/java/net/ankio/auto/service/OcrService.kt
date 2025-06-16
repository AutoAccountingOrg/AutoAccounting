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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.autoApp
import net.ankio.auto.databinding.OcrViewBinding
import net.ankio.auto.service.utils.OcrProcessor
import net.ankio.auto.service.utils.ProjectionGateway
import net.ankio.auto.service.utils.ScreenShotHelper
import net.ankio.auto.service.utils.ShakeDetector
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.throttle

class OcrService : ICoreService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var shotHelper: ScreenShotHelper
    // private lateinit var allowedPkgs: Set<String>   // 配置白名单

    private val detector by lazy {
        val t = throttle { onShake() }
        ShakeDetector(
            coreService.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        ) {
            t()
        }
    }

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

        // MediaProjection 已在前台服务里初始化后注入
        shotHelper = ScreenShotHelper(coreService, ProjectionGateway.get(coreService))

        // 允许在支付宝和微信中摇一摇触发
        //  allowedPkgs = setOf("com.eg.android.AlipayGphone", "com.tencent.mm")

        if (!detector.start()) {
            Logger.e("设备不支持加速度传感器")
            shotHelper.release()
            return
        }
        Logger.d("摇一摇监听中")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) {
        //TODO("Not yet implemented")
    }

    override fun onDestroy() {
        detector.stop()
        shotHelper.release()
        ProjectionGateway.release()
        scope.cancel()
    }

    /* -------------------------------- 业务逻辑 ------------------------------- */

    private fun onShake() {
        val pkg = getTopPackagePostL(coreService) ?: return
        Logger.d("检测到白名单应用 [$pkg]，开始截屏 OCR")
        scope.launch {
            withContext(Dispatchers.Main) {
                startOcrView(coreService)
            }
            val image = shotHelper.capture()
            if (image != null) {
                val text = OcrProcessor.recognize(image)
                if (text.isNotBlank()) send2JsEngine(text, pkg)
            }
            delay(30_000)
            withContext(Dispatchers.Main) {
                stopOcrView()
            }
            Logger.d("处理结束")
            //TODO 屏幕识别结束

        }

        //屏幕识别动画

    }

    private var floatView: View? = null
    private var windowManager: WindowManager? = null

    private fun startOcrView(context: Context) {
        // 已经显示则不再重复
        if (floatView != null) return

        // 检查并申请权限
        if (!Settings.canDrawOverlays(context)) {
            Logger.e("不支持显示悬浮窗")
            return
        }

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.CENTER

        // 你的自定义OCR悬浮View
        floatView = OcrViewBinding.inflate(LayoutInflater.from(context)).root

        windowManager?.addView(floatView, layoutParams)
    }

    private fun stopOcrView() {
        floatView?.let { view ->
            windowManager?.removeView(view)
            floatView = null
            windowManager = null
        }
    }


    private suspend fun send2JsEngine(text: String, app: String) {
        //TODO 使用js引擎识别
        Logger.d("app=$app, text=$text")
    }

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
        override fun hasPermission(): Boolean {
            val ctx = autoApp
            val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), ctx.packageName
            )
            return mode == AppOpsManager.MODE_ALLOWED
        }

        override fun startPermissionActivity(context: Context) {
            context.startActivity(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

    }
}




