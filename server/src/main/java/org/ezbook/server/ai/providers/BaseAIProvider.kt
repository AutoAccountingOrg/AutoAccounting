package org.ezbook.server.ai.providers

import com.google.gson.Gson
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.OkHttpClient
import org.ezbook.server.tools.SettingUtils
import java.util.concurrent.TimeUnit

/**
 * AI提供商的基础抽象类
 * 提供通用的key获取实现
 */
abstract class BaseAIProvider {
    companion object {
        val logger = KotlinLogging.logger(this::class.java.name)
    }

    /**
     * 提供商名称，用于获取对应的API Key
     */
    abstract val name: String

    /**
     * URI for creating API key
     */
    abstract val createKeyUri: String

    /**
     * Get available models from the provider
     * @return List of available model names
     */
    abstract suspend fun getAvailableModels(): List<String>


    abstract val apiUri: String

    abstract var model: String

    suspend fun getApiKey(): String {
        return SettingUtils.apiKey("")
    }

    suspend fun getApiUri(): String {
        return SettingUtils.apiUri(apiUri)
    }

    suspend fun getModel(): String {
        return SettingUtils.apiModel(model)
    }

    /**
     * 发送请求到AI服务（返回 Result）。
     */
    abstract suspend fun request(
        system: String, user: String, onChunk: ((String) -> Unit)? = null
    ): Result<String>


    protected val client = OkHttpClient.Builder().connectTimeout(5, TimeUnit.MINUTES).writeTimeout(5, TimeUnit.MINUTES)
        .readTimeout(5, TimeUnit.MINUTES).build()

    protected val gson = Gson()

    fun String.removeThink(): String {
        // (?si) = DOTALL + IGNORECASE => “s” 让 . 匹配换行，“i” 忽略大小写
        val regex = Regex("(?si)<think\\b[^>]*?>.*?</think>")
        return replace(regex, "")          // 去掉所有 <think>…</think>
            .replace(Regex("\\n{3,}"), "\n\n") // 压缩连续空行
            .trim()
    }
}