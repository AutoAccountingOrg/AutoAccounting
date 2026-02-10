/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package net.ankio.auto.service.ocr

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.service.OcrAccessibilityService
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.SystemUtils.startActivity
import net.ankio.shell.Shell
import org.ezbook.server.tools.runCatchingExceptCancel
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

/**
 * OCR 工具类：封装获取前台应用和截图能力。
 *
 * 根据用户选择的授权方式，**分别**调用对应的底层实现：
 * - root：强制通过 [Shell.execAsRoot] 执行
 * - shizuku：强制通过 [Shell.execAsShizuku] 执行
 * - accessibility：通过 [OcrAccessibilityService] 的 Android API
 *
 * 每种模式独立检查权限，不足时抛出 [PermissionException] 以便上层给出精准提示。
 *
 * @param shell Shell 实例，Root/Shizuku 模式下使用
 */
class OcrTools(private val shell: Shell) {

    /**
     * 权限不足异常
     * @param mode 当前授权模式（root / shizuku / accessibility）
     */
    class PermissionException(val mode: String) :
        RuntimeException("OCR permission not available: $mode")

    // ======================== 公开接口 ========================

    /**
     * 检查当前授权模式下的权限是否可用
     * @return true 权限可用
     */
    fun hasPermission(): Boolean = when (PrefManager.ocrAuthMode) {
        "root" -> shell.hasRootPermission()
        "shizuku" -> shell.hasShizukuPermission()
        "accessibility" -> OcrAccessibilityService.instance != null
        else -> false
    }

    /**
     * 获取当前前台应用包名
     * @return 包名，失败返回 null
     * @throws PermissionException 权限不足时抛出
     */
    suspend fun getTopApp(): String? = when (PrefManager.ocrAuthMode) {
        "root" -> getTopAppByShell { shell.execAsRoot(it) }
        "shizuku" -> getTopAppByShell { shell.execAsShizuku(it) }
        "accessibility" -> getTopAppByAccessibility()
        else -> throw PermissionException(PrefManager.ocrAuthMode)
    }

    /**
     * 截取当前屏幕到文件
     * @param outFile 截图输出路径
     * @return 截图是否成功
     * @throws PermissionException 权限不足时抛出
     */
    suspend fun takeScreenshot(outFile: File): Boolean = when (PrefManager.ocrAuthMode) {
        "root" -> takeScreenshotByShell(outFile) { shell.execAsRoot(it) }
        "shizuku" -> takeScreenshotByShell(outFile) { shell.execAsShizuku(it) }
        "accessibility" -> takeScreenshotByAccessibility(outFile)
        else -> throw PermissionException(PrefManager.ocrAuthMode)
    }

