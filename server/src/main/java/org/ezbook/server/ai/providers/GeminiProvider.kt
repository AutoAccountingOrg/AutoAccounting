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
                val body = response.body?.string() ?: throw RuntimeException("Empty response body")
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
    override suspend fun request(system: String, user: String): String? =
        withContext(Dispatchers.IO) {
            val url = "$apiUri/$model:generateContent?key=${getApiKey()}"
            val requestBody = mapOf(
                "contents" to listOf(
                    mapOf(
                        "role" to "user",
                        "parts" to listOf(
                            mapOf("text" to (if (system.isNotEmpty()) "$system\n$user" else user))
                        )
                    )
                )
            )
            val request = Request.Builder()
                .url(url)
                .post(
                    gson.toJson(requestBody).toRequestBody("application/json".toMediaTypeOrNull())
                )
                .addHeader("Content-Type", "application/json")
                .build()
            runCatching {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string()
                        throw RuntimeException("Request failed: ${response.code}, body: $errorBody")
                    }
                    val body = response.body?.string() ?: return@withContext null
                    val jsonObject = JsonParser.parseString(body).asJsonObject

                    return@withContext jsonObject
                        .getAsJsonArray("candidates")
                        ?.firstOrNull()
                        ?.asJsonObject
                        ?.getAsJsonObject("content")
                        ?.getAsJsonArray("parts")
                        ?.firstOrNull()
                        ?.asJsonObject
                        ?.get("text")
                        ?.asString
                }
            }.onFailure {
                Log.e("Request", "${it.message}", it)
            }.getOrElse { null }
        }
}
