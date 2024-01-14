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

import android.content.Context
import android.os.IBinder
import android.util.Log
import net.ankio.auto.IAccountingService
import net.ankio.auto.utils.context
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.lang.reflect.Method
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class AccountingService(private val mContext: Context?) : IAccountingService.Stub() {


    private val TAG = "AutoAccountingService"



    fun systemReady() {
        Log.e(TAG,"Welcome to AutoAccounting.")
        // 删除日志
        val logFile = File(logFileName)
        if (logFile.exists()) {
            logFile.delete()
        }
        Log.e(TAG,"Removed old logs：$logFile")
        //写入启动日志
        log("Android","------- AutoAccounting Start  ")
    }

    companion object{
        private const val serviceName = "accounting.service"

        fun getServiceName(): String {
            return "user.$serviceName"
        }
        private var mService: IAccountingService? = null
        fun get(): IAccountingService? {
            try {
                if (mService == null) {
                    val svcManager = Class.forName("android.os.ServiceManager")
                    val getServiceMethod: Method = svcManager.getDeclaredMethod("getService", String::class.java)
                    mService = asInterface(getServiceMethod.invoke(null, getServiceName()) as IBinder)
                }
                return mService
            } catch (e: Exception) {
                println(e.message)
                e.printStackTrace()
//                XposedBridge.log(e)
            }
            return null
        }
        fun md5(input: String): String {
            return MessageDigest.getInstance("MD5").digest(input.toByteArray()).joinToString("") {
                "%02x".format(it)
            }
        }
        const val dataDir = "/data/system/net.ankio.auto.xposed"
        const val logFileName = "$dataDir/accounting_log.txt"

    }




    /**
     * 初始化创建文件夹子
     */

    private fun initFile(file: File){
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
            val logMessage = "[${dateFormat.format(currentTime)}][$prefix]$log"

            val fileWriter = FileWriter(logFile, true) // 设置为 true，以便追加到现有文件
            val bufferedWriter = BufferedWriter(fileWriter)

            bufferedWriter.write(logMessage)
            bufferedWriter.newLine() // 换行

            bufferedWriter.close()
            fileWriter.close()
        } catch (e: IOException) {
            Log.e(TAG,"Error writing to the log file: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun put(key: String?, value: String?) {
        if (key == null || value == null) return

        try {
            val fileName = md5(key)
            val file = File("$dataDir/$fileName")

            file.writeText(value)
        } catch (e: IOException) {
            Log.e(TAG, "Error writing to file: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun callApp() {
        TODO("Not yet implemented")
    }

    override fun get(key: String?): String {
        if (key == null) return ""

        try {
            val fileName = md5(key)
            val file = File("$dataDir/$fileName")

            if (file.exists()) {
                return file.readText()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading from file: ${e.message}")
            e.printStackTrace()
        }

        return ""
    }
}
