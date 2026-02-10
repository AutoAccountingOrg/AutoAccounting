package net.ankio.auto.service

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
import net.ankio.auto.R
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.http.api.JsAPI
import net.ankio.auto.service.api.ICoreService
import net.ankio.auto.service.api.IService
import net.ankio.auto.service.ocr.FlipDetector
import net.ankio.auto.service.ocr.OcrViews
import net.ankio.auto.service.overlay.SaveProgressView
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.utils.DisplayUtils
import net.ankio.auto.utils.PrefManager
import net.ankio.ocr.OcrProcessor
import net.ankio.shell.Shell
import org.ezbook.server.constant.DataType
import org.ezbook.server.models.BillResultModel
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.intent.IntentType
import org.ezbook.server.tools.runCatchingExceptCancel
import java.io.File

/**
 * OCR服务类，用于实现屏幕文字识别功能
 * 主要功能：
 * 1. 监听设备翻转事件（从朝下翻转到朝上）
 * 2. 截取屏幕内容
 * 3. 进行OCR文字识别
 * 4. 顶部横幅提示处理进度
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
        // 确保状态横幅被清理
        ocrView.dismiss()

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
     * 所有异常和状态通过顶部横幅展示。
     */
    private suspend fun triggerOcr(manual: Boolean) {
        if (ocrDoing) {
            Logger.d("OCR正在处理中，跳过本次请求")
            return
        }

        ocrDoing = true
        triggerVibration()

        // 解析前台应用包名
        val packageName = if (manual) {
            getTopPackagePostL() ?: run {
                Logger.w("无法获取前台应用")
                ocrView.showError(
                    coreService,
                    coreService.getString(R.string.ocr_error_no_foreground_app)
                )
                ocrDoing = false
                return
            }
        } else {
            val pkg = getTopPackagePostL() ?: run {
                Logger.w("无法获取前台应用")
                // 自动触发获取失败静默忽略，不打扰用户
                ocrDoing = false
                return
            }
            if (pkg !in PrefManager.appWhiteList) {
                Logger.d("前台应用 $pkg 不在监控白名单")
                // 自动触发不在白名单静默忽略
                ocrDoing = false
                return
            }
            pkg
        }

        Logger.d("检测到应用 [$packageName]，开始OCR")
        executeOcrFlow(packageName)
    }

    /**
     * 执行OCR识别的完整流程
     *
     * 关键顺序：先截图（屏幕干净），再弹横幅告知用户进度。
     * 所有错误和结果均通过横幅展示，不使用 Toast。
     */
    private fun executeOcrFlow(packageName: String) {

        coreService.lifecycleScope.launch {
            val startTime = System.currentTimeMillis()
            try {
                Logger.d("开始OCR流程")

                // 1. 先在IO线程截图+OCR（此时屏幕无任何覆盖物，截图干净）
                val ocrResult = withContext(Dispatchers.IO) { performOcrCapture() }

                // performOcrCapture 内部已通过横幅展示错误，null 直接返回
                if (ocrResult == null) return@launch

                // 检查识别关键字过滤
                if (PrefManager.dataFilter.all { ocrResult.contains(it) }) {
                    Logger.d("数据信息不在识别关键字里面，忽略")
                    ocrView.showError(
                        coreService,
                        coreService.getString(R.string.ocr_error_keyword_filtered)
                    )
                    return@launch
                }

                Logger.d("识别文本长度: ${ocrResult.length}")

                // 2. 更新横幅：正在AI识别
                ocrView.updateStatus(coreService.getString(R.string.ocr_status_ai_analyzing))

                // 3. 发送给JS引擎做AI规则识别
                val billResult = withContext(Dispatchers.IO) {
                    send2JsEngine(ocrResult, packageName)
                }

                val totalTime = System.currentTimeMillis() - startTime
                Logger.d("OCR处理完成，总耗时: ${totalTime}ms")

                // 4. 在横幅上显示最终结果
                if (billResult != null) {
                    val moneyText = String.format("¥%.2f", billResult.billInfoModel.money)
                    ocrView.showSuccess(coreService, moneyText) {
                        // 3秒后回调：悬浮窗已由 JsAPI.analysis 内部自动拉起
                    }
                } else {
                    ocrView.showError(coreService, coreService.getString(R.string.no_rule_hint))
                }

            } catch (e: Exception) {
                Logger.e("OCR处理异常: ${e.message}")
                ocrView.dismiss()
            } finally {
                ocrDoing = false
            }
        }

    }

    /**
     * 执行屏幕截图和OCR识别
     *
     * 流程：截图 → 弹横幅"正在OCR识别" → OCR → 返回文本
     * 所有错误通过横幅展示。
     * @return 识别出的文本，失败返回null（横幅已展示错误）
     */
    private suspend fun performOcrCapture(): String? {
        val captureStartTime = System.currentTimeMillis()

        // 截图输出路径
        val outFile = File(coreService.externalCacheDir, "screen.png")
        if (outFile.exists()) outFile.delete()

        // 通过 Shell 执行系统截图
        runCatchingExceptCancel {
            shell.exec("service call statusbar 2")
            delay(300)
            shell.exec("screencap -p ${outFile.absolutePath}")
        }.onFailure {
            Logger.e(it.message ?: "", it)
            withContext(Dispatchers.Main) {
                ocrView.showError(
                    coreService,
                    coreService.getString(R.string.ocr_error_shell_not_ready)
                )
            }
            return null
        }

        val captureTime = System.currentTimeMillis() - captureStartTime
        Logger.d("截图耗时: ${captureTime}ms")

        // 截图完成，显示顶部横幅"正在OCR识别"（截图干净，不会被污染）
        withContext(Dispatchers.Main) {
            ocrView.show(coreService, coreService.getString(R.string.ocr_status_recognizing))
        }

        // 解码截图
        val bitmap = BitmapFactory.decodeFile(outFile.absolutePath)
        if (bitmap == null) {
            Logger.e("截图解码失败")
            outFile.delete()
            withContext(Dispatchers.Main) {
                ocrView.showError(
                    coreService,
                    coreService.getString(R.string.ocr_error_capture_failed)
                )
            }
            return null
        }

        // 裁剪状态栏区域
        val croppedBitmap = cropScreenshotTop(bitmap).also { cropped ->
            if (cropped !== bitmap) bitmap.recycle()
        }

        // OCR识别
        val text = runCatching {
            ocrProcessor.startProcess(croppedBitmap)
        }.getOrElse {
            Logger.e("OCR 识别失败: ${it.message}")
            outFile.delete()
            croppedBitmap.recycle()
            withContext(Dispatchers.Main) {
                ocrView.showError(coreService, coreService.getString(R.string.ocr_error_ocr_failed))
            }
            return null
        }

        croppedBitmap.recycle()
        outFile.delete()

        // 识别结果为空
        if (text.isBlank()) {
            withContext(Dispatchers.Main) {
                ocrView.showError(
                    coreService,
                    coreService.getString(R.string.ocr_error_empty_result)
                )
            }
            return null
        }

        return text
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
     * @return 匹配结果，null 表示未匹配到规则
     */
    private suspend fun send2JsEngine(
        text: String,
        app: String
    ): BillResultModel? {
        Logger.d("app=$app, text=$text")
        val billResult = JsAPI.analysis(
            DataType.OCR,
            text,
            app,
            fromAppData = false
        )

        if (billResult != null) {
            Logger.d("识别结果：${billResult.billInfoModel}")
        } else {
            Logger.d("未匹配到规则")
        }
        return billResult
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




