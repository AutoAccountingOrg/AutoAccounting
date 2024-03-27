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
import android.content.Intent
import android.net.Uri
import com.google.gson.Gson
import de.robv.android.xposed.XposedBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.BuildConfig
import net.ankio.auto.HookMainApp
import net.ankio.auto.app.js.Engine
import net.ankio.auto.app.model.AppData


class HookUtils(private val context: Application, private val packageName: String) {

    private var autoAccountingServiceUtils: AutoAccountingServiceUtils

    init {
        AppUtils.setApplication(context)
        autoAccountingServiceUtils = AppUtils.setService(context)
    }

    /**
     * 判断自动记账目前是否处于调试模式
     */
    suspend fun isDebug():Boolean = withContext(Dispatchers.IO) {
        if (BuildConfig.DEBUG) {
             true
        }
       else  autoAccountingServiceUtils.get("debug") == "true"
    }

    //仅调试模式输出日志
    fun logD(prefix: String?, log: String) {
       scope.launch {
           isDebug().let {
               if (it) {
                   log(prefix, log)
               }
           }
       }
    }

    //正常输出日志
    fun log(prefix: String?, log: String) {
        Logger.d(log)
        scope.launch {
            autoAccountingServiceUtils.putLog("[自动记账] $prefix：$log")
            isDebug().let {
                if (it) {
                    XposedBridge.log("[自动记账] $prefix：$log") //xp输出
                }
            }
        }
    }

    private val job = Job()

    private val scope = CoroutineScope(Dispatchers.Main + job)

    fun cancel() {
        job.cancel()
    }

    fun analyzeData(dataType: Int, app: String, data: String, appName: String) {
        scope.launch {
            runCatching {
                log(HookMainApp.getTag(appName, "数据分析"), data)
                val billInfo = Engine.analyze(dataType, app, data)
                val appData = AppData()
                appData.issue = 0
                appData.type = dataType
                appData.rule = billInfo?.channel ?: ""
                appData.source = app
                appData.data = data
                appData.match = billInfo != null
                appData.time = System.currentTimeMillis()

                //先存到server的数据库里面
                val billData = appData.toText()

                autoAccountingServiceUtils.putData(billData)

                //从外部启动自动记账服务，这里需要处理队列问题

                logD(HookMainApp.getTag(appName, "自动记账结果"), appData.toJSON())
                if (billInfo !== null) {
                    // 创建一个Intent来启动目标应用程序
                    val intent = Intent("net.ankio.auto.ACTION_SHOW_FLOATING_WINDOW")
                    intent.setData(Uri.parse("autoaccounting://bill?data=${billInfo.toJSON()}"))
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    context.startActivity(intent)
                }

            }.onFailure {
                it.printStackTrace()
                it.message?.let { it1 ->
                    log(
                        HookMainApp.getTag(
                            appName,
                            "自动记账执行脚本发生错误"
                        ), it1
                    )
                }
            }
        }


    }

}