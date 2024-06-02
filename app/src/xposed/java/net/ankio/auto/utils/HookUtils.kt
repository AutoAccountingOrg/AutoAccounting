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

class HookUtils(val context: Application, private val packageName: String) {
    private val loadClazz = HashMap<String, Class<*>>()

    init {
        XposedHelpers.callMethod(
            context.resources.assets,
            "addAssetPath",
            HookMainApp.modulePath,
        )

        AppUtils.setApplication(context)
        AppUtils.getService().connect()
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
    suspend fun logD(
        prefix: String?,
        log: String,
    ) = withContext(Dispatchers.IO) {
        if (isDebug()) {
            log(prefix ?: "", log)
        }
    }

    // 正常输出日志
    suspend fun log(
        prefix: String,
        log: String,
    ) = withContext(Dispatchers.IO) {
        runCatching {
            LogModel.put(
                LogModel().apply {
                    this.app = packageName
                    this.hook = 1
                    this.date = DateUtils.getTime(System.currentTimeMillis())
                    this.log = log
                    this.level = LogModel.LOG_LEVEL_INFO
                    this.thread = Thread.currentThread().name
                    this.line = prefix
                },
            )
        }.onFailure {
            XposedBridge.log(it)
            startAutoApp(AutoServiceException(it.message.toString()), context)
        }
        XposedBridge.log("[自动记账]$prefix：$log") // xp输出
    }

    private val job = Job()

    val scope = CoroutineScope(Dispatchers.Main + job)

    fun cancel() {
        job.cancel()
    }

    suspend fun analyzeData(
        dataType: Int,
        app: String,
        data: String,
        appName: String,
    ) = withContext(Dispatchers.IO) {
        runCatching {
            log(HookMainApp.getTag(appName, "数据分析"), data)
            Engine.analyze(dataType, app, data, true)
        }.onFailure {
            it.printStackTrace()
            it.message?.let { it1 ->
                log(
                    HookMainApp.getTag(
                        appName,
                        "自动记账执行脚本发生错误",
                    ),
                    it1,
                )
            }
            XposedBridge.log(it)
        }
    }

    fun getVersionCode(): Int {
        return runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        }.getOrElse {
            scope.launch {
                it.message?.let { it1 -> log("获取版本号失败", it1) }
            }
            0
        }
    }

    private val TAG = "AutoAccounting"

    fun writeData(
        key: String,
        value: String,
    ) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(TAG, Context.MODE_PRIVATE) // 私有数据

        val editor = sharedPreferences.edit() // 获取编辑器

        editor.putString(key, value)

        editor.apply() // 提交修改
    }

    fun readData(key: String): String {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(TAG, Context.MODE_PRIVATE) // 私有数据
        return sharedPreferences.getString(key, "") ?: ""
    }

    fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
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
