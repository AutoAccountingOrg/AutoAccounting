/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

package net.ankio.auto.http

import android.net.Uri
import android.util.Log
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.BuildConfig
import net.ankio.auto.storage.Logger
import okhttp3.Call
import okhttp3.Dns
import okhttp3.EventListener
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

/**
 * HTTP请求工具类
 * 提供各种HTTP请求方法的封装
 */
class RequestsUtils() {
    companion object {
        private const val DEFAULT_TIMEOUT = 30L
        private const val DEFAULT_MEDIA_TYPE = "application/json; charset=utf-8"
        private val customDns = Dns { hostname ->
            if (hostname == "license.ez-book.org" && BuildConfig.DEBUG) {
                listOf(InetAddress.getByName("192.168.100.200"))
            } else {
                Dns.SYSTEM.lookup(hostname)
            }
        }

        private val okHttpClient = OkHttpClient.Builder()
            .apply {
                if (BuildConfig.DEBUG) {
                    class LoudLogger : HttpLoggingInterceptor.Logger {
                        private var skip = false
                        override fun log(message: String) {
                            if ((message.startsWith("-->") || message.startsWith("<--")) && message.contains(
                                    "http"
                                )
                            ) {
                                // 判断当前请求 URL 是否需要跳过
                                skip = listOf("/log").any { message.contains(it) }
                            }
                            if (!skip) {
                                Log.w("Request", message)
                            }
                            // 在结束一轮请求后重置
                            if (message.startsWith("<-- END HTTP")) {
                                skip = false
                            }
                        }
                    }

                    val loud =
                        HttpLoggingInterceptor(LoudLogger()).setLevel(HttpLoggingInterceptor.Level.BODY)
                    addInterceptor(loud)
                }
            }
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .dns(customDns)
            .build()
    }


    // 自定义异常类
    sealed class RequestException(message: String, cause: Throwable? = null) :
        Exception(message, cause) {
        class NetworkException(message: String, cause: Throwable? = null) :
            RequestException(message, cause)

        class ParseException(message: String, cause: Throwable? = null) :
            RequestException(message, cause)
    }

    private val headers = HashMap<String, String>()
    private val client: OkHttpClient = okHttpClient  // 使用单例实例

    fun addHeader(key: String, value: String) {
        headers[key] = value
    }

    private fun Request.Builder.addHeaders() = apply {
        headers.forEach { (key, value) ->
            addHeader(key, value)
        }
    }

    /**
     * 在 IO 线程执行 block；
     * 发生异常时记录日志并返回 caller 提供的默认值。
     */
    private suspend inline fun <T> executeRequest(
        crossinline fallback: () -> T,      // 默认值提供者
        crossinline block: () -> T
    ): T = withContext(Dispatchers.IO) {
        runCatching { block() }
            .onFailure { ex ->
                when (ex) {
                    is IOException -> Logger.e("Network error", ex)
                    else -> Logger.e("Unexpected error", ex)
                }
            }
            .getOrElse { fallback() }       // 关键：返回默认值
    }


    suspend fun image(url: String): Pair<Int, ByteArray> =
        executeRequest(fallback = { 500 to ByteArray(0) }) {
            val request = Request.Builder()
                .url(url)
                .addHeaders()
                .build()


            client.newCall(request).execute().use { response ->
                val body = response.body?.bytes() ?: ByteArray(0)
                Pair(response.code, body)
            }
        }

    suspend fun get(url: String, query: HashMap<String, String>? = null): Pair<Int, String> =
        executeRequest(fallback = { 500 to "" }) {

            val httpUrlBuilder = url.toHttpUrlOrNull()?.newBuilder()
            query?.forEach { (key, value) ->
                httpUrlBuilder?.addQueryParameter(key, value)
            }

            val request = Request.Builder()
                .url(httpUrlBuilder?.build()?.toString() ?: url)
                .addHeaders()
                .build()



            client.newCall(request).execute().use { response ->
                Pair(response.code, response.body.string())
            }
        }

