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

package net.ankio.auto.core

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.security.NetworkSecurityPolicy
import android.widget.Toast
import com.google.gson.Gson
import com.hjq.toast.Toaster
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.ankio.auto.App.Companion.app
import net.ankio.auto.Apps
import net.ankio.auto.BuildConfig
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.logger.Logger
import net.ankio.auto.ui.activity.MainActivity
import net.ankio.dex.Dex
import org.ezbook.server.Server
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.SettingModel
import java.math.BigInteger
import java.security.MessageDigest


class App : IXposedHookLoadPackage, IXposedHookZygoteInit {

    companion object {
        const val TAG = "HookerEnvironment"
        var modulePath = ""
        var application: Application? = null
        private val job = Job()
        val scope = CoroutineScope(Dispatchers.IO + job)

        fun launch(block: suspend CoroutineScope.() -> Unit) {
            scope.launch {
                runCatching {

                    block()

                }.onFailure {
                    Logger.logE(TAG, it)
                }
            }
        }


        /**
         * 保存数据
         * @param key String
         * @param value String
         */
        fun set(key: String, value: String) {
            if (application == null) {
                return
            }
            val sharedPreferences: SharedPreferences =
                application!!.getSharedPreferences(TAG, Context.MODE_PRIVATE) // 私有数据
            val editor = sharedPreferences.edit() // 获取编辑器
            editor.putString(key, value)
            editor.apply() // 提交修改
        }

        /**
         * 读取数据
         * @param key String
         * @return String
         */
        fun get(key: String, def: String = ""): String {
            if (application == null) {
                return def
            }
            val sharedPreferences: SharedPreferences =
                application!!.getSharedPreferences(TAG, Context.MODE_PRIVATE) // 私有数据
            return sharedPreferences.getString(key, def) ?: def
        }

        /**
         * md5哈希
         */
        fun md5(data: String): String {
            val md = MessageDigest.getInstance("MD5")
            return BigInteger(1, md.digest(data.toByteArray())).toString(16).padStart(32, '0')
        }

        /**
         * 弹出提示
         * @param msg String
         */
        fun toast(msg: String) {
            if (application == null) {
                return
            }
            try {
                Toaster.show(msg)
            } catch (e: Throwable) {
                Toast.makeText(application, msg, Toast.LENGTH_LONG).show()
            }finally {
                Logger.log(TAG, msg)
            }


        }


        /**
         * 获取版本号
         * @return Int
         */
        fun getVersionCode(): Int {
            return runCatching {
                application!!.packageManager.getPackageInfo(
                    application!!.packageName,
                    0
                ).longVersionCode.toInt()
            }.getOrElse {
                0
            }
        }

        /**
         * 在主线程运行
         * @param function Function0<Unit>
         */
        fun runOnUiThread(function: () -> Unit) {
            if (Looper.getMainLooper().thread != Thread.currentThread()) {
                Handler(Looper.getMainLooper()).post { function() }
            } else {
                function()
            }
        }

        /**
         * hook Application Context
         * @param applicationName String
         * @param classLoader ClassLoader
         * @param callback Function1<Application?, Unit>
         *     回调函数，返回Application对象
         *     如果hook失败，返回null
         */
        fun hookAppContext(
            applicationName: String,
            classLoader: ClassLoader,
            callback: (Application?) -> Unit
        ) {
            var hookStatus = false
            if (applicationName.isEmpty()) {
                Logger.logD(TAG, "Application name is empty")
                callback(null)
                return
            }

            fun onCachedApplication(application: Application) {
                hookStatus = true
                runCatching {
                    callback(application)
                }.onFailure {
                    Logger.log(TAG, "Hook error: ${it.message}")
                    Logger.logE(TAG, it)
                }
            }

            runCatching {
                XposedHelpers.findAndHookMethod(
                    applicationName,
                    classLoader,
                    "attach",
                    Context::class.java,
                    object : XC_MethodHook() {
                        @Throws(Throwable::class)
                        override fun afterHookedMethod(param: MethodHookParam) {
                            super.afterHookedMethod(param)
                            if (hookStatus) {
                                return
                            }

                            val context = param.thisObject as Application

                            onCachedApplication(context)
                        }
                    },
                )
            }.onFailure {
                runCatching {
                    XposedHelpers.findAndHookMethod(
                        applicationName,
                        classLoader,
                        "attachBaseContext",
                        Context::class.java,
                        object : XC_MethodHook() {
                            @Throws(Throwable::class)
                            override fun afterHookedMethod(param: MethodHookParam) {
                                super.afterHookedMethod(param)
                                if (hookStatus) {
                                    return
                                }

                                val context = param.thisObject as Application

                                onCachedApplication(context)
                            }
                        },
                    )
                }
            }
        }

        fun restart() {
            if (application == null)return
            val intent = application!!.packageManager.getLaunchIntentForPackage(application!!.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_NEW_TASK)
            application!!.startActivity(intent)
            Process.killProcess(Process.myPid())
        }


    }


    /**
     * 加载包时的回调
     */
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        //判断是否为调试模式
        val pkg = lpparam.packageName
        val processName = lpparam.processName

       // Logger.logD(TAG, "handleLoadPackage: $pkg，processName: $processName")

        for (app in Apps.get()) {
            if (app.packageName == pkg && app.packageName == processName) {
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
        val code = getVersionCode()
        if (app.rules.size == 0) {
            return true
        }

        val adaptationVersion = get("adaptation", "").toIntOrNull() ?: 0

        Logger.logD(TAG, "AdaptationVersion: $adaptationVersion")

        if (adaptationVersion == code) {
            runCatching {
                app.clazz =
                    Gson().fromJson(
                        get("clazz", ""),
                        HashMap::class.java,
                    ) as HashMap<String, String>
                if (app.clazz.size != app.rules.size) {
                    throw Exception("需要重新适配...")
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

    private fun  setLogger(){
        launch {
            Logger.debug = SettingModel.get(Setting.DEBUG_MODE, "true").toBoolean()
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
        // 添加http访问支持
        networkError()
        // 日志初始化
        setLogger()
        //吐司框架初始化
        Toaster.init(application)
        // 检查所需的权限
        permissionCheck(app)

        Logger.log(TAG, "InitHooker: ${app.appName}, AutoVersion: ${BuildConfig.VERSION_NAME}, Application: ${application?.applicationInfo?.sourceDir}")
        App.application = application



        val code = getVersionCode()

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


        // 将自动记账的资源路径加入到查找路径中
        XposedHelpers.callMethod(
            application!!.resources.assets,
            "addAssetPath",
            modulePath,
        )



        // 启动自动记账服务
        if (app.packageName === Apps.getServerRunInApp()){
            startServer(app,application)
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
        hookerManifest: HookerManifest,
        application: Application?
    ) {
        try {
            hookerManifest.logD("Try start server...")
            Server(application!!).startServer()
            hookerManifest.logD("Server start success")
        } catch (e: Exception) {
            XposedBridge.log("Server start failed")
            XposedBridge.log(e)
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