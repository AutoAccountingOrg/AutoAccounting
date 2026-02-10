package net.ankio.auto.service

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Base64
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
import net.ankio.auto.service.ocr.OcrTools
import net.ankio.auto.service.ocr.OcrViews
import net.ankio.auto.service.overlay.SaveProgressView
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.utils.DisplayUtils
import net.ankio.auto.utils.PrefManager
import net.ankio.ocr.OcrProcessor
import net.ankio.shell.Shell
import org.ezbook.server.constant.DataType
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.intent.IntentType
import org.ezbook.server.models.BillResultModel
import org.ezbook.server.models.ResultModel
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

    // OCR 工具类：封装 getTopApp + 截图，根据授权方式自动选择实现
    private val ocrTools = OcrTools(shell)

    private val ocrView = OcrViews()

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

        OcrTools.checkPermission()
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
        ocrTools.collapseStatusBar()
        // 通过 OcrTools 获取前台应用包名（根据授权方式分别调用 Root/Shizuku/无障碍）
        val packageName = if (manual) {
            ocrTools.getTopApp() ?: run {
                Logger.w("无法获取前台应用")
                ocrView.showError(
                    coreService,
                    coreService.getString(R.string.ocr_error_no_foreground_app)
                )
                ocrDoing = false
                return
            }
        } else {
            val pkg = ocrTools.getTopApp() ?: run {
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
                val captureResult = withContext(Dispatchers.IO) { performOcrCapture() }

                // performOcrCapture 内部已通过横幅展示错误，null 直接返回
                if (captureResult == null) return@launch

                // 检查识别关键字过滤
                if (PrefManager.dataFilter.all { captureResult.text.contains(it) }) {
                    Logger.d("数据信息不在识别关键字里面，忽略")
                    ocrView.showError(
                        coreService,
                        coreService.getString(R.string.ocr_error_keyword_filtered)
                    )
                    return@launch
                }

                Logger.d("识别文本长度: ${captureResult.text.length}，图片Base64长度: ${captureResult.imageBase64.length}")

                // 2. 更新横幅：正在AI识别
                ocrView.updateStatus(coreService.getString(R.string.ocr_status_ai_analyzing))

                // 3. 发送给JS引擎做AI规则识别
                val result = withContext(Dispatchers.IO) {
                    send2JsEngine(captureResult.text, packageName)
                }

                val totalTime = System.currentTimeMillis() - startTime
                Logger.d("OCR处理完成，总耗时: ${totalTime}ms")

                // 4. 在横幅上显示最终结果（展示服务端返回的具体信息）
                val billData = result.data
                if (billData != null) {
                    val moneyText = String.format("¥%.2f", billData.billInfoModel.money)
                    ocrView.showSuccess(coreService, moneyText) {
                        // 3秒后回调：悬浮窗已由 JsAPI.analysis 内部自动拉起
                    }
                } else {
                    ocrView.showError(coreService, result.msg)
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
     * OCR 捕获结果：识别文本 + 压缩后的截图 Base64（供后续存储使用）
     */
    private data class OcrCaptureResult(val text: String, val imageBase64: String)

    /**
     * 执行屏幕截图和OCR识别
     *
     * 流程：截图 → 弹横幅"正在OCR识别" → Luban压缩 → OCR → 返回结果
     * 所有错误通过横幅展示。
     * @return 识别结果（文本+压缩图片Base64），失败返回null
     */
    private suspend fun performOcrCapture(): OcrCaptureResult? {
        val captureStartTime = System.currentTimeMillis()

        // 截图输出路径
        val outFile = File(coreService.externalCacheDir, "screen.png")
        if (outFile.exists()) outFile.delete()

        // 通过 OcrTools 执行截图（根据授权方式分别调用 Root/Shizuku/无障碍）
        runCatchingExceptCancel {
            delay(300)
            val success = ocrTools.takeScreenshot(outFile)
            if (!success) throw IllegalStateException("截图失败")
        }.onFailure {
            Logger.e(it.message ?: "", it)
            withContext(Dispatchers.Main) {
                ocrView.showError(
                    coreService,
                    coreService.getString(R.string.ocr_error_capture_failed)
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

        // 缩小图片加速 OCR（短边缩到 OCR_MAX_SHORT_EDGE，像素量大幅减少）
        val scaledBitmap = scaleDownForOcr(croppedBitmap).also { scaled ->
            if (scaled !== croppedBitmap) croppedBitmap.recycle()
        }
        Logger.d("图片缩放: ${croppedBitmap.width}x${croppedBitmap.height} → ${scaledBitmap.width}x${scaledBitmap.height}")

        // 将缩小后的图片编码为 JPEG Base64（供后续存储）
        val imageBase64 = bitmapToBase64(scaledBitmap)

        // OCR 识别（用缩小后的图，速度更快）
        val text = runCatching {
            ocrProcessor.startProcess(scaledBitmap)
        }.getOrElse {
            Logger.e("OCR 识别失败: ${it.message}")
            outFile.delete()
            scaledBitmap.recycle()
            withContext(Dispatchers.Main) {
                ocrView.showError(coreService, coreService.getString(R.string.ocr_error_ocr_failed))
            }
            return null
        }

        scaledBitmap.recycle()
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

        return OcrCaptureResult(text, imageBase64)
    }

    /**
     * 缩小 Bitmap 加速 OCR：短边不超过 [OCR_MAX_SHORT_EDGE]。
     * 对于 OCR 文字识别，720px 短边足够清晰，像素量比 1440p 减少 75%。
     * @return 缩小后的 Bitmap，如果已足够小则原样返回
     */
    private fun scaleDownForOcr(source: Bitmap): Bitmap {
        val shortSide = minOf(source.width, source.height)
        if (shortSide <= OCR_MAX_SHORT_EDGE) return source

        val scale = OCR_MAX_SHORT_EDGE.toFloat() / shortSide
        val newW = (source.width * scale).toInt()
        val newH = (source.height * scale).toInt()
        return Bitmap.createScaledBitmap(source, newW, newH, true)
    }

    /**
     * 将 Bitmap 压缩为 JPEG 并编码为 Base64
     * @param bitmap 位图（不回收，调用方负责）
     * @return Base64 字符串
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
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
     * @return 服务端完整响应（包含 code/msg/data），调用方可据此展示具体失败原因
     */
    private suspend fun send2JsEngine(
        text: String,
        app: String
    ): ResultModel<BillResultModel> {
        Logger.d("app=$app, text=$text")
        val result = JsAPI.analysis(
            DataType.OCR,
            text,
            app,
            fromAppData = false
        )

        if (result.data != null) {
            Logger.d("识别结果：${result.data?.billInfoModel}")
        } else {
            Logger.d("分析失败(${result.code}): ${result.msg}")
        }
        return result
    }

    companion object : IService {

        /** 服务启动状态 */
        var serverStarted = false

        /**
         * 检查是否有使用情况访问权限
         */
        override fun hasPermission(): Boolean {
            OcrTools.checkPermission()
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

        /**
         * OCR 识别时图片短边最大值（px）。
         * 720px 对文字识别足够清晰，像素量比 1440p 减少约 75%，OCR 速度显著提升。
         */
        private const val OCR_MAX_SHORT_EDGE = 720

    }
}