    suspend fun form(url: String, body: HashMap<String, String>? = null): Pair<Int, String> =
        executeRequest(fallback = { 500 to "" }) {
            val formBuilder = FormBody.Builder()
            body?.forEach { (key, value) ->
                formBuilder.add(key, value)
            }

            val request = Request.Builder()
                .url(url)
                .addHeaders()
                .post(formBuilder.build())
                .build()

            client.newCall(request).execute().use { response ->
                Pair(response.code, response.body.string())
            }
        }

    suspend fun jsonStr(url: String, payload: String): Pair<Int, String> =
        executeRequest(fallback = { 500 to "" }) {
            val request = Request.Builder()
                .url(url)
                .addHeaders()
                .post(payload.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            client.newCall(request).execute().use { response ->
                Pair(response.code, response.body.string())
            }
        }

    suspend fun json(url: String, body: JsonObject): Pair<Int, String> =
        executeRequest(fallback = { 500 to "" }) {
            val request = Request.Builder()
                .url(url)
                .addHeaders()
                .post(body.toString().toRequestBody(DEFAULT_MEDIA_TYPE.toMediaTypeOrNull()))
                .build()

            client.newCall(request).execute().use { response ->
                Pair(response.code, response.body.string())
            }
        }

    suspend fun upload(url: String, file: File): Pair<Int, String> =
        executeRequest(fallback = { 500 to "" }) {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url(url)
                .addHeaders()
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                Pair(response.code, response.body.string())
            }
        }

    suspend fun download(url: String, file: File): Boolean = executeRequest(fallback = { false }) {
        val request = Request.Builder()
            .url(url)
            .addHeaders()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RequestException.NetworkException("Download failed with code ${response.code}")
            }

            response.body.byteStream().use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            true
        }
    }

    suspend fun put(url: String, file: File): Pair<Int, String> =
        executeRequest(fallback = { 500 to "" }) {
            val request = Request.Builder()
                .url(url)
                .addHeaders()
                .put(file.asRequestBody())
                .build()

            client.newCall(request).execute().use { response ->
                Pair(response.code, response.body.string())
            }
        }

    suspend fun mkcol(url: String): Int = executeRequest(fallback = { 500 }) {
        val request = Request.Builder()
            .url(url)
            .addHeaders()
            .method("MKCOL", null)
            .build()

        client.newCall(request).execute().use { response ->
            response.code
        }
    }

    suspend fun dir(url: String): Pair<Int, List<String>> =
        executeRequest(fallback = { 500 to emptyList() }) {
            val request = Request.Builder()
                .url(url)
                .addHeaders()
                .method("PROPFIND", "".toRequestBody())
                .header("Depth", "1")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body.string()
                Logger.d("body: $body")
                Pair(response.code, parseResponse(body))
            }
        }

    private fun parseResponse(xml: String): List<String> {
        val data = xml.replace("D:response", "d:response").replace("D:href", "d:href")
        val fileList = mutableListOf<String>()

        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            ByteArrayInputStream(data.toByteArray(StandardCharsets.UTF_8)).use { inputStream ->
                val document = builder.parse(inputStream)
                document.documentElement.normalize()

                val fileNodes: NodeList = document.getElementsByTagName("d:response")
                for (i in 0 until fileNodes.length) {
                    val fileElement = fileNodes.item(i) as Element
                    val href: String =
                        fileElement.getElementsByTagName("d:href").item(0).textContent
                    val displayName = Uri.decode(href.substring(href.lastIndexOf("/") + 1))
                    if (displayName.isNotEmpty()) {
                        fileList.add(displayName)
                    }
                }
            }
        } catch (e: Exception) {
            throw RequestException.ParseException("Failed to parse XML response", e)
        }

        return fileList.reversed()
    }
}

