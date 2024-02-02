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

/**
 * RequestsUtils
 * 请求工具
 */
import android.content.Context
import android.os.Handler
import android.os.Looper
import net.ankio.auto.utils.JsonUtil.formatJson
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.io.IOException


private class HttpLogger : HttpLoggingInterceptor.Logger {
    private val mMessage = StringBuilder()

    override fun log(msg: String) {
        // 请求或者响应开始
        var message = msg
        if (message.startsWith("--> POST")) {
            mMessage.setLength(0)
        }
        // 以{}或者[]形式的说明是响应结果的json数据，需要进行格式化
        if ((message.startsWith("{") && message.endsWith("}"))
            || (message.startsWith("[") && message.endsWith("]"))
        ) {
            message = formatJson(message)
        }
        mMessage.append(message + "\n")
        // 请求或者响应结束，打印整条日志
        if (message.startsWith("<-- END HTTP")) {
            Logger.d(mMessage.toString())
        }
    }
}
class RequestsUtils(private val context: Context) {

    companion object {
        const val TYPE_FORM = 0
        const val TYPE_JSON = 1
        const val TYPE_FILE = 2
        const val TYPE_RAW = 3

        const val METHOD_GET = "GET"
        const val METHOD_POST = "POST"
        const val METHOD_PUT = "PUT"
        const val METHOD_DELETE = "DELETE"
         var client: OkHttpClient? = null
        val mainHandler = Handler(Looper.getMainLooper())
    }



    init {
        if(client === null){
            val logInterceptor = HttpLoggingInterceptor(HttpLogger())
            logInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
            client = OkHttpClient.Builder()
                .addNetworkInterceptor(logInterceptor)
                .cache(Cache(context.cacheDir, 10 * 1024 * 1024)) // 10MB cache
                .build()
        }
    }


    // 构建请求体
    private fun buildRequestBody(data: Map<String, String>?, contentType: Int): RequestBody? {
        return when (contentType) {
            TYPE_JSON -> {
                val json = data?.get("json") ?: "{}"
                json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            }
            TYPE_FORM -> {
                FormBody.Builder().apply {
                    data?.forEach { (key, value) -> add(key, value) }
                }.build()
            }
            TYPE_FILE -> {
                val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
                data?.forEach { (key, value) ->
                    val file = File(value)
                    if (file.exists() && file.isFile) {
                        val fileBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                        builder.addFormDataPart(key, file.name, fileBody)
                    } else {
                        builder.addFormDataPart(key, value)
                    }
                }
                builder.build()
            }
            TYPE_RAW -> {
                val rawValue = data?.get("raw") ?: ""
                val file = File(rawValue)
                if (file.exists() && file.isFile) {
                    file.readBytes().toRequestBody("application/octet-stream".toMediaTypeOrNull())
                } else {
                    rawValue.toRequestBody("application/octet-stream".toMediaTypeOrNull())
                }
            }
            else -> null
        }
    }


    private fun sendRequest(
        url: String,
        query: HashMap<String, String>? = null,
        data: HashMap<String, String>? = null,
        method: String = METHOD_GET,
        contentType: Int = TYPE_FORM,
        headers: HashMap<String, String> = HashMap(),
        onSuccess: (ByteArray, Int) -> Unit,
        onError: (String) -> Unit,
        cacheTime: Int = 0
    ) {
        var requestUrl = url
        if (!query.isNullOrEmpty()) {
            requestUrl += query.entries.joinToString("&", prefix = "?") { "${it.key}=${it.value}" }
        }

        val cacheKey = generateCacheKey(requestUrl, method, data)
        val cacheFile = File(context.cacheDir, "httpCache").resolve(cacheKey)

        if (cacheTime > 0 && cacheFile.exists() && (System.currentTimeMillis() - cacheFile.lastModified()) < cacheTime * 1000) {
            val cachedData = cacheFile.readBytes()
            onSuccess(cachedData, 200) // Assuming HTTP 200 for cached responses
            return
        }

        val requestBuilder = Request.Builder().url(requestUrl)

        val body = if (method == METHOD_GET) null else buildRequestBody(data, contentType)

        requestBuilder.method(method, body)

        headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }

        val request = requestBuilder.build()

        client?.newCall(request)?.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                response.body?.byteStream()?.use {
                    val bytes = it.readBytes()
                    if (cacheTime > 0 && response.isSuccessful) {
                        cacheFile.parentFile?.mkdirs()
                        cacheFile.writeBytes(bytes)
                    }
                    mainHandler.post {
                        onSuccess(bytes, response.code)
                    }

                } ?:  mainHandler.post {
                    onSuccess(ByteArray(0), response.code)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    onError(e.message ?: "Unknown error")
                }

            }
        })
    }
    // GET请求
    fun get(
        url: String,
        query: HashMap<String, String>? = null,
        headers: HashMap<String, String> = HashMap(),
        onSuccess: (ByteArray, Int) -> Unit,
        onError: (String) -> Unit,
        cacheTime: Int = 0
    ) {
        sendRequest(url, query, null, METHOD_GET, TYPE_FORM, headers, onSuccess, onError, cacheTime)
    }

    // POST请求
    fun post(
        url: String,
        query: HashMap<String, String>? = null,
        data: HashMap<String, String>? = null,
        contentType: Int = TYPE_FORM,
        headers: HashMap<String, String> = HashMap(),
        onSuccess: (ByteArray, Int) -> Unit,
        onError: (String) -> Unit,
        cacheTime: Int = 0
    ) {
        sendRequest(url, query, data, METHOD_POST, contentType, headers, onSuccess, onError, cacheTime)
    }

    // PUT请求
    fun put(
        url: String,
        query: HashMap<String, String>? = null,
        data: HashMap<String, String>? = null,
        contentType: Int = TYPE_FORM,
        headers: HashMap<String, String> = HashMap(),
        onSuccess: (ByteArray, Int) -> Unit,
        onError: (String) -> Unit,
        cacheTime: Int = 0
    ) {
        sendRequest(url, query, data, METHOD_PUT, contentType, headers, onSuccess, onError, cacheTime)
    }

    // DELETE请求
    fun delete(
        url: String,
        query: HashMap<String, String>? = null,
        data: HashMap<String, String>? = null,
        contentType: Int = TYPE_FORM,
        headers: HashMap<String, String> = HashMap(),
        onSuccess: (ByteArray, Int) -> Unit,
        onError: (String) -> Unit,
        cacheTime: Int = 0
    ) {
        sendRequest(url, query, data, METHOD_DELETE, contentType, headers, onSuccess, onError, cacheTime)
    }

    private fun generateCacheKey(url: String, method: String, data: Map<String, String>?): String {
        return (url + method + (data?.toString() ?: "")).hashCode().toString()
    }

}
