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
        suspend fun isServerStart(mContext: Context): Boolean = suspendCoroutine { continuation ->
                RequestsUtils(mContext).get("$HOST:$PORT/",
                    headers = hashMapOf("Authorization" to getToken(mContext)),
                    onSuccess = { _, code ->
                        continuation.resume(code == 200)
                    },
                    onError = {
                        Logger.i("$HOST:$PORT/ 请求错误$it")
                        continuation.resume(false)
                    })
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

        fun delete(name: String,mContext: Context) {
            val path =  Environment.getExternalStorageDirectory().path+"/Android/data/${mContext.packageName}/cache/shell/${name}.txt"
            val file = File(path)
            if(file.exists()){
                file.delete()
            }
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
     * 请求错误
     */
    private fun onError(string: String){
        throw AutoServiceException(string,AutoServiceException.CODE_SERVER_ERROR)
    }

    /**
     * 获取数据
     */
    suspend fun get(name: String):String = suspendCoroutine{
        requestsUtils.get(getUrl("/get"),
            query = hashMapOf("name" to name),
            headers = headers,
            onSuccess = { bytearray,code ->
                        if(code==200){
                            it.resume(String(bytearray).trim())
                        }else{
                            throw AutoServiceException(String(bytearray),AutoServiceException.CODE_SERVER_AUTHORIZE)
                        }
        },
     onError = this::onError)
    }

    /**
     * 设置数据
     */
    fun set(name: String, value: String){
        requestsUtils.post(getUrl("/set"),
            query = hashMapOf("name" to name),
            data = hashMapOf("raw" to value),
            headers = headers,
            contentType = RequestsUtils.TYPE_RAW,
            onSuccess = { bytearray, code ->
                if(code!=200){
                    throw AutoServiceException(String(bytearray),AutoServiceException.CODE_SERVER_AUTHORIZE)
                }
        },onError = this::onError)
    }

    /**
     * 设置App记录数据
     */
    fun putData(value: String){
        requestsUtils.post(getUrl("/data"),
            data = hashMapOf("raw" to value),
            contentType = RequestsUtils.TYPE_RAW,
            headers = headers,
            onSuccess = { bytearray,code ->
                if(code!=200){
                    throw AutoServiceException(String(bytearray),AutoServiceException.CODE_SERVER_AUTHORIZE)
                }
        },onError = this::onError)
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
    fun putLog(value: String){
        requestsUtils.post(getUrl("/log"),
            data = hashMapOf("raw" to value),
            contentType = RequestsUtils.TYPE_RAW,
            headers = headers,
            onSuccess = { bytearray,code ->
                if(code!=200){
                    onError(String(bytearray).trim())
                }
        },onError = this::onError)
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