    /**
     * 收起状态栏/通知栏
     * 截图前调用，避免通知栏遮挡内容。
     */
    suspend fun collapseStatusBar() {
        Logger.d("收起通知栏")
        Logger.d(PrefManager.ocrAuthMode)
        when (PrefManager.ocrAuthMode) {
            "root" -> runCatchingExceptCancel { shell.execAsRoot("service call statusbar 2") }.onFailure {
                Logger.e(
                    it
                )
            }

            "shizuku" -> runCatchingExceptCancel { shell.execAsShizuku("service call statusbar 2") }.onFailure {
                Logger.e(
                    it
                )
            }

            "accessibility" -> OcrAccessibilityService.instance?.performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE
            )
        }
    }
    // ======================== Shell 实现 ========================

    /**
     * Shell 方式获取前台应用包名
     * 通过传入的 [exec] 函数决定使用 Root 还是 Shizuku。
     * 依次尝试 topResumedActivity → ResumedActivity → mCurrentFocus，重试3次。
     */
    private suspend fun getTopAppByShell(exec: suspend (String) -> String): String? {
        val commands = listOf(
            "dumpsys activity activities | grep topResumedActivity",
            "dumpsys activity activities | grep ResumedActivity",
            "dumpsys window | grep mCurrentFocus"
        )

        repeat(3) { attempt ->
            for (cmd in commands) {
                val output = runCatchingExceptCancel { exec(cmd) }.getOrElse {
                    Logger.w("Shell执行失败[$cmd]: ${it.message}")
                    null
                }

                if (output.isNullOrBlank()) continue

                Logger.d("命令[$cmd]输出为 $output")
                val pkg = extractPackageFromDumpsys(output)
                if (!pkg.isNullOrBlank()) {
                    Logger.d("成功获取包名: $pkg (第${attempt + 1}次尝试)")
                    return pkg
                }
            }
            // 前两次失败后等待100ms再重试
            if (attempt < 2) delay(100)
        }

        Logger.e("所有尝试均失败，无法获取前台应用")
        return null
    }

    /**
     * Shell 方式截图
     * 通过传入的 [exec] 函数决定使用 Root 还是 Shizuku。
     * @return 截图文件是否生成成功
     */
    private suspend fun takeScreenshotByShell(
        outFile: File,
        exec: suspend (String) -> String
    ): Boolean {
        return runCatchingExceptCancel {
            exec("screencap -p ${outFile.absolutePath}")
            outFile.exists() && outFile.length() > 0
        }.getOrElse {
            Logger.e("Shell截图失败: ${it.message}")
            false
        }
    }

    // ======================== 无障碍实现 ========================

    /**
     * 无障碍方式获取前台应用包名
     * 通过 [OcrAccessibilityService] 跟踪的窗口变化事件获取。
     */
    private fun getTopAppByAccessibility(): String? {
        val service = OcrAccessibilityService.instance
        if (service == null) {
            Logger.w("无障碍服务未运行")
            throw PermissionException("accessibility")
        }
        val pkg = OcrAccessibilityService.topPackage
        if (pkg.isNullOrBlank()) {
            Logger.w("无障碍服务未获取到前台应用")
        }
        return pkg
    }

    /**
     * 无障碍方式截图（Android 11+ API）
     * @return 截图文件是否生成成功
     */
    private suspend fun takeScreenshotByAccessibility(outFile: File): Boolean {
        // takeScreenshot API 需要 Android 11 (API 30)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Logger.e("无障碍截图需要 Android 11+")
            return false
        }

        val service = OcrAccessibilityService.instance
            ?: throw PermissionException("accessibility")

        return suspendCancellableCoroutine { cont ->
            service.takeScreenshot(
                android.view.Display.DEFAULT_DISPLAY,
                service.mainExecutor,
                object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                        val bitmap = Bitmap.wrapHardwareBuffer(
                            result.hardwareBuffer, result.colorSpace
                        )
                        val success = if (bitmap != null) {
                            FileOutputStream(outFile).use { fos ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                            }
                            bitmap.recycle()
                            true
                        } else {
                            false
                        }
                        result.hardwareBuffer.close()
                        cont.resume(success)
                    }

                    override fun onFailure(errorCode: Int) {
                        Logger.e("无障碍截图失败，错误码: $errorCode")
                        cont.resume(false)
                    }
                }
            )
        }
    }

    // ======================== 辅助方法 ========================

    /**
     * 从 dumpsys 输出中提取包名（支持多行输出）
     * 使用正则匹配 "包名/Activity" 格式
     */
    private fun extractPackageFromDumpsys(output: String): String? {
        val regex = Regex(
            """([a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+)/""",
            RegexOption.IGNORE_CASE
        )
        return output.lineSequence()
            .mapNotNull { line -> regex.find(line)?.groupValues?.get(1) }
            .firstOrNull { pkg -> pkg.contains('.') && pkg.length > 3 && !pkg.startsWith(".") }
    }

    companion object {
        /** 用于权限检查的 Shell 实例（懒加载，避免无用开销） */
        private val shell by lazy { Shell(BuildConfig.APPLICATION_ID) }

        fun reqShizuku() {
            if (!shell.hasShizukuPermission()) {
                // 请求 Shizuku 授权
                shell.requestShizukuPermission()
                ToastUtils.warn(R.string.ocr_error_shizuku_not_available)
            }
        }

        fun reqRoot() {
            if (!shell.hasRootPermission()) {
                ToastUtils.error(R.string.ocr_error_root_not_available)
            }
        }

        fun reqAccessibility() {
            if (OcrAccessibilityService.instance == null) {
                // 跳转到系统无障碍设置
                ToastUtils.warn(R.string.ocr_error_accessibility_not_ready)
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }

        fun getDefault(): String {
            return when (PrefManager.workMode) {
                WorkMode.Ocr -> "accessibility"
                WorkMode.LSPatch -> "shizuku"
                WorkMode.Xposed -> "root"
            }
        }

        fun checkPermission() {
            when (PrefManager.ocrAuthMode) {
                "root" -> reqRoot()
                "shizuku" -> reqShizuku()
                "accessibility" -> reqAccessibility()
            }
        }

    }
}
