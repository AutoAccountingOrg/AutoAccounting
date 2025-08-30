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

package org.ezbook.server.ai

import android.util.Log
import org.ezbook.server.ai.providers.BaseAIProvider
import org.ezbook.server.ai.providers.ChatGPTProvider
import org.ezbook.server.ai.providers.DeepSeekProvider
import org.ezbook.server.ai.providers.GeminiProvider
import org.ezbook.server.ai.providers.KimiProvider
import org.ezbook.server.ai.providers.OpenRouterProvider
import org.ezbook.server.ai.providers.QWenProvider
import org.ezbook.server.ai.providers.SparkProvider
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.Db

/**
 * AI 管理器
 * 管理AI提供者，支持设置选择的AI提供者的url、模型、key
 */
class AiManager {

    // 当前选择的AI提供者名称
    var currentProviderName: String = "DeepSeek"

    // 所有可用的AI提供者列表
    private val providers = mapOf(
        "ChatGPT" to ChatGPTProvider(),
        "DeepSeek" to DeepSeekProvider(),
        "Gemini" to GeminiProvider(),
        "Kimi" to KimiProvider(),
        "OpenRouter" to OpenRouterProvider(),
        "讯飞星火" to SparkProvider(),
        "通义千问" to QWenProvider()
    )

    /**
     * 获取所有可用的AI提供者名称
     * @return 所有AI提供者名称列表
     */
    fun getProviderNames(): List<String> = providers.keys.toList()

    /**
     * 获取当前选择的AI提供者
     * @return 当前选择的AI提供者实例
     */
    private suspend fun getCurrentProvider(): BaseAIProvider? {

        val providerName =
            Db.get().settingDao().query(Setting.AI_MODEL)?.value ?: currentProviderName
        //默认deepseek

        return providers[providerName] ?: providers[currentProviderName]
    }

    /**
     * 设置当前选择的AI提供者
     * @param providerName AI提供者名称
     * @return 是否设置成功
     */
    suspend fun setCurrentProvider(providerName: String): Boolean {
        if (providers.containsKey(providerName)) {
            currentProviderName = providerName
            // 必须写入数据库，否则重启后丢失
            Db.get().settingDao().set(Setting.AI_MODEL, providerName)
            return true
        }
        return false
    }

    /**
     * 获取当前AI提供者的API Key
     * @return API Key
     */
    suspend fun getCurrentApiKey(): String {
        return getCurrentProvider()?.getApiKey() ?: ""
    }

    /**
     * 设置当前AI提供者的API Key
     * @param apiKey 要设置的API Key
     */
    suspend fun setCurrentApiKey(apiKey: String) {
        val provider = getCurrentProvider() ?: return

        Db.get().settingDao().set(
            "${Setting.API_KEY}_${provider.name}", apiKey
        )
        provider.apiKey = apiKey
    }

    /**
     * 获取当前AI提供者的API URL
     * @return API URL
     */
    suspend fun getCurrentApiUrl(): String {
        return getCurrentProvider()?.getApiUri() ?: ""
    }

    /**
     * 设置当前AI提供者的API URL
     * @param apiUrl 要设置的API URL
     */
    suspend fun setCurrentApiUrl(apiUrl: String) {
        val provider = getCurrentProvider() ?: return
        Db.get().settingDao().set("${Setting.API_URI}_${provider.name}", apiUrl)
    }

    /**
     * 获取当前AI提供者的模型
     * @return 模型名称
     */
    suspend fun getCurrentModel(): String {
        return getCurrentProvider()?.getModel() ?: ""
    }

    /**
     * 设置当前AI提供者的模型
     * @param model 要设置的模型名称
     */
    suspend fun setCurrentModel(model: String) {
        val provider = getCurrentProvider() ?: return
        Db.get().settingDao().set("${Setting.API_MODEL}_${provider.name}", model)
    }

    /**
     * 获取当前AI提供者可用的模型列表
     * @return 可用的模型列表
     */
    suspend fun getAvailableModels(): List<String> {
        return runCatching { getCurrentProvider()?.getAvailableModels() }.onFailure {
            Log.e("模块错误", it.message ?: "", it)
        }.getOrNull() ?: emptyList()
    }

    /**
     * 获取创建API Key的URI
     * @return 创建API Key的URI
     */
    suspend fun getCreateKeyUri(): String {
        return getCurrentProvider()?.createKeyUri ?: ""
    }

    /**
     * 发送请求到当前AI服务
     * @param system 系统提示
     * @param user 用户提示
     * @return AI响应
     */
    suspend fun request(system: String, user: String): String? {
        return getCurrentProvider()?.request(system, user)
    }


    /**
     * 发送流式请求到当前AI服务
     * @param system 系统提示
     * @param user 用户提示
     * @param onChunk 接收到数据块时的回调函数
     * @return 是否成功开始流式请求
     */
    suspend fun requestStream(system: String, user: String, onChunk: (String) -> Unit) {
        getCurrentProvider()?.request(system, user, onChunk)
    }

    companion object {
        @Volatile
        private var instance: AiManager? = null

        /**
         * 获取AiManager单例实例
         * @return AiManager实例
         */
        fun getInstance(): AiManager {
            return instance ?: synchronized(this) {
                instance ?: AiManager().also { instance = it }
            }
        }
    }
}