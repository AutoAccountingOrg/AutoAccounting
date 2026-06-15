package net.ankio.auto.service.ocr

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.Settings
import android.view.Display
import com.google.android.accessibility.selecttospeak.SelectToSpeakService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.SystemUtils
import net.ankio.shell.Shell
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

/**
 * OCR 助手：负责截图与前台应用检测
 */
object OcrTools {

    private val SERVICE_CLASS = SelectToSpeakService::class.java
    private val COMPONENT_NAME = "${BuildConfig.APPLICATION_ID}/${SERVICE_CLASS.name}"

    // ======================== 核心功能 ========================

    /**
     * 当前前台应用包名（供 OCR 规则引擎 app 参数）。
     */
    suspend fun getTopApp(): String? {
        if (SelectToSpeakService.instance == null) {
            Logger.w("[OcrTopApp] SelectToSpeakService.instance is null (accessibility not connected)")
        }



        return SelectToSpeakService.instance?.getTopPackage()
    }

    /** 截取当前屏幕 */
    suspend fun takeScreenshot(outFile: File): Boolean = withContext(Dispatchers.IO) {
        val service = SelectToSpeakService.instance ?: return@withContext false

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext false

        suspendCancellableCoroutine { cont ->
            service.takeScreenshot(
                Display.DEFAULT_DISPLAY,
                service.mainExecutor,
                object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                        val bitmap =
                            Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)
                        val success = bitmap?.let {
                            val saved = saveBitmap(it, outFile)
                            it.recycle()
                            saved
                        } ?: false
                        result.hardwareBuffer.close()
                        cont.resume(success)
                    }

                    override fun onFailure(errorCode: Int) {
                        Logger.e("Screenshot failed: $errorCode")
                        cont.resume(false)
                    }
                })
        }
    }

    private fun saveBitmap(bitmap: Bitmap, file: File): Boolean = try {
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        true
    } catch (e: Exception) {
        false
    }

    // ======================== 权限管理 ========================

    fun hasPermission() = SystemUtils.isAccessibilityServiceEnabled(SERVICE_CLASS)


    /** 尝试开启无障碍：有 Root 用 Root，没 Root 弹设置 */
    suspend fun requestPermission(): Boolean {
        if (hasPermission()) return true

        val shell = Shell(BuildConfig.APPLICATION_ID)
        val hasShell = shell.rootPermission() || shell.shizukuPermission()

        if (hasShell) {
            tryEnableViaShell(shell)
            delay(800) // 等待服务启动
        }

        if (!hasPermission()) {
            withContext(Dispatchers.Main) { openSettings() }
            return false

        }
        return true
    }

    private suspend fun tryEnableViaShell(shell: Shell) {
        val cmdGet = "settings get secure enabled_accessibility_services"
        val current = shell.exec(cmdGet).trim().let { if (it == "null") "" else it }

        if (current.contains(COMPONENT_NAME)) return

        val newList = if (current.isEmpty()) COMPONENT_NAME else "$current:$COMPONENT_NAME"
        shell.exec("settings put secure enabled_accessibility_services $newList")
        shell.exec("settings put secure accessibility_enabled 1")
    }

    private fun openSettings() {
        SystemUtils.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    suspend fun collapseStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SelectToSpeakService.instance?.performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE
            )
        } else {
            withContext(Dispatchers.IO) {
                Shell(BuildConfig.APPLICATION_ID).use { shell ->
                    runCatching {
                        if (shell.checkPermission()) {
                            shell.exec("cmd statusbar collapse")
                        }
                    }
                }
            }
        }
        delay(500)
    }
}
