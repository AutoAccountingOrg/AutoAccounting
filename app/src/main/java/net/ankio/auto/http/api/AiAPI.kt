package net.ankio.auto.http.api

import com.google.gson.Gson
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.http.LocalNetwork
import org.ezbook.server.tools.runCatchingExceptCancel

object AiAPI {

    private val logger = KotlinLogging.logger(this::class.java.name)

    /**
     * 列出所有 AI 提供者名称（后端保留）
     */
    suspend fun getProviders(): List<String> = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.get<List<String>>("/ai/providers").getOrThrow()
            resp.data ?: emptyList()
        }.getOrElse {
            logger.error(it) { "getProviders gson error: ${it.message}" }
            emptyList()
        }
    }

    /**
     * 获取 Provider 信息（apiUri、apiModel）
     */
    suspend fun getInfo(provider: String): Map<String, String> = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.get<Map<String, String>>("/ai/info?provider=${provider}").getOrThrow()
            resp.data ?: emptyMap()
        }.getOrElse {
            logger.error(it) { "getInfo error: ${it.message}" }
            emptyMap()
        }
    }

    /**
     * 列出可用模型（POST），支持传入 provider 与 apiKey
     */
    suspend fun getModels(provider: String, apiKey: String, apiUri: String? = null): List<String> =
        withContext(Dispatchers.IO) {

            return@withContext runCatchingExceptCancel {
                val payload = mutableMapOf(
                    "provider" to provider, "apiKey" to apiKey
                )
                apiUri?.takeIf { it.isNotEmpty() }?.let { payload["apiUri"] = it }
                val body = Gson().toJson(payload)
                val resp = LocalNetwork.post<List<String>>("/ai/models", body).getOrThrow()
                resp.data ?: emptyList()
            }.getOrElse {
                logger.error(it) { "getModels error: ${it.message}" }
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
    ): Result<String> = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val payload = mutableMapOf(
                "system" to systemPrompt, "user" to userPrompt
            )
            provider?.takeIf { it.isNotEmpty() }?.let { payload["provider"] = it }
            apiKey?.takeIf { it.isNotEmpty() }?.let { payload["apiKey"] = it }
            apiUri?.takeIf { it.isNotEmpty() }?.let { payload["apiUri"] = it }
            model?.takeIf { it.isNotEmpty() }?.let { payload["model"] = it }

            val body = Gson().toJson(payload)
            val resp = LocalNetwork.post<String>("/ai/request", body).getOrThrow()
            if (resp.code != 200) {
                throw Exception(resp.msg)
            }
            Result.success(resp.data ?: "")
        }.getOrElse {
            logger.error(it) { "request error: ${it.message}" }
            Result.failure(it)
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
            logger.debug { "开始流式AI请求" }
            val payload = mutableMapOf(
                "system" to systemPrompt, "user" to userPrompt
            )
            provider?.takeIf { it.isNotEmpty() }?.let { payload["provider"] = it }
            apiKey?.takeIf { it.isNotEmpty() }?.let { payload["apiKey"] = it }
            apiUri?.takeIf { it.isNotEmpty() }?.let { payload["apiUri"] = it }
            model?.takeIf { it.isNotEmpty() }?.let { payload["model"] = it }

            val body = Gson().toJson(payload)
            logger.debug { "请求体: ${body.take(200)}..." }

            LocalNetwork.postStream("/ai/request/stream", body) { event, data ->
                logger.debug { "收到SSE事件: event=$event, data=${data.take(100)}..." }
                when (event) {
                    "message" -> {
                        // 处理标准SSE消息
                        if (data == "[DONE]") {
                            logger.debug { "流式请求完成" }
                            onComplete()
                        } else if (data.startsWith("{\"type\":\"connected\"}")) {
                            logger.debug { "SSE连接已建立" }
                        } else {
                            logger.debug {
                                "收到数据块: ${data.take(50)}..."
                            }
                            onChunk(data)
                        }
                    }

                    "chunk" -> {
                        logger.debug {
                            "收到chunk数据: ${data.take(50)}..."
                        }
                        onChunk(data)
                    }

                    "done" -> {
                        logger.debug { "流式请求完成" }
                        onComplete()
                    }

                    "error" -> {
                        logger.error { "流式请求错误: $data" }
                        onError(data)
                    }

                    else -> {
                        logger.debug { "未知事件类型: $event, 数据: $data" }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "requestStream error: ${e.message}" }
            onError(e.message ?: "Unknown error")
        }
    }
}