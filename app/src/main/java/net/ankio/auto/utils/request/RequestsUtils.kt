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

package net.ankio.auto.utils.request

/**
 * RequestsUtils
 * 请求工具
 */
import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.exceptions.HttpException
import net.ankio.auto.utils.AppTimeMonitor
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.CacheManager
import net.ankio.auto.utils.Logger
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import java.io.File

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
        const val METHOD_MKCOL = "MKCOL"
        var client: OkHttpClient? = null
    }

    private val cacheManager = CacheManager(context)

    init {
        if (client === null) {
            client =
                OkHttpClient.Builder()
                    .retryOnConnectionFailure(true)
                    .build()
        }
    }

    private fun requestBodyToString(requestBody: RequestBody?): String? {
        if (requestBody == null) return null
        val buffer = Buffer()
        requestBody.writeTo(buffer)
        return buffer.readString(Charsets.UTF_8)
    }

    // 构建请求体
    private fun buildRequestBody(
        data: Map<String, String>?,
        contentType: Int,
    ): RequestBody? {
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
                        val fileBody =
                            file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
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

    private fun convertByteArray(byteArray: ByteArray): String {
        var message = ""
        runCatching {
            // 判断byteArray大小，太大抛异常
            if (byteArray.size > 1024 * 100) {
                throw HttpException("byteArray too large")
            }
            message = String(byteArray, Charsets.UTF_8)
        }.onSuccess {
            if ((message.startsWith("{") && message.endsWith("}")) ||
                (message.startsWith("[") && message.endsWith("]"))
            ) {
                runCatching {
                    val gson = GsonBuilder().setPrettyPrinting().create()
                    // 将JSON字符串转换成Java对象，然后再转换回格式化的JSON字符串
                    val obj: Any = gson.fromJson(message, Any::class.java)
                    message = gson.toJson(obj)
                }
            }
        }.onFailure {
            message = "[ Cannot be displayed byteArray ] Size: ${byteArray.size} "
        }

        return message
    }

    private suspend fun sendRequest(
        url: String,
        query: HashMap<String, String>? = null,
        data: HashMap<String, String>? = null,
        method: String = METHOD_GET,
        contentType: Int = TYPE_FORM,
        headers: HashMap<String, String> = HashMap(),
        cacheTime: Int = 0,
    ): RequestResult =
        withContext(Dispatchers.IO) {
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
                        "───────────────────────────────────────────────────────────────\n" +
                        convertByteArray(cachedData),
                )
                Logger.d(message.toString())
                return@withContext RequestResult(cachedData, 200)
            }
            AppTimeMonitor.startMonitoring("请求: $requestUrl")
            val requestBuilder = Request.Builder().url(requestUrl)

            headers.forEach { (key, value) ->
                message.append("$key: $value\n")
                requestBuilder.addHeader(key, value)
            }

            val body = if (method == METHOD_GET) null else buildRequestBody(data, contentType)

            requestBuilder.method(method, body)

            if (body != null) {
                message.append(
                    requestBodyToString(body)?.toByteArray(Charsets.UTF_8)
                        ?.let { convertByteArray(it) },
                )
            }

            val request = requestBuilder.build()

            Logger.d(message.toString())

            val response =
                client?.newCall(request)?.execute()
                    ?: throw HttpException("Request failed: response is null")

            val bytes = response.body?.bytes()
            if (cacheTime > 0 && response.isSuccessful) {
                AppUtils.getScope().launch {
                    cacheManager.saveToCacheWithExpiry(
                        cacheKey,
                        bytes ?: ByteArray(0),
                        cacheTime.toLong(),
                    )
                }
            }
            message.append(
                "\n───────────────────────────────────────────────────────────────\n" +
                    " Response Success \n " + response.code + " " + response.message + "\n" +
                    "───────────────────────────────────────────────────────────────\n" +
                    convertByteArray(bytes ?: ByteArray(0)),
            )
            Logger.d(message.toString())

            AppTimeMonitor.stopMonitoring("请求: $requestUrl")

            RequestResult(bytes ?: ByteArray(0), response.code)
        }

    suspend fun get(
        url: String,
        query: HashMap<String, String>? = null,
        headers: HashMap<String, String> = HashMap(),
        cacheTime: Int = 0,
    ): RequestResult {
        return sendRequest(url, query, null, METHOD_GET, TYPE_FORM, headers, cacheTime)
    }

    // POST请求
    suspend fun post(
        url: String,
        query: HashMap<String, String>? = null,
        data: HashMap<String, String>? = null,
        contentType: Int = TYPE_FORM,
        headers: HashMap<String, String> = HashMap(),
        cacheTime: Int = 0,
    ): RequestResult {
        return sendRequest(url, query, data, METHOD_POST, contentType, headers, cacheTime)
    }

    // PUT请求
    suspend fun put(
        url: String,
        query: HashMap<String, String>? = null,
        data: HashMap<String, String>? = null,
        contentType: Int = TYPE_FORM,
        headers: HashMap<String, String> = HashMap(),
        cacheTime: Int = 0,
    ): RequestResult {
        return sendRequest(url, query, data, METHOD_PUT, contentType, headers, cacheTime)
    }

    // DELETE请求
    suspend fun delete(
        url: String,
        query: HashMap<String, String>? = null,
        data: HashMap<String, String>? = null,
        contentType: Int = TYPE_FORM,
        headers: HashMap<String, String> = HashMap(),
        cacheTime: Int = 0,
    ): RequestResult {
        return sendRequest(url, query, data, METHOD_DELETE, contentType, headers, cacheTime)
    }

    fun json(byteArray: ByteArray): JsonObject? {
        return Gson().fromJson(byteArray.toString(Charsets.UTF_8), JsonObject::class.java)
    }

    private fun generateCacheKey(
        url: String,
        method: String,
        data: Map<String, String>?,
    ): String {
        return AppUtils.md5(url + method + (data?.toString() ?: ""))
    }

    suspend fun mkcol(
        url: String,
        query: HashMap<String, String>? = null,
        data: HashMap<String, String>? = null,
        contentType: Int = TYPE_FORM,
        headers: HashMap<String, String> = HashMap(),
        cacheTime: Int = 0,
    ): RequestResult {
        return sendRequest(url, query, data, METHOD_MKCOL, contentType, headers, cacheTime)
    }
}
