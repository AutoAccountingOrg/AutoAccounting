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

import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import de.robv.android.xposed.XposedBridge
import net.ankio.auto.BuildConfig
import net.ankio.auto.HookMainApp
import net.ankio.auto.IAccountingService
import net.ankio.auto.app.Engine
import net.ankio.auto.database.table.AppData
import net.ankio.auto.hooks.android.AccountingService


class HookUtils(private val context: Context?,private val packageName:String) {

    companion object {
        const val TAG = "AutoAccounting"

    }
    private val service: IAccountingService? = AccountingService.get()

    fun getSp(key:String): String {
        return service?.get(key) ?:""
    }

   private fun putSp(key: String, data: String){
       service?.put(key,data)
    }

    /**
     * 判断自动记账目前是否处于调试模式
     */
    fun isDebug(): Boolean {
        return BuildConfig.DEBUG || getSp("debug") == "true";
    }
    //仅调试模式输出日志
    fun logD(prefix: String?, log: String){
        if(!isDebug()){ return }
        log(prefix,log)
    }
    //正常输出日志
    fun log(prefix: String?, log: String) {

        service?.log(prefix,log)

        if (isDebug()) {
            XposedBridge.log("[自动记账] $prefix：$log") //xp输出
        }
    }

    private fun getLastLine(string: String, line: Int = 200): String {
        return string.lines().takeLast(line).joinToString("\n")
    }
    fun analyzeData(dataType: Int, app: String, data: String) {
        runCatching {
                log(HookMainApp.getTag(app,"数据分析"), data)
                val billInfo = Engine.runAnalyze(dataType, app, data,this@HookUtils)
                val appData = AppData()
                appData.issue = 0
                appData.type = dataType
                appData.rule = billInfo?.channel ?: ""
                appData.source = app
                appData.data = data
                appData.match = billInfo != null
                appData.time = System.currentTimeMillis()
                val key = "billData"
                //先存到server的数据库里面
                var billData = getSp(key)
                billData+="\n"+Gson().toJson(appData).replace("\n","___r_n")
                putSp(key,getLastLine(billData,100))
                if (billInfo !== null) {
                    val intent = Intent().apply {
                        setClassName(BuildConfig.APPLICATION_ID, "net.ankio.auto.ui.activity.FloatingWindowTriggerActivity")
                        putExtra("data", Gson().toJson(billInfo))
                    }
                    intent.setAction("net.ankio.auto.ACTION_SHOW_FLOATING_WINDOW")
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    context?.startActivity(intent)
                }
        }.onFailure {
            it.printStackTrace()
            it.message?.let { it1 -> log(HookMainApp.getTag(app,"自动记账执行脚本发生错误"), it1) }
        }

    }
    fun onExitApp(){

    }
}