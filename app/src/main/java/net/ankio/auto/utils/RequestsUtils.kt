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
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import java.io.File
import java.io.IOException


class RequestsUtils(context: Context) {

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


    val cacheManager = CacheManager(context)
    init {
        if(client === null){
            client = OkHttpClient.Builder()
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


    private fun convertByteArray(byteArray: ByteArray):String{
        var message = ""
        runCatching {
            message = String(byteArray, Charsets.UTF_8)
        }.onSuccess {
            if ((message.startsWith("{") && message.endsWith("}"))
                || (message.startsWith("[") && message.endsWith("]"))
            ) {

                runCatching {
                    val gson = GsonBuilder().setPrettyPrinting().create()
                    // 将JSON字符串转换成Java对象，然后再转换回格式化的JSON字符串
                    val obj: Any = gson.fromJson(message, Any::class.java)
                    message = gson.toJson(obj)
                }
            }
        }.onFailure {
            val hexResult = byteArray.joinToString(separator = " ") {
                String.format("%02X", it)
            }
            val asciiResult = byteArray.filter { it.toInt() in 32..126 }.map { it.toInt().toChar() }.joinToString("")

            message = "[ Cannot be displayed byteArray ] Size: ${byteArray.size} \n Hex: \n $hexResult\nASCII:\n $asciiResult"

        }

        return message

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
        AppUtils.getScope().launch {

            val message = StringBuilder()
            message.append("$method ")
            var requestUrl = url
            if (!query.isNullOrEmpty()) {
                requestUrl += query.entries.joinToString("&", prefix = "?") { "${it.key}=${it.value}" }
            }

            message.append(requestUrl + "\n")

            val cacheKey = generateCacheKey(requestUrl, method, data)
            val cachedData = cacheManager.readFromCache(cacheKey)
            if (cacheTime > 0 && cachedData.isNotEmpty()) {
                message.append(
                    "\n───────────────────────────────────────────────────────────────\n" +
                            " Cache Hit \n" +
                            "───────────────────────────────────────────────────────────────\n"+
                            convertByteArray(cachedData)
                )
                Logger.d(message.toString())
                withContext(Dispatchers.Main){
                    onSuccess(cachedData, 200)
                }
                return@launch
            }
            AppTimeMonitor.startMonitoring("请求: $requestUrl")
            val requestBuilder = Request.Builder().url(requestUrl)

            val body = if (method == METHOD_GET) null else buildRequestBody(data, contentType)



            requestBuilder.method(method, body)

            headers.forEach { (key, value) ->
                message.append("$key: $value\n")
                requestBuilder.addHeader(key, value)
            }

            if(body!=null){
                message.append( convertByteArray(body.toString().toByteArray()))
            }

            val request = requestBuilder.build()

            withContext(Dispatchers.Main){
                client?.newCall(request)?.enqueue(object : Callback {
                    //还在子线程中
                    override fun onResponse(call: Call, response: Response) {
                        AppTimeMonitor.startMonitoring("请求: $requestUrl")
                        response.body?.byteStream()?.use {
                            val bytes = it.readBytes()
                            if (cacheTime > 0 && response.isSuccessful) {
                                AppUtils.getScope().launch {
                                    cacheManager.saveToCacheWithExpiry(cacheKey, bytes, cacheTime.toLong())
                                }
                            }
                            message.append(
                                "\n───────────────────────────────────────────────────────────────\n" +
                                        " Response Success \n " + response.code+" "+ response.message+"\n"+
                                        "───────────────────────────────────────────────────────────────\n"+
                                        convertByteArray(bytes)
                            )
                            Logger.d(message.toString())
                            mainHandler.post {
                                onSuccess(bytes, response.code)
                            }

                        } ?: {
                            message.append(
                                "\n───────────────────────────────────────────────────────────────\n" +
                                        " Response Empty \n " + response.code+" "+ response.message+"\n"+
                                        "───────────────────────────────────────────────────────────────\n"
                            )
                            Logger.d(message.toString())
                            mainHandler.post {
                                onSuccess(ByteArray(0), response.code)
                            }
                        }
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        AppTimeMonitor.startMonitoring("请求: $requestUrl")
                        message.append(
                            "\n───────────────────────────────────────────────────────────────\n" +
                                    " Response Error \n" +
                                    "───────────────────────────────────────────────────────────────\n"+
                                    e.message
                        )
                        Logger.e(message.toString(),e)
                        mainHandler.post {
                            onError(e.message ?: "Unknown error")
                        }

                    }
                })
            }

        }


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

    fun json(byteArray: ByteArray): JsonObject? {
        return Gson().fromJson(byteArray.toString(Charsets.UTF_8), JsonObject::class.java)
    }

    private fun generateCacheKey(url: String, method: String, data: Map<String, String>?): String {
        return AppUtils.md5(url + method + (data?.toString() ?: ""))
    }


}
