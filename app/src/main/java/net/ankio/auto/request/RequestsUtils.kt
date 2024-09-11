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
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.storage.CacheManager
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * @param context 上下文
 * @param cacheTime 缓存时间,默认为0,不缓存
 */
class RequestsUtils(context: Context, private val cacheTime: Int = 0) {

    /**
     * 缓存管理器
     */
    private val cacheManager = CacheManager(context)

    private val headers = HashMap<String, String>()

    fun addHeader(key: String, value: String) {
        headers[key] = value
    }

    suspend fun image(url: String): Pair<Int, ByteArray> = withContext(Dispatchers.IO) {
        val md5 = App.md5(url)
        val byte = cacheManager.readFromCache(md5)
        if (byte.isNotEmpty()) {
            return@withContext Pair(200, byte)
        }
        val client = OkHttpClient.Builder().build()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.bytes() ?: ByteArray(0)
        if (cacheTime > 0 && body.isNotEmpty()) {
            cacheManager.saveToCacheWithExpiry(md5, body, cacheTime.toLong())
        }
        Pair(response.code, body)
    }

    /**
     * GET请求
     */
    suspend fun get(url: String, query: HashMap<String, String>? = null): Pair<Int, String> =
        withContext(Dispatchers.IO) {

            val byte = cacheManager.readFromCache(url)
            if (byte.isNotEmpty()) {
                return@withContext Pair(200, String(byte))
            }

// OkHttpClient 实例
            val client = OkHttpClient.Builder()
                .build()

            // 构建查询参数
            val httpUrlBuilder = url.toHttpUrlOrNull()?.newBuilder()
            query?.forEach { (key, value) ->
                httpUrlBuilder?.addQueryParameter(key, value)
            }

            // 构建完整URL
            val fullUrl = httpUrlBuilder?.build()?.toString() ?: url

            // 构建请求
            val requestBuilder = Request.Builder()
                .url(fullUrl)

            // 添加请求头
            headers.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }

            // 构建最终请求
            val request = requestBuilder.build()

            // 执行请求
            val response = client.newCall(request).execute()

            val body = response.body?.string() ?: ""
            if (cacheTime > 0 && body.isNotEmpty()) {
                cacheManager.saveToCacheWithExpiry(url, body.toByteArray(), cacheTime.toLong())

            }

            Pair(response.code, body)
        }


    /**
     * POST请求
     */
    suspend fun form(url: String, body: HashMap<String, String>? = null): Pair<Int, String> =
        withContext(Dispatchers.IO) {
            val client = OkHttpClient.Builder().build()
            val requestBuilder = Request.Builder().url(url)

            // 添加请求头
            headers.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }

            // 构建表单请求体
            val formBuilder = FormBody.Builder()
            body?.forEach { (key, value) ->
                formBuilder.add(key, value)
            }
            val requestBody = formBuilder.build()

            // 设置 POST 方法和请求体
            val request = requestBuilder.post(requestBody).build()

            // 执行请求
            val response = client.newCall(request).execute()

            Pair(response.code, response.body.toString())
        }

    /**
     * JSON请求
     */
    suspend fun json(url: String, body: JsonObject): Pair<Int, String> =
        withContext(Dispatchers.IO) {
            val client = OkHttpClient.Builder().build()
            val requestBuilder = Request.Builder().url(url)

            // 添加请求头
            headers.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }

            // 构建 JSON 请求体
            val requestBody = RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                body.toString() // 将 JsonObject 转换为字符串
            )

            // 设置 POST 方法和请求体
            val request = requestBuilder.post(requestBody).build()

            // 执行请求并获取响应
            val response = client.newCall(request).execute()

            // 返回响应结果
            Pair(response.code, response.body.toString())
        }

    /**
     * 执行文件上传
     */
    suspend fun upload(url: String, file: File): Pair<Int, String> = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder().build()

        // 构建 MultipartBody
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM) // 设置表单类型
            .addFormDataPart(
                "file",
                file.name,
                RequestBody.create("application/octet-stream".toMediaTypeOrNull(), file)
            )
            .build()

        // 构建请求
        val request = Request.Builder()
            .url(url)
            .post(requestBody) // 使用 POST 请求
            .apply {
                // 添加请求头（如果需要）
                headers.forEach { (key, value) ->
                    addHeader(key, value)
                }
            }
            .build()

        // 执行请求
        val response = client.newCall(request).execute()

        // 返回响应结果
        Pair(response.code, response.body.toString())
    }

    /**
     * 下载文件
     */
    suspend fun download(url: String, file: File): Boolean = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder().build()
        val requestBuilder = Request.Builder().url(url)

        // 添加请求头
        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        val request = requestBuilder.build()
        try {
            // 执行请求
            val response = client.newCall(request).execute()

            // 检查响应是否成功
            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response")
            }

            // 获取响应体的输入流
            val inputStream = response.body?.byteStream() ?: return@withContext false

            // 创建文件输出流
            FileOutputStream(file).use { outputStream ->
                // 读取和写入数据
                inputStream.copyTo(outputStream)
            }

            true
        } catch (e: IOException) {
            // 捕捉异常并返回 false
            e.printStackTrace()
            false
        }
    }


    // PUT 请求，上传文件并返回响应的字符串形式
    suspend fun put(url: String, file: File): Pair<Int, String> = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder().build()
        val fileRequestBody = file.asRequestBody() // 根据文件自动推断 MIME 类型


        val requestBuilder = Request.Builder().url(url).put(fileRequestBody)

        // 添加请求头
        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        val request = requestBuilder.build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: "No response body" // 将响应转换为字符串
            Pair(response.code, body)
        }
    }

    // DELETE 请求并返回响应的字符串形式
    suspend fun delete(url: String): String = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder().build()

        val requestBuilder = Request.Builder().url(url).delete()

        // 添加请求头
        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        val request = requestBuilder.build()

        client.newCall(request).execute().use { response ->
            response.body?.string() ?: "No response body" // 将响应转换为字符串
        }
    }

    // MKCOL 请求并返回响应的字符串形式
    suspend fun mkcol(url: String): Int = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder().build()
        val requestBuilder = Request.Builder().url(url).method("MKCOL", null) // MKCOL 请求不需要请求体

        // 添加请求头
        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        val request = requestBuilder.build()

        client.newCall(request).execute().use { response ->
            response.body?.close()
            response.code // 返回响应码
        }
    }

}
