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

package net.ankio.auto.hooks.android

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.IBinder
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.google.gson.Gson
import de.robv.android.xposed.XposedBridge
import kotlinx.coroutines.runBlocking
import net.ankio.auto.HookMainApp
import net.ankio.auto.IAccountingService
import net.ankio.auto.app.Engine
import net.ankio.auto.constant.toDataType
import net.ankio.auto.database.AppDatabase
import net.ankio.auto.database.table.AccountMap
import net.ankio.auto.database.table.AppData
import net.ankio.auto.utils.toJsonArray
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class AccountingService(val mContext: Context) : IAccountingService.Stub() {

    private val SERVICE_NAME = "accounting.service"

    private val TAG = "AutoAccountingService"

    fun getServiceName(): String {
        return "user.$SERVICE_NAME"
    }

    private lateinit var db: DbHelper

    fun systemReady() {
        log(HookMainApp.getTag(TAG), "Welcome to AutoAccounting.")
        // 删除日志
        val logFile = File(logFileName)
        if (logFile.exists()) {
            logFile.delete()
        }
        log(HookMainApp.getTag(TAG), "Removed old logs：$logFile")
        //写入启动日志
        log(HookMainApp.getTag(TAG), "AutoAccounting  Service Start  ")
        //数据库初始化放到service中来

        db = DbHelper(mContext,"$dataDir/database.db")

        log(HookMainApp.getTag(TAG), "AutoAccounting Db Init  ")

    }

    companion object {
        private var mService: IAccountingService? = null
        fun get(): IAccountingService? {
            try {
                if (mService == null) {
                    val SERVICE_NAME = "user.accounting.service"
                    val svcManager = Class.forName("android.os.ServiceManager")
                    val getServiceMethod: Method =
                        svcManager.getDeclaredMethod("getService", String::class.java)
                    mService = asInterface(getServiceMethod.invoke(null, SERVICE_NAME) as IBinder)
                }
                return mService
            } catch (e: Exception) {
                println(e.message)
                e.printStackTrace()
//                XposedBridge.log(e)
            }
            return null
        }

        const val dataDir = "/data/system/net.ankio.auto.xposed"
        const val logFileName = "$dataDir/auto-log.txt"

    }


    /**
     * 初始化创建文件夹子
     */

    private fun initFile(file: File) {
        if (!file.exists()) {
            val parentDirectory = file.parentFile
            if (parentDirectory != null && !parentDirectory.exists()) {
                // 递归创建目录
                parentDirectory.mkdirs()
            }
            file.createNewFile()
        }
    }

    override fun log(prefix: String?, log: String?) {
        try {
            val logFile = File(logFileName)
            initFile(logFile)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
            val currentTime = Date()
            val logMessage = "[${dateFormat.format(currentTime)}]$prefix$log"

            val fileWriter = FileWriter(logFile, true) // 设置为 true，以便追加到现有文件
            val bufferedWriter = BufferedWriter(fileWriter)

            bufferedWriter.write(logMessage)
            bufferedWriter.newLine() // 换行

            bufferedWriter.close()
            fileWriter.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error writing to the log file: ${e.message}")
            e.printStackTrace()
        } finally {
            Log.e(HookMainApp.getTag(TAG), "$prefix：$log")
        }
    }

    /**
     * 读配置文件
     */
    override fun get(key: String): String {
        val dataFileName = "$dataDir/auto-$key.txt"
        val dataFile = File(dataFileName)

        return if (dataFile.exists()) {
            dataFile.readText()
        } else {
            ""
        }
    }

    /**
     * 写配置文件
     */
    override fun put(key: String, value: String) {
        val dataFileName = "$dataDir/auto-$key.txt"
        val dataFile = File(dataFileName)
        initFile(dataFile)
        dataFile.writeText(value)
    }

    override fun analyzeData(dataType: Int, app: String, data: String) {
       runCatching {
           runBlocking {
               log("自动记账收到数据","App:$app \n Data:$data")
               val billInfo = Engine.runAnalyze(dataType, app, data)
               val appData = AppData()
               appData.issue = 0
               appData.type = dataType
               appData.rule = billInfo?.channel ?: ""
               appData.source = app
               appData.data = data
               appData.match = billInfo != null
               appData.time = System.currentTimeMillis()
               //先存到server的数据库里面
               db.insert("AppData",appData)
               if (billInfo !== null) {
                   val serviceIntent = Intent()
                   serviceIntent.setComponent(
                       ComponentName(
                           HookMainApp.pkg,
                           "net.ankio.auto.service.FloatingWindowService"
                       )
                   )
                   serviceIntent.putExtra("data", billInfo.toJSON())
                   serviceIntent.setAction("SHOW_WINDOW")
                   mContext.startService(serviceIntent)
               }
           }

        }.onFailure {
            it.printStackTrace()
            log(HookMainApp.getTag(TAG),"自动记账执行脚本发生错误:"+it.message)
            XposedBridge.log(it)
        }

    }

    /**
     * 单向同步数据库
     */
    override fun sql(table: String, action: String, data: String) {
        runCatching {
        runBlocking {
            when(table){
                "AccountMap"->{
                    when(action){
                        "insert"-> db.insert("AccountMap",AccountMap.fromJSON(data))
                        "update"->db.update("AccountMap",AccountMap.fromJSON(data))
                        "delete"->db.delete("AccountMap",AccountMap.fromJSON(data))
                    }
                }
                /*  "BillInfo"->{
                      when(action){
                          "insert"-> db.BillInfoDao().insert(BillInfo.fromJSON(data))
                          "update"->db.BillInfoDao().update(BillInfo.fromJSON(data))
                          "delete"->db.BillInfoDao().delete(BillInfo.fromJSON(data))
                      }
                  }
                  "AppData"->{
                      when(action){
                          "insert"-> db.AppDataDao().insert(AppData.fromJSON(data))
                          "update"->db.AppDataDao().update(AppData.fromJSON(data))
                          "delete"->db.AppDataDao().delete(AppData.fromJSON(data))
                      }
                  }*/
            }
            ""
        }}.onFailure {
            it.printStackTrace()
            log(HookMainApp.getTag(TAG),it.message)
            XposedBridge.log(it)
        }
    }

    /**
     * 需要同步 (双向同步)
     *
     */
    override fun syncData(): String {
        return runBlocking {
            val total = db.getTotalAppData()
            db.empty("AppData")
            total.map { it.toJSON() }.toJsonArray().toString()
        }
    }

    override fun getMap(name: String?): String {
        return  runBlocking {
            Gson().toJson(db.getTotalAccountMap())
        }
    }



}






