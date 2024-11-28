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
import android.content.Context
import android.content.pm.PackageManager
import android.security.NetworkSecurityPolicy
import com.google.gson.Gson
import com.hjq.toast.Toaster
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import net.ankio.auto.BuildConfig
import net.ankio.auto.xposed.Apps
import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.logger.Logger
import net.ankio.auto.xposed.core.utils.AppUtils.getVersionCode
import net.ankio.auto.xposed.core.utils.AppUtils.getVersionName
import net.ankio.auto.xposed.core.utils.DataUtils
import net.ankio.auto.xposed.core.utils.DataUtils.get
import net.ankio.auto.xposed.core.utils.DataUtils.set
import net.ankio.auto.xposed.core.utils.MessageUtils.toast
import net.ankio.auto.xposed.hooks.common.JsEngine
import net.ankio.auto.xposed.hooks.common.UnLockScreen
import net.ankio.dex.Dex
import org.ezbook.server.Server
import org.ezbook.server.constant.Setting


class App : IXposedHookLoadPackage, IXposedHookZygoteInit {

    companion object {
        const val TAG = "HookerEnvironment"
        var modulePath = ""
        var application: Application? = null
        var name  = ""

    }
    /**
     * hook Application Context
     * @param applicationName String
     * @param classLoader ClassLoader
     * @param callback Function1<Application?, Unit>
     *     回调函数，返回Application对象
     *     如果hook失败，返回null
     */
    private fun hookAppContext(
        applicationName: String,
        classLoader: ClassLoader,
        callback: (Application?) -> Unit
    ) {
        var hookStatus = false
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


        fun hookApplication(method:String){
            try {
                Hooker.after(
                    applicationName,
                    method,
                    Context::class.java
                ){
                    val context = it.thisObject as Application
                    onCachedApplication(context,method)
                }

            }catch (e:NoSuchMethodError){
             //   Logger.logE(TAG,e)
            }
        }

        for (method in arrayOf("attachBaseContext", "attach")) {
            hookApplication(method)
        }
    }

    /**
     * 加载包时的回调
     */
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        //判断是否为调试模式
        val pkg = lpparam.packageName
        val processName = lpparam.processName
        Logger.debug = DataUtils.configBoolean(Setting.DEBUG_MODE, false) || BuildConfig.DEBUG
        Logger.logD(TAG, "handleLoadPackage: $pkg，processName: $processName")

