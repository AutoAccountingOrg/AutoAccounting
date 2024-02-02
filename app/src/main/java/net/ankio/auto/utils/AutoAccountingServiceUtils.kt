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
import net.ankio.auto.exceptions.AutoServiceException
import java.io.IOException
import java.net.ServerSocket

/**
 * AutoAccountingServiceUtils
 * 自动记账服务调用工具
 */
class AutoAccountingServiceUtils(mContext: Context) {

    private var requestsUtils = RequestsUtils(mContext)

    private val host = "http://127.0.0.1"



    private val token = "qSohhh91qLBMtIMpdXoOwGn8vvVKJx6UXkZkiW"

    private val headers = HashMap<String, String>()

    companion object{
        private const val PORT = 52045
        fun isPortAvailable(): Boolean {
            var serverSocket: ServerSocket? = null
            return try {
                serverSocket = ServerSocket(PORT)
                true // 端口可用
            } catch (e: IOException) {
                false // 端口被占用
            } finally {
                serverSocket?.close()
            }
        }
    }

    init {

       if(isPortAvailable()){
          throw AutoServiceException("Service not started")
       }
        headers["Authorization"] = token

    }

    /**
     * 获取请求地址
     */
    private fun getUrl(path:String): String {
        return "$host:$PORT$path"
    }

    /**
     * 请求错误
     */
    private fun onError(string: String){
        Logger.i("自动记账服务错误：${string}")
    }

    /**
     * 获取数据
     */
    fun get(name: String, onSuccess: (String) -> Unit){
        requestsUtils.get(getUrl("/get"),
            query = hashMapOf("name" to name),
            headers = headers,
            onSuccess = { bytearray,code ->
                        if(code==200){
                            onSuccess(String(bytearray).trim())
                        }else{
                            onError(String(bytearray).trim())
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
                    onError(String(bytearray).trim())
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
                    onError(String(bytearray).trim())
                }
        },onError = this::onError)
    }

    /**
     * 获取记录的数据
     */
    fun getData(onSuccess: (String) -> Unit){
        get("data",onSuccess)
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
    fun getLog(onSuccess: (String) -> Unit){
        get("log",onSuccess)
    }
}