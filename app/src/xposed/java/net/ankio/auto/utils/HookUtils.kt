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
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.BuildConfig
import net.ankio.auto.HookMainApp
import net.ankio.auto.app.js.Engine
import net.ankio.auto.exceptions.AutoServiceException
import net.ankio.auto.utils.server.model.LogModel

object HookUtils {
    private val loadClazz = HashMap<String, Class<*>>()

    fun setApplication(application: Application) {
        addAutoContext(application)
       AppUtils.setApplication(application)
        XposedBridge.hookAllMethods(
            ClassLoader::class.java,
            "loadClass",
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (param.hasThrowable()) return
                    if (param.args.size != 1) return

                    val cls = param.result as Class<*>
                    val name = cls.name
                    loadClazz[name] = cls
                }
            },
        )
    }

    fun addAutoContext(context: Context) {
        XposedHelpers.callMethod(
            context.resources.assets,
            "addAssetPath",
            HookMainApp.modulePath,
        )
    }

    fun startAutoApp(
        e: Throwable,
        application: Application,
    ): Boolean {
        var result = false
        if (e is AutoServiceException) {
            // 不在自动记账跳转
            if (application.packageName != BuildConfig.APPLICATION_ID) {
                ActiveUtils.startApp(application)
            }
            result = true
        }
        return result
    }

    /**
     * TODO 公测结束后需要使用正常模式
     * 判断自动记账目前是否处于调试模式
     */
    suspend fun isDebug(): Boolean = true
       /* withContext(Dispatchers.IO) {
            if (BuildConfig.DEBUG) {
                true
            } else {
                runCatching {
                    autoAccountingServiceUtils.get("debug") == "true"
                }.getOrNull() ?: false
            }
        }*/

    // 仅调试模式输出日志
    fun logD(
        prefix: String?,
        log: String,
    )  {
       AppUtils.getScope().launch {
           if (isDebug()) {
               log(prefix ?: "", log)
           }else{
               Logger.d("$prefix: $log")
           }
       }
    }

    // 正常输出日志
    fun log(
        prefix: String,
        log: String,
    )  {

        AppUtils.getScope().launch {
            Logger.i("$prefix: $log")
            XposedBridge.log("$prefix：$log") // xp输出
        }
    }

    suspend fun analyzeData(
        dataType: Int,
        app: String,
        data: String,
        appName: String,
    ) = withContext(Dispatchers.IO) {
        runCatching {
            logD("数据分析", data)
            Engine.analyze(dataType, app, data, true)
        }.onFailure {
            it.printStackTrace()
            it.message?.let { it1 ->
                log(
                    "自动记账执行脚本发生错误",
                    it1,
                )
            }
        }
    }

    fun getVersionCode(): Int {
        return runCatching {
            AppUtils.getApplication().packageManager.getPackageInfo( AppUtils.getApplication().packageName, 0).longVersionCode.toInt()
        }.getOrElse {
            0
        }
    }

    private val TAG = "AutoAccounting"

    fun writeData(
        key: String,
        value: String,
    ) {
        val sharedPreferences: SharedPreferences =
            AppUtils.getApplication().getSharedPreferences(TAG, Context.MODE_PRIVATE) // 私有数据

        val editor = sharedPreferences.edit() // 获取编辑器

        editor.putString(key, value)

        editor.apply() // 提交修改
    }

    fun readData(key: String): String {
        val sharedPreferences: SharedPreferences =
            AppUtils.getApplication().getSharedPreferences(TAG, Context.MODE_PRIVATE) // 私有数据
        return sharedPreferences.getString(key, "") ?: ""
    }

    fun toast(msg: String) {
        Toast.makeText(AppUtils.getApplication(), msg, Toast.LENGTH_LONG).show()
    }

    /**
     * 加载clazz类
     */
    suspend fun loadClass(
        name: String,
        count: Int = 0,
    ): Class<*> =
        withContext(Dispatchers.IO) {
            var clazz = loadClazz[name]
            if (clazz == null) {
                Thread.sleep((count * 1000).toLong())
                if (count > 30) {
                    throw ClassNotFoundException("加载类（$name）失败")
                }
                clazz = loadClass(name, count + 1)
            }
            clazz
        }
}
