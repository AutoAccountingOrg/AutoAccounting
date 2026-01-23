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
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.ezbook.server.tools.HttpUtils
import org.ezbook.server.tools.runCatchingExceptCancel
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

/**
 * HTTP请求工具类
 * 提供各种HTTP请求方法的封装
 */
class RequestsUtils {
    companion object {
        private const val DEFAULT_TIMEOUT = 300L
        private const val DEFAULT_MEDIA_TYPE = "application/json; charset=utf-8"

        /**
         * 默认客户端：遵循系统代理设置
         */
        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .build()

        /**
         * 禁用代理的客户端：用于需要直连本机回环地址等场景
         */
        private val noProxyClient: OkHttpClient = OkHttpClient.Builder()
            .proxy(java.net.Proxy.NO_PROXY)
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }



    private val headers = HashMap<String, String>()

    /**
     * 当前使用的 OkHttpClient 实例。
     * - 默认使用系统代理（defaultClient）
     * - 调用 noProxy() 后切换为禁用代理的实例（noProxyClient）
     */
    private var client: OkHttpClient = defaultClient

    fun addHeader(key: String, value: String) {
        headers[key] = value
    }

    /**
     * 禁用代理：切换到不使用任何代理的 OkHttpClient。
     * 典型场景：访问 127.0.0.1/localhost 回环地址，避免被系统代理劫持。
     */
    fun noProxy() = apply {
        client = noProxyClient
    }

    private fun Request.Builder.addHeaders() = apply {
        headers.forEach { (key, value) ->
            addHeader(key, value)
        }
    }


    /**
     * 统一执行请求并返回字符串响应体
     * - 成功: 返回 body（空则返回空字符串）
     * - 失败: 抛出包含 HTTP 状态码和错误信息的异常
     */
    private fun executeAndGetBody(request: Request): String {
        // HttpUtils.logRawHttpRequest(request)
        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                // 构建包含状态码的错误信息
                val errorMsg = when {
                    body.isNotEmpty() -> "HTTP ${resp.code}: $body"
                    else -> "HTTP ${resp.code}: ${resp.message}"
                }
                throw HttpException(resp.code, errorMsg)
            }
            return body
        }
    }

    /**
     * HTTP 异常类 - 携带状态码和错误信息
     */
    class HttpException(val code: Int, message: String) : Exception(message)


    suspend fun get(url: String, query: Map<String, String>? = null): Result<String> =
        runCatchingExceptCancel {
            val httpUrl = url.toHttpUrlOrNull()
                ?: error("Invalid url: $url")

            val finalUrl = httpUrl.newBuilder().apply {
                query?.forEach { (k, v) -> addQueryParameter(k, v) }
            }.build()

            val request = Request.Builder()
                .url(finalUrl)
                .addHeaders()
                .build()

            executeAndGetBody(request)
        }


    suspend fun form(url: String, body: HashMap<String, String>? = null): Result<String> =
        runCatchingExceptCancel {
            val formBuilder = FormBody.Builder()
            body?.forEach { (key, value) ->
                formBuilder.add(key, value)
            }

            val request = Request.Builder()
                .url(url)
                .addHeaders()
                .post(formBuilder.build())
                .build()

            executeAndGetBody(request)
        }

    suspend fun jsonStr(url: String, payload: String): Result<String> =
        runCatchingExceptCancel {
            val request = Request.Builder()
                .url(url)
                .addHeaders()
                .post(payload.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            executeAndGetBody(request)
        }

    suspend fun json(url: String, body: JsonObject): Result<String> =
        runCatchingExceptCancel {
            val request = Request.Builder()
                .url(url)
                .addHeaders()
                .post(body.toString().toRequestBody(DEFAULT_MEDIA_TYPE.toMediaTypeOrNull()))
                .build()

            executeAndGetBody(request)
        }

    suspend fun upload(url: String, file: File): Result<String> =
        runCatchingExceptCancel {
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

            executeAndGetBody(request)
        }

    suspend fun download(url: String, file: File): Result<Unit> = runCatchingExceptCancel {
        val request = Request.Builder()
            .url(url)
            .addHeaders()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorMsg = "HTTP ${response.code}: ${response.message}"
                throw HttpException(response.code, errorMsg)
            }

            response.body?.byteStream().use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream?.copyTo(outputStream)
                }
            }
            Unit
        }
    }

    // 模仿 get：仅在成功时返回 body，失败抛出错误，不返回状态码
    suspend fun put(url: String, file: File): Result<String> =
        runCatchingExceptCancel {
            val request = Request.Builder()
                .url(url)
                .addHeaders()
                .put(file.asRequestBody())
                .build()

            executeAndGetBody(request)
        }

    // 模仿 get：成功返回 Unit，失败抛出错误，不返回状态码
    suspend fun mkcol(url: String): Result<Unit> = runCatchingExceptCancel {
        val request = Request.Builder()
            .url(url)
            .addHeaders()
            .method("MKCOL", null)
            .build()

        executeAndGetBody(request)
        Unit
    }

    // 模仿 get：仅在成功时返回 body，失败抛出错误，不返回状态码
    suspend fun delete(url: String): Result<String> = runCatchingExceptCancel {
        val request = Request.Builder()
            .url(url)
            .addHeaders()
            .delete()
            .build()

        executeAndGetBody(request)
    }

    // 模仿 get：仅在成功时返回解析后的列表，失败抛出错误，不返回状态码
    suspend fun dir(url: String): Result<List<String>> =
        runCatchingExceptCancel {
            val request = Request.Builder()
                .url(url)
                .addHeaders()
                .method("PROPFIND", "".toRequestBody())
                .header("Depth", "1")
                .build()

            val body = executeAndGetBody(request)
            Logger.d("body: $body")
            parseResponse(body)
        }

    private fun parseResponse(xml: String): List<String> {
        val data = xml.replace("D:response", "d:response").replace("D:href", "d:href")
        val fileList = mutableListOf<String>()

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
        return fileList.reversed()
    }
}

