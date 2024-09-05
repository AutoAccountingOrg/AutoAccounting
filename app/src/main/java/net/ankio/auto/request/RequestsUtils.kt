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

package net.ankio.auto.request

/**
 * RequestsUtils
 * 请求工具
 */
import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.exceptions.HttpException
import net.ankio.auto.storage.CacheManager
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.AppTimeMonitor
import net.ankio.auto.utils.AppUtils
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

    /**
     * 缓存管理器
     */
    private val cacheManager = CacheManager(context)

    init {
        if (client == null) {
            client =
                OkHttpClient.Builder()
                    .retryOnConnectionFailure(true)
                    .build()
        }
    }

    /**
     * 将请求体转换成字符串
     * @param requestBody 请求体
     * @return 返回转换后的字符串
     * @throws Exception
     */
    private fun requestBodyToString(requestBody: RequestBody?): String? {
        if (requestBody == null) return null
        val buffer = Buffer()
        requestBody.writeTo(buffer)
        return buffer.readString(Charsets.UTF_8)
    }

    /**
     * 构建请求体
     * @param data 数据
     * @param contentType 内容类型
     * @return 返回请求体
     */
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

    /**
     * 将byteArray转换成字符串
     * @param byteArray 要转换的byteArray
     * @return 返回转换后的字符串
     */
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

    /**
     * 发送请求
     * @param url 请求的URL
     * @param query 查询参数
     * @param data 数据
     * @param method 请求方法
     * @param contentType 内容类型
     * @param headers 请求头
     * @param cacheTime 缓存时间
     * @return 返回请求结果
     */
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
                    " Cache Hit \n",
                    //    convertByteArray(cachedData),
                )
                Logger.d(message.toString())
                return@withContext RequestResult(cachedData, 200)
            }
            AppTimeMonitor.startMonitoring("请求: $requestUrl")

            try {
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
                    App.launch {
                        cacheManager.saveToCacheWithExpiry(
                            cacheKey,
                            bytes ?: ByteArray(0),
                            cacheTime.toLong(),
                        )
                    }
                }
                message.append(
                    " Response Success  " + response.code + " " + response.message + "\n"
                )


                RequestResult(bytes ?: ByteArray(0), response.code)
            } catch (e: Throwable) {
                throw e
            } finally {
                Logger.d(message.toString())
                AppTimeMonitor.stopMonitoring("请求: $requestUrl")
            }
        }

    /**
     * GET请求
     * @param url 请求的URL
     * @param query 查询参数
     * @param headers 请求头
     * @param cacheTime 缓存时间
     * @return 返回请求结果
     */
    suspend fun get(
        url: String,
        query: HashMap<String, String>? = null,
        headers: HashMap<String, String> = HashMap(),
        cacheTime: Int = 0,
    ): RequestResult {
        return sendRequest(url, query, null, METHOD_GET, TYPE_FORM, headers, cacheTime)
    }

    /**
     * POST请求
     * @param url 请求的URL
     * @param query 查询参数
     * @param data 数据
     * @param contentType 内容类型
     * @param headers 请求头
     * @param cacheTime 缓存时间
     * @return 返回请求结果
     */
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

    /**
     * PUT请求
     * @param url 请求的URL
     * @param query 查询参数
     * @param data 数据
     * @param contentType 内容类型
     * @param headers 请求头
     * @param cacheTime 缓存时间
     * @return 返回请求结果
     */
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

    /**
     * DELETE请求
     * @param url 请求的URL
     * @param query 查询参数
     * @param data 数据
     * @param contentType 内容类型
     * @param headers 请求头
     * @param cacheTime 缓存时间
     * @return 返回请求结果
     */
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


    /**
     * 将byteArray转换成JSON对象
     * @param byteArray 要转换的byteArray
     * @return 返回转换后的JSON对象
     */
    fun json(byteArray: ByteArray): JsonObject? {
        return Gson().fromJson(byteArray.toString(Charsets.UTF_8), JsonObject::class.java)
    }

    /**
     * 生成缓存键
     * @param url 请求的URL
     * @param method 请求的方法
     * @param data 请求的数据
     */
    private fun generateCacheKey(
        url: String,
        method: String,
        data: Map<String, String>?,
    ): String {
        return AppUtils.md5(url + method + (data?.toString() ?: ""))
    }

    /**
     * 创建目录
     * @param url 集合的URL
     * @param query 查询参数
     * @param data 数据
     * @param contentType 内容类型
     * @param headers 请求头
     * @param cacheTime 缓存时间
     * @return 返回请求结果
     */
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
