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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.crossbowffs.remotepreferences.RemotePreferences
import de.robv.android.xposed.XposedBridge
import net.ankio.auto.App
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.database.table.AccountMap
import net.ankio.auto.database.table.AppData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object ActiveUtils {
    fun getActiveAndSupportFramework(): Boolean {
        return false
    }
    fun errorMsg(context: Context):String{
        return context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }

    fun startApp(mContext:Context){
        val intent: Intent? = mContext.packageManager.getLaunchIntentForPackage("net.ankio.auto.xposed")
        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mContext.startActivity(intent)
        }
    }
    fun onStartApp(activity: Activity){

    }
    //仅Hook环境有效，所以此处不实现，交给hook
    fun get(key:String,app:String=BuildConfig.APPLICATION_ID): String {
        val sharedPreferences = App.context.getSharedPreferences("AutoAccounting.$app", Context.MODE_PRIVATE)
        return sharedPreferences.getString(key,"")?:""
    }
    //非hook环境
    fun put(key:String,value:String){
        val sharedPreferences = App.context.getSharedPreferences("AutoAccounting.${BuildConfig.APPLICATION_ID}", Context.MODE_PRIVATE)
        val result = sharedPreferences.edit().putString(key,value).commit()
        Log.e("写入结果 $key",result.toString())
    }
    //仅hook环境有效
    fun getAccountMap():List<AccountMap>{
      return arrayListOf()
    }

    /**
     * 非Hook环境
     * 获取日志，Xposed模式下，日志从目标app读取
     */
    fun getLogList(context: Context): String {
        var log = "";
        for (app in context.resources.getStringArray(R.array.xposed_scope)){
            log += get("log",app)
        }

        return sortLogs(log)
    }
    /**
     * 非Hook环境
     * 获取APP数据
     */
    fun getDataList(currentPage:Int,itemsPerPage:Int,context: Context,callback: (list: List<AppData>) -> Unit) {
        val list = arrayListOf<AppData>();
        for (app in context.resources.getStringArray(R.array.xposed_scope)){
            runCatching {
                val data = get("billData",app)
                for (line in data.lines()){
                    if(line.isNotEmpty()){
                        runCatching {
                            list.add(AppData.fromJSON(line))
                        }
                    }
                }
            }
        }
        callback(list.sortedBy { it.time })
    }
    private fun sortLogs(logString: String): String {
        val regex = Regex("\\[(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\]") // 正则表达式匹配日期和时间
        return logString.split("\n") // 将字符串分割成日志条目列表
            .map { it.trim().replace("___r_n","\n") } // 移除每行的首尾空白
            .filter { it.isNotBlank() } // 过滤掉空白行
            .sortedBy { regex.find(it)?.groups?.get(1)?.value } // 排序
            .joinToString("\n") // 用换行符重新连接
    }




}