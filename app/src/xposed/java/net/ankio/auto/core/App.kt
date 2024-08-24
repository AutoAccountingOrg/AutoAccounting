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
import android.icu.lang.UCharacter.GraphemeClusterBreak.L
import android.os.Build
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


    }






    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        Logger.debug = true
        //判断是否为调试模式
        val pkg = lpparam.packageName
        val processName = lpparam.processName

        Logger.log(TAG,"handleLoadPackage: $pkg，processName: $processName")

        for (app in Apps.get()){
            if (app.packageName == pkg && app.packageName == processName){
                networkError()
                hookAppContext(app, lpparam.classLoader)
                return
            }
        }

    }

    private fun ruleHook(app:HookerManifest): Boolean {
        val code = getVersionCode()
        if (app.rules.size == 0) {
            return true
        }
        val adaptationVersion = get("adaptation","").toIntOrNull() ?: 0
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
                XposedBridge.log(it)
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
    private fun initHooker(app:HookerManifest,application: Application?,classLoader: ClassLoader){
        Logger.log(TAG,"initHooker: ${app.appName}")
        App.application = application

        val code = getVersionCode()

        if (!ruleHook(app)){
            Logger.log(TAG,"autoAdaption failed , ${app.appName}(${code}) will not be hooked")
            return
        }

        app.hookLoadPackage(application,classLoader)

        app.partHookers.forEach {
            runCatching {
                it.hook(app,application,classLoader)
            }.onFailure {
               app.logD("Hooker error: ${it.message}")
                app.logE(it)
            }
        }

        app.logD("Hooker init success, ${app.appName}(${code})")
    }



    private fun networkError(){
        val policy = NetworkSecurityPolicy.getInstance()
        if (policy != null && !policy.isCleartextTrafficPermitted) {
            // 允许明文流量
            XposedHelpers.callMethod(policy, "setCleartextTrafficPermitted", true)
            Logger.log(TAG, "allow CleartextTraffic:" + policy.isCleartextTrafficPermitted)
        }
    }

    private fun hookAppContext(app:HookerManifest,classLoader: ClassLoader){
        var hookStatus = false
        if (app.applicationName.isEmpty()){
            Logger.log(TAG,"applicationName is empty")
            initHooker(app, null,classLoader)
            return
        }

        fun onCachedApplication(application: Application) {
            hookStatus = true
            runCatching {
                initHooker(app, application,classLoader)
            }.onFailure {
                Logger.log(TAG,"Hook error: ${it.message}")
                Logger.logE(TAG,it)
            }
        }

        runCatching {
            XposedHelpers.findAndHookMethod(
                app.applicationName,
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
                    app.applicationName,
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



    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        modulePath = startupParam?.modulePath ?: ""
    }



}