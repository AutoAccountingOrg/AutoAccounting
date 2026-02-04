package net.ankio.auto.service

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.BuildConfig
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.http.api.JsAPI
import net.ankio.auto.service.api.ICoreService
import net.ankio.auto.service.api.IService
import net.ankio.auto.service.ocr.FlipDetector
import net.ankio.auto.service.ocr.OcrViews
import net.ankio.auto.service.overlay.SaveProgressView
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.utils.DisplayUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.PrefManager
import net.ankio.ocr.OcrProcessor
import net.ankio.shell.Shell
import org.ezbook.server.constant.DataType
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.intent.IntentType
import org.ezbook.server.tools.runCatchingExceptCancel
import java.io.File
import kotlin.math.max

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
    private var saveProgressView: SaveProgressView? = null

    // 翻转检测器，当设备从朝下翻转到朝上时触发OCR
    private val detector by lazy {
        FlipDetector(coreService.getSystemService(Context.SENSOR_SERVICE) as SensorManager) {
            coreService.lifecycleScope.launch {
                // 传感器触发：非手动
                triggerOcr(false)
            }
        }
    }


    private val shell = Shell(BuildConfig.APPLICATION_ID)

    private val ocrView = OcrViews()

    init {

    }

    /**
     * 服务创建时的初始化
     * 检查必要权限并初始化相关组件
     */
    override fun onCreate(coreService: CoreService) {
        super.onCreate(coreService)

        ocrProcessor = OcrProcessor().debug(PrefManager.debugMode).attach(coreService)
            .log { string, type -> Logger.log(LogLevel.fromAndroidLevel(type), string) }

        // 只在非Xposed模式下启用翻转检测，且需要配置项开启
        if (WorkMode.isOcr() && PrefManager.ocrFlipTrigger) {
            // 启动翻转检测
            if (!detector.start()) {
                Logger.e("设备不支持重力/加速度传感器")
            } else {
                Logger.d("翻转检测已启动（非Xposed模式）")
            }
        }

        serverStarted = true
        Logger.d("OCR服务初始化成功，等待触发")

        if (WorkMode.isOcrOrLSPatch()) {
            saveProgressView = SaveProgressView()
            saveProgressView?.show(this)
            Logger.d("Ocr 或 LSPatch模式，使用1px悬浮窗保活")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) {

        if (intent?.getStringExtra("intentType") == IntentType.OCR.name) {
            Logger.d("收到Intent启动OCR请求")
            //延迟1秒启动，等activity退出
            coreService.lifecycleScope.launch {
                val manual = intent.getBooleanExtra("manual", false)
                triggerOcr(manual)
            }
        }

    }

    /**
     * 服务销毁时的清理工作
     */
    override fun onDestroy() {

        detector.stop()

        shell.close()
        // 释放 OCR 引擎资源，确保跟随服务生命周期关闭。
        ocrProcessor.close()
        // 确保悬浮窗被清理
        ocrView.stopOcrView()

        saveProgressView?.destroy()
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
     * 触发OCR识别
     * 支持多种触发方式：设备翻转、Intent、磁贴等
     */
    private suspend fun triggerOcr(manual: Boolean) {
        if (ocrDoing) {
            Logger.d("OCR正在处理中，跳过本次请求")
            return
        }

        // 先进入忙碌状态并触发振动反馈，再进行后续检测
        ocrDoing = true
        triggerVibration()

        // 解析前台应用包名：
        // - 手动触发：不走白名单，只读取当前前台应用（获取失败则用应用自身包名占位）
        // - 自动触发：严格校验白名单
        val packageName = if (manual) {
            getTopPackagePostL() ?: run {
                ocrDoing = false
                return
            }
        } else {
            validateForegroundApp() ?: run {
                ocrDoing = false
                return
            }
        }

        Logger.d("检测到白名单应用 [$packageName]，开始OCR")

        // 执行OCR流程
        executeOcrFlow(packageName, manual)
    }

    /**
     * 验证前台应用是否在白名单中
     * @return 有效的包名，如果无效则返回null
     */
    private suspend fun validateForegroundApp(): String? {
        val pkg = getTopPackagePostL() ?: run {
            Logger.w("无法获取前台应用")
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
    private fun executeOcrFlow(packageName: String, manual: Boolean) {

        coreService.lifecycleScope.launch {
            val startTime = System.currentTimeMillis()
            try {
                Logger.d("开始OCR流程")
                // 显示OCR界面
                ocrView.startOcrView(coreService)


                // 执行截图和识别

                withContext(Dispatchers.IO) {
                    val ocrResult = performOcrCapture()
                    // 处理识别结果
                    if (ocrResult != null) {
                        if (PrefManager.dataFilter.any { !ocrResult.contains(it) }) {
                            Logger.d("识别文本长度: ${ocrResult.length}")
                            // 手动触发 → 强制AI识别
                            send2JsEngine(ocrResult, packageName)
                        } else {
                            Logger.d("数据信息不在识别关键字里面，忽略")
                        }
                    } else {
                        Logger.d("识别结果为空，跳过处理")
                    }
                }

                val totalTime = System.currentTimeMillis() - startTime
                Logger.d("OCR处理完成，总耗时: ${totalTime}ms")

            } catch (e: Exception) {
                Logger.e("OCR处理异常: ${e.message}")
            } finally {
                ocrView.stopOcrView()
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

        // 截图输出路径（外部缓存）
        val outFile = File(coreService.externalCacheDir, "screen.png")
        if (outFile.exists()) {
            outFile.delete()
        }
        // 通过 Shell 执行系统截图
        runCatchingExceptCancel {
            shell.exec("service call statusbar 2")
            delay(300)
            shell.exec("screencap -p ${outFile.absolutePath}")
        }.onFailure {
            // 提醒用户未授权root或者shizuku未运行（使用资源字符串，避免硬编码）
            ToastUtils.info(coreService.getString(net.ankio.auto.R.string.toast_shell_not_ready))
            Logger.e(it.message ?: "", it)
            return null
        }

        val captureTime = System.currentTimeMillis() - captureStartTime
        Logger.d("截图耗时: ${captureTime}ms")

        // 解码并识别
        val bitmap = BitmapFactory.decodeFile(outFile.absolutePath)
        if (bitmap == null) {
            Logger.e("截图解码失败")
            outFile.delete()
            return null
        }

        val croppedBitmap = cropScreenshotTop(bitmap).also { cropped ->
            if (cropped !== bitmap) {
                bitmap.recycle()
            }
        }

        val text = runCatching {
            // 使用服务级 OCR 处理器，跟随服务生命周期复用并统一释放。
            ocrProcessor.startProcess(croppedBitmap)
        }.getOrElse {
            Logger.e("OCR 识别失败: ${it.message}")
            outFile.delete()
            croppedBitmap.recycle()
            return null
        }

        croppedBitmap.recycle()
        outFile.delete()
        return text.ifBlank { null }
    }

    /**
     * 裁剪截图顶部区域，移除系统导航栏与状态栏。
     * @param source 原始截图
     * @return 去掉顶部导航栏后的位图
     */
    private fun cropScreenshotTop(source: Bitmap): Bitmap {
        val cropHeight = DisplayUtils.getStatusBarHeight(coreService)
        if (cropHeight <= 0) return source
        if (source.height <= cropHeight) return source
        return Bitmap.createBitmap(source, 0, cropHeight, source.width, source.height - cropHeight)
    }
    /**
     * 将识别文本交给 JS 引擎做规则识别。
     * 成功：静默（不打扰用户）。
     * 失败：统一 toast 提示（避免多处 if 分支）。
     * @return true 表示识别成功；false 表示未匹配到规则。
     */
    private suspend fun send2JsEngine(
        text: String,
        app: String
    ): Boolean {
        Logger.d("app=$app, text=$text")
        val billResult = JsAPI.analysis(
            DataType.OCR,
            text,
            app,
            fromAppData = false
        )

        return if (billResult == null) {
            // 未匹配到规则 → 明确提示用户
            ToastUtils.error(net.ankio.auto.R.string.no_rule_hint)
            false
        } else {
            // 成功：仅记录日志，不打扰用户
            Logger.d("识别结果：${billResult.billInfoModel}")
            true
        }
    }

    /**
     * 获取当前前台应用包名
     * 支持重试机制，处理应用切换时的不稳定状态
     * @return 返回最近使用的应用包名
     */
    private suspend fun getTopPackagePostL(): String? {
        // 解析顺序：
        // 1) topResumedActivity（最准确）
        // 2) ResumedActivity（其次）
        // 3) mCurrentFocus（窗口焦点，可能为null）
        val commands = listOf(
            "dumpsys activity activities | grep topResumedActivity",
            "dumpsys activity activities | grep ResumedActivity",
            "dumpsys window | grep mCurrentFocus"
        )

        // 重试3次，处理应用切换时的时序问题
        repeat(3) { attempt ->
            for (cmd in commands) {
                val output = runCatchingExceptCancel {
                    shell.exec(cmd)
                }.getOrElse {
                    Logger.w("Shell执行失败[$cmd]: ${it.message}")
                    null
                }

                if (output.isNullOrBlank()) {
                    Logger.d("命令[$cmd]输出为空")
                    continue
                }

                Logger.d("命令[$cmd]输出为 $output")

                val pkg = extractPackageFromDumpsys(output)
                if (!pkg.isNullOrBlank()) {
                    Logger.d("成功获取包名: $pkg (第${attempt + 1}次尝试)")
                    return pkg
                }
            }

            // 前两次失败后等待100ms再重试
            if (attempt < 2) {
                Logger.d("第${attempt + 1}次尝试失败，100ms后重试")
                delay(100)
            }
        }

        Logger.e("所有尝试均失败，无法获取前台应用")
        return null
    }

    /**
     * 从 dumpsys 输出中提取包名（支持多行输出）
     * 使用正则匹配 "包名/Activity" 格式
     * @param output dumpsys 命令的完整输出（可能包含多行）
     * @return 提取到的包名，失败返回null
     */
    private fun extractPackageFromDumpsys(output: String): String? {
        // 正则：匹配标准的包名格式（至少包含一个点，且以字母开头）
        // 示例：com.android.systemui/MainActivity
        val regex = Regex(
            """([a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+)/""",
            RegexOption.IGNORE_CASE
        )

        // 逐行扫描，找到第一个有效的包名
        return output.lineSequence()
            .mapNotNull { line ->
                regex.find(line)?.groupValues?.get(1)
            }
            .firstOrNull { pkg ->
                // 过滤明显无效的包名
                pkg.contains('.') && pkg.length > 3 && !pkg.startsWith(".")
            }
    }


    companion object : IService {

        /** 服务启动状态 */
        var serverStarted = false

        /**
         * 检查是否有使用情况访问权限
         */
        override fun hasPermission(): Boolean {
            return true
        }

        /**
         * 启动权限设置页面
         */
        override fun startPermissionActivity(context: Context) {

        }

        /**
         * 无法获取状态栏高度时的默认裁剪高度 (dp)。
         */
        private const val DEFAULT_TOP_CROP_DP = 56

    }
}




