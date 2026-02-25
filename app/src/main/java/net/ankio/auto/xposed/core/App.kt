/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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

package net.ankio.auto.xposed.core

import android.app.AndroidAppHelper
import android.app.Application
import android.app.Instrumentation
import com.hjq.toast.Toaster
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.runBlocking
import net.ankio.auto.BuildConfig
import net.ankio.auto.xposed.XposedModule
import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.logger.XposedLogger
import net.ankio.auto.xposed.core.utils.AdaptationUtils
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.CoroutineUtils
import net.ankio.auto.xposed.core.utils.DataUtils
import net.ankio.auto.xposed.core.utils.DataUtils.set
import net.ankio.auto.xposed.core.utils.MessageUtils
import net.ankio.auto.xposed.core.utils.NetSecurityUtils
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting


class App : IXposedHookLoadPackage, IXposedHookZygoteInit {

    companion object {
        const val TAG = "HookerEnvironment"
    }

    /**
     * Hook应用上下文
     *
     * 根据不同场景获取目标应用的 Application 实例：
     * 1. android 系统进程：无需 Application，直接返回 null
     * 2. 使用默认 Application：从当前运行环境获取
     * 3. 自定义 Application：Hook Instrumentation.callApplicationOnCreate 来捕获
     */
    private fun hookAppContext(
        manifest: HookerManifest,
        callback: (Application?) -> Unit
    ) {
        XposedLogger.d("start hook app context")
        when {
            // 系统进程无需 Application 实例
            manifest.packageName == "android" -> {
                callback(null)
            }

            // 默认 Application，直接获取
            manifest.applicationName.isEmpty() -> {
                XposedLogger.d("Using default Application: ${manifest.appName}")
                callback(AndroidAppHelper.currentApplication())
            }

            // 自定义 Application，Hook 生命周期方法
            else -> {
                hookCustomApplication(manifest, callback)
            }
        }
    }

    /**
     * Hook 自定义 Application 的创建过程
     *
     * 通过 Hook Instrumentation.callApplicationOnCreate 在 Application 创建时获取实例。
     * 使用 runOnce 标志确保回调只执行一次，避免重复初始化。
     */
    private var hookedApplication = false
    private fun hookCustomApplication(
        manifest: HookerManifest,
        callback: (Application?) -> Unit
    ) {
        try {


            Hooker.after(
                Instrumentation::class.java,
                "callApplicationOnCreate",
                Application::class.java
            ) { param ->
                // 确保回调只执行一次
                if (hookedApplication) return@after
                hookedApplication = true

                val application = param.args[0] as Application
                XposedLogger.d("Hook Application success: ${manifest.applicationName} -> ${application.javaClass.name}")
                callback(application)
            }



        } catch (e: Exception) {
            XposedLogger.i("Hook Application failed: ${manifest.applicationName}, error: ${e.message}")
            XposedLogger.e(e)
        }
    }

    /**
     * 查找目标应用的Hook清单
     */
    private fun findTargetApp(pkg: String?, processName: String?): HookerManifest? {
        if (pkg == null) return null
        XposedModule.get().forEach {
            val process = it.processName.ifEmpty { it.packageName }
            if (it.packageName == pkg) {
                if (processName == null || process == processName) return it
                else {
                    XposedLogger.d("Process name mismatch: expected ${process}, got ${processName}")
                }
            }
        }
        return null
    }

    /**
     * 加载包时的回调
     */
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val targetApp = findTargetApp(lpparam.packageName, lpparam.processName) ?: return

        XposedLogger.app = lpparam.packageName
        // 如果无法通过api获取当前是否为调试模式应该默认会退到调试模式，因为此时的API服务尚未启动或者被其他应用阻止
        // 会退到调试模式有利于用户通过xposed日志反馈问题

        XposedLogger.extendLoggerPrinter = {
            XposedBridge.log(it)
        }

        AppRuntime.classLoader = lpparam.classLoader

        hookAppContext(targetApp) { application ->
            val classLoader = application?.classLoader ?: lpparam.classLoader
            AppRuntime.init(application, classLoader, targetApp)
            XposedLogger.i("Init hook: ${AppRuntime.name}, version: ${BuildConfig.VERSION_NAME}, app path: ${AppRuntime.application?.applicationInfo?.sourceDir}")
            // 设置允许明文
            NetSecurityUtils.allowCleartextTraffic()
            // 初始化Toast
            application?.let { Toaster.init(it) }
            //
            if (!AdaptationUtils.autoAdaption(targetApp)) {
                XposedLogger.w("Auto adaptation failed, ${AppRuntime.manifest.appName} will not be hooked")
                return@hookAppContext
            }

            startHooker(targetApp)
        }
    }


    /**
     * 初始化Hook器
     */
    private fun startHooker(manifest: HookerManifest) {


        // 启动Hook
        try {
            manifest.hookLoadPackage()
        } catch (e: Exception) {
            XposedLogger.e(e)
        }

        manifest.partHookers.forEach { hooker ->
            val hookerName = hooker.javaClass.simpleName
            XposedLogger.d("Init part hooker: $hookerName")

            try {
                hooker.hook()
                XposedLogger.d("Part hooker init success: $hookerName")
            } catch (e: Exception) {
                XposedLogger.d("Part hooker error: ${e.message}")
                XposedLogger.e(e)
                set("adaptation_version", "0")
            }
        }

        XposedLogger.d("Hook init success, ${AppRuntime.manifest.appName}")

        // 成功通知
        if (!manifest.systemApp &&
            manifest.packageName != BuildConfig.APPLICATION_ID
        ) {
            CoroutineUtils.withMain {
                if (DataUtils.configBoolean(Setting.LOAD_SUCCESS, DefaultData.LOAD_SUCCESS)) {
                    MessageUtils.toast("自动记账加载成功")
                }
            }
        }
    }


    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        AppRuntime.modulePath = startupParam?.modulePath ?: ""
    }


}