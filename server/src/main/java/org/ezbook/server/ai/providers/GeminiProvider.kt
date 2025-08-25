package org.ezbook.server.ai.providers

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Gemini API 提供商实现
 */
class GeminiProvider : BaseAIProvider() {
    override val name: String = "gemini"

    override val createKeyUri: String = "https://aistudio.google.com/app/apikey"

    override val apiUri: String = "https://generativelanguage.googleapis.com/v1beta/models"

    override var model: String = "gemini-2.0-flash"


    /**
     * 获取可用模型列表
     */
    override suspend fun getAvailableModels(): List<String> = withContext(Dispatchers.IO) {
        val url = "$apiUri?key=${getApiKey()}"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw RuntimeException("Failed to get models: ${response.code}")
                val body = response.body?.string()
                val jsonObject = JsonParser.parseString(body).asJsonObject

                val models = mutableListOf<String>()
                jsonObject.getAsJsonArray("models")?.forEach { model ->
                    model.asJsonObject.get("name")?.asString?.let { models.add(it) }
                }
                models
            }
        }.onFailure {
            Log.e("Request", "${it.message}", it)
        }.getOrElse { emptyList() }
    }

    /**
     * 发送聊天请求
     */
    override suspend fun request(
        system: String,
        user: String,
        onChunk: ((String) -> Unit)?
    ): String? =
        withContext(Dispatchers.IO) {
            val path = if (onChunk === null) "generateContent" else "streamGenerateContent?alt=sse"
            val url = "$apiUri/$model:$path"
            val requestBody = mapOf(
                "contents" to listOf(
                    mapOf(
                        "role" to "system",
                        "parts" to listOf(
                            mapOf("text" to system)
                        )
                    ),
                    mapOf(
                        "role" to "user",
                        "parts" to listOf(
                            mapOf("text" to user)
                        )
                    )
                )
            )
            val request = Request.Builder()
                .url(url)
                .post(
                    gson.toJson(requestBody).toRequestBody("application/json".toMediaTypeOrNull())
                )
                .addHeader("x-goog-api-key", getApiKey())
                .addHeader("Content-Type", "application/json")
                .build()
            runCatching {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string()
                        throw RuntimeException("Request failed: ${response.code}, body: $errorBody")
                    }

                    if (onChunk != null) {
                        // 流式处理
                        val source = response.body?.source()
                        if (source != null) {
                            while (!source.exhausted()) {
                                val line = source.readUtf8Line() ?: break
                                if (line.startsWith("data: ")) {
                                    val data = line.substring(6)
                                    if (data.trim().isNotEmpty() && data != "[DONE]") {
                                        val content = parseGeminiContent(data)
                                        if (content != null) {
                                            onChunk(content)
                                        }
                                    }
                                }
                            }
                        }
                        return@withContext null // 流式模式返回null
                    } else {
                        // 非流式处理
                        val body = response.body?.string()?.removeThink() ?: ""
                        return@withContext parseGeminiContent(body)
                    }
                }
            }.onFailure {
                Log.e("Request", "${it.message}", it)
            }.getOrElse { null }
        }

    /**
     * 解析Gemini响应内容
     */
    private fun parseGeminiContent(body: String): String? {
        return try {
            val jsonObject = JsonParser.parseString(body).asJsonObject
            jsonObject
                .getAsJsonArray("candidates")
                ?.firstOrNull()
                ?.asJsonObject
                ?.getAsJsonObject("content")
                ?.getAsJsonArray("parts")
                ?.firstOrNull()
                ?.asJsonObject
                ?.get("text")
                ?.asString
        } catch (e: Exception) {
            Log.e("GeminiParse", "解析失败: ${e.message}")
            null
        }
        }
}
