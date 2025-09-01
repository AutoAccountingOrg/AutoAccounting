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
import net.ankio.auto.BuildConfig
import net.ankio.auto.xposed.XposedModule
import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.logger.Logger
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.DataUtils
import net.ankio.auto.xposed.core.utils.DataUtils.set
import net.ankio.auto.xposed.core.utils.MessageUtils
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting


class App : IXposedHookLoadPackage, IXposedHookZygoteInit {

    companion object {
        const val TAG = "HookerEnvironment"

        /**
         * 目标App映射表，用于快速查找
         * key: packageName + processName
         * value: HookerManifest
         */
        private val hookerMap: Map<String, HookerManifest> by lazy {
            buildHookerMap()
        }

        /**
         * 构建Hook映射表，使用原始包名映射
         */
        private fun buildHookerMap(): Map<String, HookerManifest> {
            return XposedModule.get().associateBy { manifest ->
                manifest.packageName + (manifest.processName.ifEmpty { manifest.packageName })
            }
        }
    }

    /**
     * Hook应用上下文
     */
    private fun hookAppContext(
        manifest: HookerManifest,
        callback: (Application?) -> Unit
    ) {
        when {
            manifest.packageName == "android" -> callback(null)
            manifest.applicationName.isEmpty() -> {
                Logger.logD(TAG, "使用当前应用程序: ${manifest.appName}")
                callback(AndroidAppHelper.currentApplication())
            }
            else -> {
                try {
                    var hooked = false
                    Hooker.after(
                        Instrumentation::class.java,
                        "callApplicationOnCreate",
                        Application::class.java
                    ) {
                        if (hooked) return@after
                        hooked = true
                        val application = it.args[0] as Application
                        Logger.logD(TAG, "Hook成功: ${manifest.applicationName} -> $application")
                        callback(application)

                    }
                } catch (e: Exception) {
                    Logger.log(TAG, "Hook失败: ${e.message}")
                    Logger.logE(TAG, e)
                }
            }
        }
    }

    /**
     * 查找目标应用的Hook清单
     */
    private fun findTargetApp(pkg: String?, processName: String?): HookerManifest? {

        Logger.log(TAG, "$pkg$processName")
        Logger.log(TAG, "$hookerMap")
        if (pkg == null || processName == null) return null

        val key = "$pkg$processName"

        return hookerMap[key]
    }

    /**
     * 加载包时的回调
     */
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val targetApp = findTargetApp(lpparam.packageName, lpparam.processName) ?: return
        // 设置运行时环境
        AppRuntime.classLoader = lpparam.classLoader
        AppRuntime.name = targetApp.appName
        AppRuntime.manifest = targetApp
        hookAppContext(targetApp) { application ->
            AppRuntime.application = application
            application?.let { AppRuntime.classLoader = it.classLoader }
            AppRuntime.debug = DataUtils.configBoolean(Setting.DEBUG_MODE, BuildConfig.DEBUG)
            Logger.logD(
                TAG,
                "Hook器: ${targetApp.appName}(${targetApp.packageName}) 运行在${if (AppRuntime.debug) "调试" else "生产"}模式"
            )
            initHooker()
        }
    }


    /**
     * 初始化Hook器
     */
    private fun initHooker() {
        Logger.log(
            TAG,
            "初始化Hook器: ${AppRuntime.name}, 自动记账版本: ${BuildConfig.VERSION_NAME}, 应用路径: ${AppRuntime.application?.applicationInfo?.sourceDir}"
        )

        // 基础设置
        try {
            AppRuntime.manifest.networkError()

            Toaster.init(AppRuntime.application)

            AppRuntime.manifest.permissionCheck()

        } catch (e: Exception) {
            Logger.logE(TAG, e)
        }

        // 版本检查
        if (!AppRuntime.manifest.versionCheck()) return

        // 规则适配
        val rules = AppRuntime.manifest.beforeAdaption()
        if (!AppRuntime.manifest.autoAdaption(rules)) {
            Logger.log(
                TAG,
                "自动适配失败，${AppRuntime.manifest.appName} 将不会被Hook"
            )
            return
        }

        // 启动Hook
        try {
            AppRuntime.manifest.hookLoadPackage()
        } catch (e: Exception) {
            AppRuntime.manifest.logE(e)
        }

        AppRuntime.manifest.partHookers.forEach { hooker ->
            val hookerName = hooker.javaClass.simpleName
            AppRuntime.manifest.logD("初始化部分Hook器: $hookerName")

            try {
                hooker.hook()
                AppRuntime.manifest.logD("部分Hook器初始化成功: $hookerName")
            } catch (e: Exception) {
                AppRuntime.manifest.logD("部分Hook器错误: ${e.message}")
                AppRuntime.manifest.logE(e)
                set("adaptation_version", "0")
            }
        }

        AppRuntime.manifest.logD("Hook器初始化成功, ${AppRuntime.manifest.appName}(${AppRuntime.versionCode})")

        // 成功通知
        if (!AppRuntime.manifest.systemApp &&
            AppRuntime.manifest.packageName != BuildConfig.APPLICATION_ID &&
            DataUtils.configBoolean(Setting.LOAD_SUCCESS, DefaultData.LOAD_SUCCESS)
        ) {
            MessageUtils.toast("自动记账加载成功")
        }
    }


    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        AppRuntime.modulePath = startupParam?.modulePath ?: ""
    }


}