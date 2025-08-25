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
     * 列出所有 AI 提供者名称
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
     * 获取当前选中的提供者
     */
    suspend fun getCurrentProvider(): String = withContext(Dispatchers.IO) {
        val resp = LocalNetwork.get("/ai/provider")
        val type = object : TypeToken<ApiResponse<String>>() {}.type
        return@withContext try {
            val apiResp: ApiResponse<String> = gson.fromJson(resp, type)
            apiResp.data ?: ""
        } catch (e: Exception) {
            Logger.e("getCurrentProvider gson error: ${e.message}", e)
            ""
        }
    }

    /**
     * 设置当前提供者
     */
    suspend fun setCurrentProvider(name: String) = withContext(Dispatchers.IO) {
        try {
            val body = gson.toJson(mapOf("name" to name))
            LocalNetwork.post("/ai/provider", body)
        } catch (e: Exception) {
            Logger.e("setCurrentProvider gson/req error: ${e.message}", e)
        }
    }

    /**
     * 获取当前 API Key
     */
    suspend fun getApiKey(): String = withContext(Dispatchers.IO) {
        val resp = LocalNetwork.get("/ai/apikey")
        val type = object : TypeToken<ApiResponse<String>>() {}.type
        return@withContext try {
            val apiResp: ApiResponse<String> = gson.fromJson(resp, type)
            apiResp.data ?: ""
        } catch (e: Exception) {
            Logger.e("getApiKey gson error: ${e.message}", e)
            ""
        }
    }

    /**
     * 设置 API Key
     */
    suspend fun setApiKey(key: String) = withContext(Dispatchers.IO) {
        try {
            val body = gson.toJson(mapOf("key" to key))
            LocalNetwork.post("/ai/apikey", body)
        } catch (e: Exception) {
            Logger.e("setApiKey gson/req error: ${e.message}", e)
        }
    }

    /**
     * 获取当前 API URL
     */
    suspend fun getApiUrl(): String = withContext(Dispatchers.IO) {
        val resp = LocalNetwork.get("/ai/apiurl")
        val type = object : TypeToken<ApiResponse<String>>() {}.type
        return@withContext try {
            val apiResp: ApiResponse<String> = gson.fromJson(resp, type)
            apiResp.data ?: ""
        } catch (e: Exception) {
            Logger.e("getApiUrl gson error: ${e.message}", e)
            ""
        }
    }

    /**
     * 设置 API URL
     */
    suspend fun setApiUrl(url: String) = withContext(Dispatchers.IO) {
        try {
            val body = gson.toJson(mapOf("url" to url))
            LocalNetwork.post("/ai/apiurl", body)
        } catch (e: Exception) {
            Logger.e("setApiUrl gson/req error: ${e.message}", e)
        }
    }

    /**
     * 列出可用模型
     */
    suspend fun getModels(): List<String> = withContext(Dispatchers.IO) {
        val resp = LocalNetwork.get("/ai/models")
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
     * 获取当前模型
     */
    suspend fun getCurrentModel(): String = withContext(Dispatchers.IO) {
        val resp = LocalNetwork.get("/ai/model")
        val type = object : TypeToken<ApiResponse<String>>() {}.type
        return@withContext try {
            val apiResp: ApiResponse<String> = gson.fromJson(resp, type)
            apiResp.data ?: ""
        } catch (e: Exception) {
            Logger.e("getCurrentModel gson error: ${e.message}", e)
            ""
        }
    }

    /**
     * 设置当前模型
     */
    suspend fun setCurrentModel(model: String) = withContext(Dispatchers.IO) {
        try {
            val body = gson.toJson(mapOf("model" to model))
            LocalNetwork.post("/ai/model", body)
        } catch (e: Exception) {
            Logger.e("setCurrentModel gson/req error: ${e.message}", e)
        }
    }

    /**
     * 获取创建 API Key 的链接
     */
    suspend fun getCreateKeyUri(): String = withContext(Dispatchers.IO) {
        val resp = LocalNetwork.get("/ai/createKeyUri")
        val type = object : TypeToken<ApiResponse<String>>() {}.type
        return@withContext try {
            val apiResp: ApiResponse<String> = gson.fromJson(resp, type)
            apiResp.data ?: ""
        } catch (e: Exception) {
            Logger.e("getCreateKeyUri gson error: ${e.message}", e)
            ""
        }
    }

    /**
     * 向 AI 服务发送对话请求
     */
    suspend fun request(systemPrompt: String, userPrompt: String): String =
        withContext(Dispatchers.IO) {
            try {
                val body = gson.toJson(mapOf("system" to systemPrompt, "user" to userPrompt))
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
        onError: (String) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        try {
            Logger.d("开始流式AI请求")
            val body = gson.toJson(mapOf("system" to systemPrompt, "user" to userPrompt))
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