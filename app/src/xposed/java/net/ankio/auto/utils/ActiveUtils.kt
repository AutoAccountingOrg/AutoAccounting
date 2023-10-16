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
import com.google.gson.JsonArray
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.runBlocking
import net.ankio.auto.database.Db
import net.ankio.auto.database.table.AccountMap
import net.ankio.auto.database.table.AppData
import net.ankio.auto.hooks.android.AccountingService
import net.ankio.auto.ui.activity.RestartActivity


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
        val service = AccountingService.get()
        if(service==null){
            val intent = Intent(activity, RestartActivity::class.java)
            activity.startActivity(intent)
            return
        }

      runBlocking {
          val data = service.syncData()

          val jsonArray = Gson().fromJson(data,JsonArray::class.java) ?: return@runBlocking
          for (string in jsonArray){
              Db.get().AppDataDao().insert(AppData.fromJSON(string.asString))
          }
      }
    }

    fun get(key:String):String{
        return AccountingService.get()?.get(key) ?: ""
    }

    fun put(key:String,value:String){
        AccountingService.get()?.put(key,value)
    }

    fun getAccountMap(name:String):List<AccountMap>{
        val string = AccountingService.get()?.getMap(name) ?:""
        if(string.isEmpty()){
            return arrayListOf()
        }
        return  Gson().fromJson(string, object : TypeToken<List<AccountMap?>?>() {}.type)
    }

}