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
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.security.NetworkSecurityPolicy
import android.widget.Toast
import com.google.gson.Gson
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import net.ankio.auto.Apps
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.logger.Logger
import net.ankio.dex.Dex


class App: IXposedHookLoadPackage, IXposedHookZygoteInit  {

    companion object{
        const val TAG = "HookerEnvironment"
        var modulePath = ""
        var application: Application? = null
        private val job  = Job()
        val scope = CoroutineScope(Dispatchers.IO + job)

        /**
         * 保存数据
         * @param key String
         * @param value String
         */
        fun set(key:String,value:String){
            if (application == null){
                return
            }
            Logger.logD(TAG,"set: $key -> $value")
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
        fun get(key:String,def:String = ""):String{
            if (application == null){
                return def
            }
            Logger.logD(TAG,"get: $key")
            val sharedPreferences: SharedPreferences =
                application!!.getSharedPreferences(TAG, Context.MODE_PRIVATE) // 私有数据
            return sharedPreferences.getString(key, def) ?: def
        }

        /**
         * 弹出提示
         * @param msg String
         */
        fun toast(msg: String) {
            if (application == null){
                return
            }
            Logger.logD(TAG,"toast: $msg")
            Toast.makeText(application, msg, Toast.LENGTH_LONG).show()
        }

        /**
         * 获取版本号
         * @return Int
         */
        fun getVersionCode(): Int {
            return runCatching {
                application!!.packageManager.getPackageInfo( application!!.packageName, 0).longVersionCode.toInt()
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
        fun hookAppContext(applicationName:String, classLoader: ClassLoader, callback: (Application?)->Unit){
            var hookStatus = false
            if (applicationName.isEmpty()){
                Logger.logD(TAG,"applicationName is empty")
                callback(null)
                return
            }

            fun onCachedApplication(application: Application) {
                hookStatus = true
                runCatching {
                    callback(application)
                }.onFailure {
                    Logger.log(TAG,"Hook error: ${it.message}")
                    Logger.logE(TAG,it)
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


    }


    /**
     * 加载包时的回调
     */
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        Logger.debug = true
        //判断是否为调试模式
        val pkg = lpparam.packageName
        val processName = lpparam.processName

        Logger.logD(TAG,"handleLoadPackage: $pkg，processName: $processName")

        for (app in Apps.get()){
            if (app.packageName == pkg && app.packageName == processName){
                networkError()
                hookAppContext(app.applicationName, lpparam.classLoader){
                    initHooker(app,it,lpparam.classLoader)
                }
                return
            }
        }

    }

    /**
     * 自动适配hook
     */
    private fun ruleHook(app:HookerManifest): Boolean {
        val code = getVersionCode()
        if (app.rules.size == 0) {
            return true
        }

        val adaptationVersion = get("adaptation","").toIntOrNull() ?: 0

        Logger.logD(TAG,"adaptationVersion: $adaptationVersion")

        if (adaptationVersion == code) {
            runCatching {
                app.clazz =
                    Gson().fromJson(
                        get("clazz",""),
                        HashMap::class.java,
                    ) as HashMap<String, String>
                if (app.clazz.size != app.rules.size) {
                    throw Exception("适配失败")
                }
            }.onFailure {
                set("adaptation", "0")
                Logger.logE(TAG,it)
            }.onSuccess {
                return true
            }
        }

        toast("自动记账开始适配中...")
        val path = application!!.packageResourcePath
        app.logD("App Package Path: $path")
        val total = app.rules.size
        val hashMap = Dex.findClazz(path, application!!.classLoader, app.rules)
        if (hashMap.size == total) {
            set("adaptation", code.toString())
            app.clazz = hashMap
            set("clazz", Gson().toJson(app.clazz))
            app.logD(" 适配成功:$hashMap")
            toast("适配成功")
            return true
        } else {
            app.logD(" 适配失败:$hashMap")
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
    private fun initHooker(app:HookerManifest,application: Application?,classLoader: ClassLoader){
        Logger.log(TAG,"initHooker: ${app.appName}")
        App.application = application

        val code = getVersionCode()

        if (!ruleHook(app)){
            Logger.log(TAG,"autoAdaption failed , ${app.appName}(${code}) will not be hooked")
            return
        }

        permissionCheck(app)

        app.hookLoadPackage(application,classLoader)

        app.partHookers.forEach {
            runCatching {
                app.logD("PartHooker init: ${it.javaClass.simpleName}")
                it.hook(app,application,classLoader)
                app.logD("PartHooker init success: ${it.javaClass.simpleName}")
            }.onFailure {
               app.logD("PartHooker error: ${it.message}")
               app.logE(it)
            }
        }

        app.logD("Hooker init success, ${app.appName}(${code})")
    }

    /**
     * 权限检查
     * 确保已经成功授权
     */
    private fun permissionCheck(app: HookerManifest){
        val permissions = app.permissions
        if (permissions.isEmpty()){
            app.logD("${app.appName} No permission required")
            return
        }
        val context = application ?: return
        permissions.forEach {
            if (context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED){
                app.logD("${app.appName} Permission denied: $it")
            } else{
                app.logD("${app.appName} Permission granted: $it")
            }
        }
    }

    /**
     * 网络错误修复
     * 修复Android9及以上版本的网络错误
     */
    private fun networkError(){
        Logger.logD(TAG,"networkError Fix")
        val policy = NetworkSecurityPolicy.getInstance()
        if (policy != null && !policy.isCleartextTrafficPermitted) {
            // 允许明文流量
            XposedHelpers.callMethod(policy, "setCleartextTrafficPermitted", true)
            Logger.log(TAG, "allow CleartextTraffic:" + policy.isCleartextTrafficPermitted)
        }
    }



    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        modulePath = startupParam?.modulePath ?: ""
    }



}