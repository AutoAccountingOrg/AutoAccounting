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
import net.ankio.auto.xposed.Apps
import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.logger.Logger
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.DataUtils
import net.ankio.auto.xposed.core.utils.DataUtils.set
import net.ankio.auto.xposed.core.utils.MessageUtils
import org.ezbook.server.Server
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting


class App : IXposedHookLoadPackage, IXposedHookZygoteInit {

    companion object {
        const val TAG = "HookerEnvironment"
    }

    /**
     * hook Application Context
     * @param applicationName String
     * @param callback Function1<Application?, Unit>
     *     回调函数，返回Application对象
     *     如果hook失败，返回null
     */
    private fun hookAppContext(
        applicationName: String,
        callback: (Application?) -> Unit
    ) {
        var hookStatus = false
        if (AppRuntime.manifest.packageName == "android") {
            callback(null)
            return
        }
        if (applicationName.isEmpty()) {
            Logger.logD(TAG, "Application name is empty")
            callback(AndroidAppHelper.currentApplication())
            return
        }

        fun onCachedApplication(application: Application, method: String) {
            if (hookStatus) {
                return
            }
            hookStatus = true
            runCatching {
                Logger.logD(TAG, "Hook success: $applicationName.$method -> $application")
                callback(application)
            }.onFailure {
                Logger.log(TAG, "Hook error: ${it.message}")
                Logger.logE(TAG, it)
            }
        }

        Hooker.after(
            Instrumentation::class.java.name,
            "callApplicationOnCreate",
            Application::class.java.name
        ) {
            val context = it.args[0] as Application
            onCachedApplication(context, "callApplicationOnCreate")
        }

        /* fun hookApplication(method: String) {
             try {
                 Hooker.after(
                     applicationName,
                     method,
                     Context::class.java
                 ) {
                     val context = it.thisObject as Application
                     onCachedApplication(context, method)
                 }

             } catch (e: NoSuchMethodError) {
                 //   Logger.logE(TAG,e)
             }
         }

         for (method in arrayOf("attachBaseContext", "attach")) {
             // hookApplication(method)
         }*/
    }

    private fun checkIsTargetApp(
        pkg: String?,
        processName: String?,
        manifest: HookerManifest
    ): Boolean {
        if(pkg == null || processName == null){
            return false
        }
        //原始的匹配
        if (manifest.packageName == pkg && "${manifest.packageName}${manifest.processName}" == processName) {
            return true
        }
        //别名的匹配
        if (manifest.aliasPackageName.isNotEmpty() && manifest.aliasPackageName == pkg && "${manifest.aliasPackageName}${manifest.processName}" == processName) {
            return true
        }

        return false
    }

    /**
     * 加载包时的回调
     */
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        //判断是否为调试模式
        val pkg = lpparam.packageName
        val processName = lpparam.processName

        for (app in Apps.get()) {
            if (checkIsTargetApp(
                    pkg,
                    processName,
                    app)) {
                AppRuntime.classLoader = lpparam.classLoader
                AppRuntime.name = app.appName
                AppRuntime.manifest = app
                hookAppContext(app.applicationName) {
                    AppRuntime.application = it
                    if (it !== null) {
                        AppRuntime.classLoader = it.classLoader
                    }
                    AppRuntime.debug =
                        DataUtils.configBoolean(Setting.DEBUG_MODE, false) || BuildConfig.DEBUG
                    Logger.logD(
                        TAG,
                        "Hooker: ${app.appName}(${app.packageName}) Run in ${if (AppRuntime.debug) "debug" else "production"} Mode"
                    )
                    initHooker()
                }
                return
            }
        }

    }


    /**
     * 初始化Hooker
     * @param app HookerManifest
     * @return Boolean
     */
    private fun initHooker() {
        // 添加http访问支持
        Logger.log(
            TAG,
            "InitHooker: ${AppRuntime.name}, AutoVersion: ${BuildConfig.VERSION_NAME}, Application: ${AppRuntime.application?.applicationInfo?.sourceDir}"
        )
        AppRuntime.manifest.networkError()
        Logger.logD(TAG, "Allow Cleartext Traffic")
        //吐司框架初始化
        Toaster.init(AppRuntime.application)

        Logger.logD(TAG, "Toaster init success")
        // 检查所需的权限
        AppRuntime.manifest.permissionCheck()
        Logger.logD(TAG, "Permission check success")

        if (!AppRuntime.manifest.versionCheck()) {
            return
        }
       val rules =  AppRuntime.manifest.beforeAdaption()
        if (!AppRuntime.manifest.autoAdaption(rules)) {
            Logger.log(
                TAG,
                "Auto adaption failed , ${AppRuntime.manifest.appName} will not be hooked"
            )
            return
        }

        initHookers()

        AppRuntime.manifest.logD("Hooker init success, ${AppRuntime.manifest.appName}(${AppRuntime.versionCode})")




        if (
            !AppRuntime.manifest.systemApp &&
            AppRuntime.manifest.packageName !== BuildConfig.APPLICATION_ID &&
            DataUtils.configBoolean(Setting.LOAD_SUCCESS, DefaultData.LOAD_SUCCESS)
        ) {
            MessageUtils.toast("自动记账加载成功")
        }
    }

    /**
     * 初始化Hookers。
     *
     * 该方法负责初始化应用程序的核心Hook和区域Hook。
     *
     * 1. 首先，尝试初始化核心Hook。如果核心Hook初始化失败，将记录错误日志。
     * 2. 然后，遍历所有区域Hook，并尝试初始化每个区域Hook。
     *    - 对于每个区域Hook，首先尝试查找相关方法。如果查找失败，记录日志并跳过该Hook的初始化。
     *    - 如果方法查找成功，则继续执行Hook操作。
     *    - 如果Hook操作成功，记录成功日志；否则，记录错误日志，并将适应版本设置为0。
     *
     * 该方法在初始化过程中捕获并处理所有可能的异常，以确保应用程序的稳定性。
     */
    private fun initHookers() {
        // hook初始化
        runCatching {
            AppRuntime.manifest.hookLoadPackage()
        }.onFailure {
            // 核心初始化失败
            AppRuntime.manifest.logE(it)
        }

        // 区域hook初始化
        AppRuntime.manifest.partHookers.forEach {
            runCatching {
                AppRuntime.manifest.logD("PartHooker init: ${it.javaClass.simpleName}")
                it.hook()
                AppRuntime.manifest.logD("PartHooker init success: ${it.javaClass.simpleName}")
            }.onFailure {
                AppRuntime.manifest.logD("PartHooker error: ${it.message}")
                AppRuntime.manifest.logE(it)
                set("adaptation_version", "0")
            }
        }

    }


    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        AppRuntime.modulePath = startupParam?.modulePath ?: ""
    }


}