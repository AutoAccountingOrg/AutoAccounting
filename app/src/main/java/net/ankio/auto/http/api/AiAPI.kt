package net.ankio.auto.http.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.http.LocalNetwork
import net.ankio.auto.storage.Logger

object AiAPI {

    private val gson: Gson by lazy { Gson() }

    private data class ApiResponse<T>(
        val code: Int,
        val message: String,
        val data: T?
    )

    /**
     * 列出所有 AI 提供者名称（后端保留）
     */
    suspend fun getProviders(): List<String> = withContext(Dispatchers.IO) {
        val resp = LocalNetwork.get("/ai/providers")
        val type = object : TypeToken<ApiResponse<List<String>>>() {}.type
        return@withContext try {
            val apiResp: ApiResponse<List<String>> = gson.fromJson(resp, type)
            apiResp.data ?: emptyList()
        } catch (e: Exception) {
            Logger.e("getProviders gson error: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * 获取 Provider 信息（apiUri、apiModel）
     */
    suspend fun getInfo(provider: String): Map<String, String> = withContext(Dispatchers.IO) {
        val resp = LocalNetwork.get("/ai/info?provider=${provider}")
        val type = object : TypeToken<ApiResponse<Map<String, String>>>() {}.type
        return@withContext try {
            val apiResp: ApiResponse<Map<String, String>> = gson.fromJson(resp, type)
            apiResp.data ?: emptyMap()
        } catch (e: Exception) {
            Logger.e("getInfo gson error: ${e.message}", e)
            emptyMap()
        }
    }

    /**
     * 列出可用模型（POST），支持传入 provider 与 apiKey
     */
    suspend fun getModels(provider: String, apiKey: String, apiUri: String? = null): List<String> =
        withContext(Dispatchers.IO) {
            val payload = mutableMapOf(
                "provider" to provider,
                "apiKey" to apiKey
            )
            apiUri?.takeIf { it.isNotEmpty() }?.let { payload["apiUri"] = it }
            val body = gson.toJson(payload)
            val resp = LocalNetwork.post("/ai/models", body)
        val type = object : TypeToken<ApiResponse<List<String>>>() {}.type
        return@withContext try {
            val apiResp: ApiResponse<List<String>> = gson.fromJson(resp, type)
            apiResp.data ?: emptyList()
        } catch (e: Exception) {
            Logger.e("getModels gson error: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * 向 AI 服务发送对话请求
     */
    suspend fun request(
        systemPrompt: String,
        userPrompt: String,
        provider: String? = null,
        apiKey: String? = null,
        apiUri: String? = null,
        model: String? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            val payload = mutableMapOf(
                "system" to systemPrompt,
                "user" to userPrompt
            )
            provider?.takeIf { it.isNotEmpty() }?.let { payload["provider"] = it }
            apiKey?.takeIf { it.isNotEmpty() }?.let { payload["apiKey"] = it }
            apiUri?.takeIf { it.isNotEmpty() }?.let { payload["apiUri"] = it }
            model?.takeIf { it.isNotEmpty() }?.let { payload["model"] = it }

            val body = gson.toJson(payload)
            val resp = LocalNetwork.post("/ai/request", body)
            val type = object : TypeToken<ApiResponse<String>>() {}.type
            val apiResp: ApiResponse<String> = gson.fromJson(resp, type)
            apiResp.data ?: ""
        } catch (e: Exception) {
            Logger.e("request gson/req error: ${e.message}", e)
            ""
        }
    }

    /**
     * 向 AI 服务发送流式对话请求
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户输入
     * @param onChunk 接收到数据块时的回调函数
     * @param onComplete 请求完成时的回调函数
     * @param onError 发生错误时的回调函数
     */
    suspend fun requestStream(
        systemPrompt: String,
        userPrompt: String,
        onChunk: (String) -> Unit,
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {},
        provider: String? = null,
        apiKey: String? = null,
        apiUri: String? = null,
        model: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            Logger.d("开始流式AI请求")
            val payload = mutableMapOf(
                "system" to systemPrompt,
                "user" to userPrompt
            )
            provider?.takeIf { it.isNotEmpty() }?.let { payload["provider"] = it }
            apiKey?.takeIf { it.isNotEmpty() }?.let { payload["apiKey"] = it }
            apiUri?.takeIf { it.isNotEmpty() }?.let { payload["apiUri"] = it }
            model?.takeIf { it.isNotEmpty() }?.let { payload["model"] = it }

            val body = gson.toJson(payload)
            Logger.d("请求体: ${body.take(200)}...")

            LocalNetwork.postStream("/ai/request/stream", body) { event, data ->
                Logger.d("收到SSE事件: event=$event, data=${data.take(100)}...")
                when (event) {
                    "message" -> {
                        // 处理标准SSE消息
                        if (data == "[DONE]") {
                            Logger.d("流式请求完成")
                            onComplete()
                        } else if (data.startsWith("{\"type\":\"connected\"}")) {
                            Logger.d("SSE连接已建立")
                        } else {
                            Logger.d("收到数据块: ${data.take(50)}...")
                            onChunk(data)
                        }
                    }

                    "chunk" -> {
                        Logger.d("收到chunk数据: ${data.take(50)}...")
                        onChunk(data)
                    }

                    "done" -> {
                        Logger.d("流式请求完成")
                        onComplete()
                    }

                    "error" -> {
                        Logger.e("流式请求错误: $data")
                        onError(data)
                    }

                    else -> {
                        Logger.d("未知事件类型: $event, 数据: $data")
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e("requestStream error: ${e.message}", e)
            onError(e.message ?: "Unknown error")
        }
    }
}