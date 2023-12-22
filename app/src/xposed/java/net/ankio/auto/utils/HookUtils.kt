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
import android.content.SharedPreferences
import com.crossbowffs.remotepreferences.RemotePreferenceAccessException
import com.crossbowffs.remotepreferences.RemotePreferences
import com.google.gson.Gson
import de.robv.android.xposed.XposedBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.BuildConfig
import net.ankio.auto.HookMainApp
import net.ankio.auto.app.Engine
import net.ankio.auto.database.table.AppData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class HookUtils(private val context: Context,private val packageName:String) {

    companion object{
        val TAG = "AutoAccounting"
    }

    private fun getSharedPreferences(pkg:String = BuildConfig.APPLICATION_ID): SharedPreferences? {
        if(pkg===BuildConfig.APPLICATION_ID && packageName===pkg){
            return context.getSharedPreferences("$TAG.${pkg}",Context.MODE_PRIVATE);
        }
        return  RemotePreferences(context, "net.ankio.auto.utils.SharePreferenceProvider", "$TAG.${pkg}",true)
    }

    /**
     * 获取自动记账配置
     */
    fun getConfig(key: String): String {
        return getSharedPreferences(BuildConfig.APPLICATION_ID)?.getString(key,"")?:""
    }

    private fun getSp(key:String): String {
        return getSharedPreferences(packageName)?.getString(key,"") ?:""
    }

   private fun putSp(key: String, data: String){
       try {
            getSharedPreferences(packageName)?.edit()?.putString(key,data)?.commit()
       }catch (e: RemotePreferenceAccessException){
           XposedBridge.log(e)
       }
    }

    /**
     * 判断自动记账目前是否处于调试模式
     */
    fun isDebug(): Boolean {
        return getConfig("debug") == "true";
    }
    //仅调试模式输出日志
    fun logD(prefix: String?, log: String){
        if(!isDebug()  && !BuildConfig.DEBUG ){ return }
        log(prefix,log)
    }
    //正常输出日志
    fun log(prefix: String?, log: String) {
        val key = "log"
        var logInfo = getSp(key)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        val currentTime = Date()
        val logMessage = "[${dateFormat.format(currentTime)}]$prefix${log.replace("\n","___r_n")}\n"
        logInfo+=logMessage
        putSp(key,getLastLine(logInfo,200))
        if(isDebug() || BuildConfig.DEBUG ){
            val printLog = "[自动记账] $prefix：$log"
            XposedBridge.log(printLog) //xp输出
        }
    }

    private fun getLastLine(string: String,line:Int = 200): String {
        val lines = string.lines()
        if (lines.size > line) {
            return lines.takeLast(line).joinToString("\n")
        }
        return string
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
                billData+=Gson().toJson(appData)
                putSp(key,getLastLine(billData,100))
                if (billInfo !== null) {
                    // TODO 拉起自动记账的Activity
                }


        }.onFailure {
            it.printStackTrace()
            it.message?.let { it1 -> log(HookMainApp.getTag(app,"自动记账执行脚本发生错误"), it1) }
        }

    }
}