package org.ezbook.server.ai.providers

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.ezbook.server.log.ServerLog
import org.ezbook.server.tools.runCatchingExceptCancel

/**
 * Gemini API 提供商实现
 */
class GeminiProvider : BaseAIProvider() {
    override val name: String = "gemini"

    override val createKeyUri: String = "https://aistudio.google.com/app/apikey"

    override val apiUri: String = "https://generativelanguage.googleapis.com/v1beta"

    override var model: String = "models/gemini-3-flash-preview"

    private suspend fun base(): String {
        val base = getApiUri().trimEnd('/')
        return if (base.endsWith("/v1beta")) base else "$base/v1beta"
    }

    /**
     * 获取可用模型列表
     */
    override suspend fun getAvailableModels(): List<String> = withContext(Dispatchers.IO) {
        val url = "${base()}?key=${getApiKey()}"
        val request = Request.Builder()
            .url(url)
            .addUserAgent()
            .get()
            .build()

        runCatchingExceptCancel {
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
            ServerLog.e("GeminiProvider: 获取模型失败：${it.message}", it)
        }.getOrElse { emptyList() }
    }

    /**
     * 发送聊天请求
     */
    override suspend fun request(
        system: String,
        user: String,
        onChunk: ((String) -> Unit)?
    ): Result<String> =
        withContext(Dispatchers.IO) {
            val path = if (onChunk === null) "generateContent" else "streamGenerateContent?alt=sse"
            val url = "${base()}/${getModel()}:$path"
            val requestBody = mapOf(
                "contents" to listOf(
                    mapOf(
                        "role" to "user",
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
                .addUserAgent()
                .post(
                    gson.toJson(requestBody).toRequestBody("application/json".toMediaTypeOrNull())
                )
                .addHeader("x-goog-api-key", getApiKey())
                .addHeader("Content-Type", "application/json")
                .build()

            runCatchingExceptCancel {
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
                        return@withContext Result.success("") // 流式模式返回空串占位
                    } else {
                        // 非流式处理
                        val body = response.body?.string()?.removeThink() ?: ""
                        val content = parseGeminiContent(body) ?: error("Empty AI response")
                        return@withContext Result.success(content)
                    }
                }
            }.onFailure {
                ServerLog.e("GeminiProvider: 请求失败：${it.message}", it)
            }.fold(
                onSuccess = { it },
                onFailure = { Result.failure(it) }
            )
        }

    /**
     * 解析Gemini响应内容
     */
    private suspend fun parseGeminiContent(body: String): String? {
        return runCatchingExceptCancel {
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
        }.onFailure {
            ServerLog.e("GeminiProvider: 解析失败: ${it.message}", it)
        }.getOrNull()
    }
}
