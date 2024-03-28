/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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
import android.os.Environment
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.exceptions.AutoServiceException
import net.ankio.auto.utils.event.EventBus
import net.ankio.auto.utils.request.RequestsUtils
import net.ankio.common.config.AccountingConfig
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * AutoAccountingServiceUtils
 * 自动记账服务调用工具
 * @throws AutoServiceException
 */
class AutoAccountingServiceUtils(mContext: Context) {

    private var requestsUtils = RequestsUtils(mContext)

    private val headers = HashMap<String, String>()

    companion object{
        private const val HOST = "http://127.0.0.1"
        private const val PORT = 52045
        // 将isServerStart转换为挂起函数
        suspend fun isServerStart(mContext: Context): Boolean = withContext(Dispatchers.IO) {
              runCatching {
                  val result =   RequestsUtils(mContext).get("$HOST:$PORT/",
                      headers = hashMapOf("Authorization" to getToken(mContext)))
                  result.code == 200
              }.getOrNull()?:false
        }


        fun getToken(mContext: Context): String {
           return get("token",mContext)
        }
        /**
         * 获取文件内容
         */
        fun get(name: String,mContext: Context): String {
            val path =  Environment.getExternalStorageDirectory().path+"/Android/data/${mContext.packageName}/cache/shell/${name}.txt"
            val file = File(path)
            if(file.exists()){
                return file.readText().trim()
            }
            return ""
        }

        fun set(name: String,data:String,mContext: Context) {
            val path =  Environment.getExternalStorageDirectory().path+"/Android/data/${mContext.packageName}/cache/shell/${name}.txt"
            val file = File(path)
            if(!file.exists()){
                file.createNewFile()
            }
            file.writeText(data)

        }

        fun delete(name: String,mContext: Context) {
            val path =  Environment.getExternalStorageDirectory().path+"/Android/data/${mContext.packageName}/cache/shell/${name}.txt"
            val file = File(path)
            if(file.exists()){
                file.delete()
            }
        }

        fun log(data: String,mContext: Context) {
            val path =  Environment.getExternalStorageDirectory().path+"/Android/data/${mContext.packageName}/cache/shell/log.txt"
            val file = File(path)
            if(!file.exists()){
                file.createNewFile()
            }
            file.appendText("[ ${DateUtils.getTime(System.currentTimeMillis())} ]\n$data\n")
        }

        /**
         * 获取请求地址
         */
        fun getUrl(path:String = ""): String {
            return "$HOST:$PORT$path"
        }



    }

    init {
        headers["Authorization"] = getToken(mContext)
    }



    /**
     * 获取数据
     */
    suspend fun get(name: String):String = withContext(Dispatchers.IO){
            val result = requestsUtils.post(
                getUrl("/get"),
                query = hashMapOf("name" to name),
                headers = headers
            )
            if (result.code == 200) {
                String(result.byteArray).trim()
            } else {
                throw AutoServiceException(
                    String(result.byteArray),
                    AutoServiceException.CODE_SERVER_AUTHORIZE
                )
            }

    }

    /**
     * 设置数据
     */
    suspend fun set(name: String, value: String) = withContext(Dispatchers.IO){
        requestsUtils.post(getUrl("/set"),
            query = hashMapOf("name" to name),
            data = hashMapOf("raw" to value),
            contentType = RequestsUtils.TYPE_RAW,
            headers = headers).apply {
            if (code != 200) {
                throw AutoServiceException(
                    String(byteArray),
                    AutoServiceException.CODE_SERVER_AUTHORIZE
                )
            }
        }
    }

    /**
     * 设置App记录数据
     */
   suspend fun putData(value: String)= withContext(Dispatchers.IO){
        requestsUtils.post(getUrl("/data"),
            data = hashMapOf("raw" to value),
            contentType = RequestsUtils.TYPE_RAW,
            headers = headers).apply {
            if (code != 200) {
                throw AutoServiceException(
                    String(byteArray),
                    AutoServiceException.CODE_SERVER_AUTHORIZE
                )
            }
        }

    }

    /**
     * 获取记录的数据
     */
    suspend fun getData():String = withContext(Dispatchers.IO){
        get("data")
    }

    /**
     * 设置App记录日志
     */
    suspend fun putLog(value: String)= withContext(Dispatchers.IO){
        requestsUtils.post(getUrl("/log"),
            data = hashMapOf("raw" to value),
            contentType = RequestsUtils.TYPE_RAW,
            headers = headers,
        ).apply {
            if(code!=200){
                throw AutoServiceException(
                    String(byteArray),
                    AutoServiceException.CODE_SERVER_AUTHORIZE
                )
            }
        }
    }

    /**
     * 获取App记录的日志
     */
    suspend fun getLog():String = withContext(Dispatchers.IO){
        get("log")
    }
    /**
     * 获取配置
     */
   suspend fun config():AccountingConfig = withContext(Dispatchers.IO){
        runCatching {
            val bookAppConfig = get("bookAppConfig")
            val config = Gson().fromJson(bookAppConfig, AccountingConfig::class.java)
            if(config==null){
                set("bookAppConfig",Gson().toJson(AccountingConfig()))
                AccountingConfig()
            }else{
                config
            }
        }.onFailure {
            Logger.e("获取配置失败",it)
        }.getOrNull()?:AccountingConfig()
    }
}