        for (app in Apps.get()) {
            if (app.packageName == pkg && app.packageName == processName) {
                Hooker.setClassLoader(lpparam.classLoader)
                Logger.logD(TAG,"Hooker: ${app.appName}(${app.packageName}) Run in ${if(Logger.debug) "debug" else "production"} Mode")
                name = app.appName
                hookAppContext(app.applicationName, lpparam.classLoader) {
                    initHooker(app, it, lpparam.classLoader)
                }
                return
            }
        }

    }

    /**
     * 自动适配hook
     */
    private fun ruleHook(app: HookerManifest): Boolean {

        if (app.rules.size == 0) {
            return true
        }

        val adaptationVersion = get("adaptation", "").toIntOrNull() ?: 0

        Logger.logD(TAG, "AdaptationVersion: $adaptationVersion")
        val code = getVersionCode()
        if (adaptationVersion == code) {
            runCatching {
                app.clazz =
                    Gson().fromJson(
                        get("clazz", ""),
                        HashMap::class.java,
                    ) as HashMap<String, String>
                if (app.clazz.size != app.rules.size) {
                    throw Exception("Adaptation failed")
                }
            }.onFailure {
                set("adaptation", "0")
                Logger.logE(TAG, it)
            }.onSuccess {
                app.log("Adaptation Info:${app.clazz}")
                return true
            }
        }

        toast("自动记账开始适配中...")
        val appInfo = application!!.applicationInfo
        val path = appInfo.sourceDir
        app.beforeAdapter(application!!,path)
        app.logD("App Package Path: $path")
        val total = app.rules.size
        val hashMap = Dex.findClazz(path, application!!.classLoader, app.rules)
        if (hashMap.size == total) {
            set("adaptation", code.toString())
            app.clazz = hashMap
            set("clazz", Gson().toJson(app.clazz))
            app.log("Adaptation success:$hashMap")
            toast("适配成功")
            return true
        } else {
            app.logD("Adaptation failed: $hashMap")
            for (clazz in app.rules) {
                if (!hashMap.containsKey(clazz.name)) {
                    app.logD("Failed to adapt:${clazz.name}")
                }
            }

            toast("适配失败")
            return false
        }
    }



    /**
     * 初始化Hooker
     * @param app HookerManifest
     * @param application Application?
     * @param classLoader ClassLoader
     * @return Boolean
     */
    private fun initHooker(
        app: HookerManifest,
        application: Application?,
        classLoader: ClassLoader
    ) {
        Hooker.setClassLoader(classLoader)
        // 添加http访问支持
        Logger.log(TAG, "InitHooker: ${app.appName}, AutoVersion: ${BuildConfig.VERSION_NAME}, Application: ${application?.applicationInfo?.sourceDir}")
        networkError()
        Logger.logD(TAG,"Allow Cleartext Traffic")
        //吐司框架初始化
        Toaster.init(application)
        Logger.logD(TAG,"Toaster init success")
        // 检查所需的权限
        permissionCheck(app)
        Logger.logD(TAG,"Permission check success")

        Companion.application = application



        val code = getVersionCode()

        val name = getVersionName()

        Logger.log(TAG, "App VersionCode: $code, VersionName: $name")

        // 检查App版本是否过低，过低无法使用
        if (app.minVersion != 0 && code < app.minVersion) {
            Logger.log(TAG, "Auto adaption failed , ${app.appName}(${code}) version is too low")
            toast("${app.appName}版本过低，无法适配，请升级到最新版本后再试。")
            return
        }

        // 自动适配
        runCatching {
            if (!ruleHook(app)) {
                Logger.log(TAG, "Auto adaption failed , ${app.appName}(${code}) will not be hooked")
                return
            }
        }.onFailure {
            Logger.logE(TAG, it)
            toast("自动适配失败！")
            return
        }

        //注入版本信息
        Server.versionName = BuildConfig.VERSION_NAME

        // 启动自动记账服务
        if (app.packageName === Apps.getServerRunInApp()){
            startServer(application)
        }

        // hook初始化
        runCatching {
            app.hookLoadPackage(application, classLoader)
        }.onFailure {
            // 核心初始化失败
            app.logE(it)
        }

        // 区域hook初始化
        app.partHookers.forEach {
            runCatching {
                app.logD("PartHooker init: ${it.javaClass.simpleName}")
                if (!it.findMethods(classLoader, app)) {
                    app.logD("PartHooker init failed: ${it.javaClass.simpleName}")
                    return@runCatching
                }
                it.hook(app, application, classLoader)
                app.logD("PartHooker init success: ${it.javaClass.simpleName}")
            }.onFailure {
                app.logD("PartHooker error: ${it.message}")
                app.logE(it)
                set("adaptation", "0")
            }
        }

        app.logD("Hooker init success, ${app.appName}(${code})")
    }

    /**
     * 权限检查
     * 确保已经成功授权
     */
    private fun permissionCheck(app: HookerManifest) {
        val permissions = app.permissions
        if (permissions.isEmpty()) {
            return
        }
        val context = application ?: return
        permissions.forEach {
            if (context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED) {
                app.logE(Throwable("${app.appName} Permission denied: $it , this hook may not work as expected"))
            }
        }
    }

    /**
     * 启动自动记账服务
     */
    private fun startServer(
        application: Application?
    ) {
        Logger.logD(TAG, "Start server...: ${AndroidAppHelper.currentPackageName()}")
        try {
            JsEngine.init()
            UnLockScreen.init(application!!)
            Server(application).startServer()
            Logger.logD(TAG, "Server start success")
        } catch (e: Throwable) {
            XposedBridge.log("Server start failed")
            XposedBridge.log(e)
            Logger.logD(TAG,e.message?:"")
        }
    }




    /**
     * 网络错误修复
     * 修复Android9及以上版本的网络错误
     */
    private fun networkError() {
        val policy = NetworkSecurityPolicy.getInstance()
        if (policy != null && !policy.isCleartextTrafficPermitted) {
            // 允许明文流量
            XposedHelpers.callMethod(policy, "setCleartextTrafficPermitted", true)
        }
    }


    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        modulePath = startupParam?.modulePath ?: ""
    }


}