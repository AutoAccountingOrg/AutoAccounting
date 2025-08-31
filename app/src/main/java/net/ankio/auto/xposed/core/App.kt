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
                "${manifest.packageName}${manifest.processName}"
            }
        }
    }

    /**
     * hook Application Context - 直接简洁版本
     */
    private fun hookAppContext(
        manifest: HookerManifest,
        callback: (Application?) -> Unit
    ) {
        when {
            manifest.packageName == "android" -> callback(null)
            manifest.applicationName.isEmpty() -> {
                Logger.logD(TAG, "Using current application for ${manifest.appName}")
                callback(AndroidAppHelper.currentApplication())
            }

            else -> {
                var hookExecuted = false
                runCatching {
                    Hooker.after(
                        Instrumentation::class.java.name,
                        "callApplicationOnCreate",
                        Application::class.java.name
                    ) {
                        if (!hookExecuted) {
                            hookExecuted = true
                            val application = it.args[0] as Application
                            Logger.logD(
                                TAG,
                                "Hook success: ${manifest.applicationName} -> $application"
                            )
                            callback(application)
                        }
                    }
                }.onFailure {
                    Logger.log(TAG, "Hook failed: ${it.message}")
                    Logger.logE(TAG, it)
                }
            }
        }
    }

    /**
     * 查找目标应用的Hook清单 - 使用原始包名直接查找
     */
    private fun findTargetApp(pkg: String?, processName: String?): HookerManifest? {
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
            AppRuntime.debug = DataUtils.configBoolean(Setting.DEBUG_MODE, true)

            Logger.logD(
                TAG,
                "Hooker: ${targetApp.appName}(${targetApp.packageName}) Run in ${if (AppRuntime.debug) "debug" else "production"} Mode"
            )
            initHooker()
        }
    }


    /**
     * 初始化Hooker - 直接执行，不过度抽象
     */
    private fun initHooker() {
        Logger.log(
            TAG,
            "InitHooker: ${AppRuntime.name}, AutoVersion: ${BuildConfig.VERSION_NAME}, Application: ${AppRuntime.application?.applicationInfo?.sourceDir}"
        )

        // 基础设置
        runCatching {
            AppRuntime.manifest.networkError()
            Logger.logD(TAG, "Allow Cleartext Traffic")
        }.onFailure { Logger.logE(TAG, it) }

        runCatching {
            Toaster.init(AppRuntime.application)
            Logger.logD(TAG, "Toaster init success")
        }.onFailure { Logger.logE(TAG, it) }

        runCatching {
            AppRuntime.manifest.permissionCheck()
            Logger.logD(TAG, "Permission check success")
        }.onFailure { Logger.logE(TAG, it) }

        // 版本检查
        if (!AppRuntime.manifest.versionCheck()) return

        // 规则适配
        val rules = AppRuntime.manifest.beforeAdaption()
        if (!AppRuntime.manifest.autoAdaption(rules)) {
            Logger.log(
                TAG,
                "Auto adaption failed, ${AppRuntime.manifest.appName} will not be hooked"
            )
            return
        }

        // 启动Hook
        runCatching {
            AppRuntime.manifest.hookLoadPackage()
        }.onFailure { AppRuntime.manifest.logE(it) }

        AppRuntime.manifest.partHookers.forEach { hooker ->
            val hookerName = hooker.javaClass.simpleName
            AppRuntime.manifest.logD("PartHooker init: $hookerName")

            runCatching {
                hooker.hook()
                AppRuntime.manifest.logD("PartHooker init success: $hookerName")
            }.onFailure {
                AppRuntime.manifest.logD("PartHooker error: ${it.message}")
                AppRuntime.manifest.logE(it)
                set("adaptation_version", "0")
            }
        }

        AppRuntime.manifest.logD("Hooker init success, ${AppRuntime.manifest.appName}(${AppRuntime.versionCode})")

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