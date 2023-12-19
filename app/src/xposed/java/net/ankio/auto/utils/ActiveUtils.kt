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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.robv.android.xposed.XSharedPreferences
import net.ankio.auto.App
import net.ankio.auto.BuildConfig
import net.ankio.auto.database.table.AccountMap


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
    //仅Hook环境有效
    fun get(key:String): String {
        return try {
            val pref = XSharedPreferences(BuildConfig.APPLICATION_ID, "AutoAccounting")
            pref.reload()
            pref.getString(key,"")?:""
        }catch (e:Exception){
            //TODO 自动记账自己的日志功能
            ""
        }
    }
    //非hook环境
    fun put(key:String,value:String){
        val sharedPreferences = App.context.getSharedPreferences("AutoAccounting", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(key,value).apply()
    }
    //hook环境
    fun getAccountMap(name:String):List<AccountMap>{
        val string = get("AccountMap")
        if(string.isEmpty()){
            return arrayListOf()
        }
        return  Gson().fromJson(string, object : TypeToken<List<AccountMap?>?>() {}.type)
    }

}