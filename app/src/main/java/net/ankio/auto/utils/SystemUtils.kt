/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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

package net.ankio.auto.utils

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.util.TypedValue
import androidx.lifecycle.LifecycleOwner
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.activity.HomeActivity
import net.ankio.auto.ui.activity.MainActivity
import net.ankio.auto.ui.utils.ToastUtils
import java.math.BigInteger
import java.security.MessageDigest

/**
 * 系统相关工具类
 */
object SystemUtils {

    private lateinit var application: Application

    /**
     * 初始化
     */
    fun init(app: Application) {
        application = app
    }
    

    /**
     * 在主线程运行
     * @param action () -> Unit
     */
    fun runOnUiThread(action: () -> Unit) {
        application.mainExecutor.execute(action)
    }

    /**
     * 复制到剪切板
     */
    fun copyToClipboard(text: String?) {
        val clipboard = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("text", text)
        clipboard.setPrimaryClip(clip)
    }

    /**
     * 重启应用
     */
    fun restart() {
        val intent = Intent(application, MainActivity::class.java)
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
        Process.killProcess(Process.myPid())
    }

    /**
     * 获取md5
     */
    fun md5(input: String): String {
        val md5Digest = MessageDigest.getInstance("MD5")
        val messageDigest = md5Digest.digest(input.toByteArray())
        val number = BigInteger(1, messageDigest)
        var md5Hash = number.toString(16)
        while (md5Hash.length < 32) {
            md5Hash = "0$md5Hash"
        }
        return md5Hash
    }

    /**
     * dp转px
     */
    fun dp2px(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            application.resources.displayMetrics,
        ).toInt()
    }

    /**
     * 启动Activity
     * @param intent 要启动的Intent
     */
    fun startActivity(intent: Intent) {
        try {
            // 确保Intent有NEW_TASK标志，因为从非Activity上下文启动
            if (intent.flags and FLAG_ACTIVITY_NEW_TASK == 0) {
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
            }
            application.startActivity(intent)
        } catch (e: Exception) {
            Logger.e("Failed to start activity", e)
        }
    }

    /**
     * 安全启动Activity：在调用前检查是否有可处理该Intent的Activity。
     * 当不可处理时，提示用户目标应用未安装，避免崩溃。
     *
     * @param intent 需要启动的隐式或显式Intent
     * @param appName 目标应用名称（用于提示文案）
     * @param onStarted 启动成功后的回调
     * @return 是否成功发起启动
     */
    fun startActivityIfResolvable(
        intent: Intent,
        appName: String,
        onStarted: (() -> Unit)? = null,
    ): Boolean {
        // 保证 NEW_TASK 标志
        if (intent.flags and FLAG_ACTIVITY_NEW_TASK == 0) {
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        }

        val canResolve = application.packageManager.resolveActivity(intent, 0) != null
        if (canResolve) {
            return try {
                application.startActivity(intent)
                onStarted?.invoke()
                true
            } catch (e: Exception) {
                Logger.e("Failed to start activity", e)
                false
            }
        }

        // 统一提示：目标应用未安装 / 无法处理
        ToastUtils.error(
            application.getString(
                net.ankio.auto.R.string.toast_target_app_not_installed,
                appName
            )
        )
        return false
    }
    
    /**
     * 打开记账软件应用
     */
    fun startBookApp() {
        val packageName = PrefManager.bookApp
        val launchIntent = application.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        }
    }

    /**
     * 从Context自动推断LifecycleOwner的扩展函数
     *
     * 通过ContextWrapper递归查找LifecycleOwner，支持：
     * - Activity（实现了LifecycleOwner）
     * - Fragment（实现了LifecycleOwner）
     * - LifecycleService（实现了LifecycleOwner）
     *
     * @return LifecycleOwner实例，如果找不到则抛出异常
     * @throws IllegalStateException 当无法找到LifecycleOwner时抛出
     */
    fun Context.findLifecycleOwner(): LifecycleOwner {
        var context = this
        while (context is ContextWrapper) {
            if (context is LifecycleOwner) {
                return context
            }
            context = context.baseContext
        }
        error("无法从Context中找到LifecycleOwner，请确保Context是Activity、Fragment或LifecycleService")
    }
}
