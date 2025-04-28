package org.ezbook.server.ai.providers

import org.ezbook.server.constant.Setting
import org.ezbook.server.db.Db

/**
 * AI提供商的基础抽象类
 * 提供通用的key获取实现
 */
abstract class BaseAIProvider {
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


    var apiKey: String = ""

    abstract val apiUri: String

    abstract var model: String

    suspend fun getApiKey(): String {
        return Db.get().settingDao().query("${Setting.API_KEY}_$name")?.value ?: apiKey
    }

    suspend fun getApiUri(): String {
        return Db.get().settingDao().query("${Setting.API_URI}_$name")?.value ?: apiUri
    }

    suspend fun getModel(): String {
        return Db.get().settingDao().query("${Setting.API_MODEL}_$name")?.value ?: model
    }

    /**
     * 发送请求到AI服务
     */
    abstract suspend fun request(system: String, user: String): String?


}