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
import org.ezbook.server.ai.providers.BigModelProvider
import org.ezbook.server.ai.providers.ChatGPTProvider
import org.ezbook.server.ai.providers.DeepSeekProvider
import org.ezbook.server.ai.providers.GeminiProvider
import org.ezbook.server.ai.providers.KimiProvider
import org.ezbook.server.ai.providers.MiMoProvider
import org.ezbook.server.ai.providers.OpenRouterProvider
import org.ezbook.server.ai.providers.QWenProvider
import org.ezbook.server.ai.providers.SiliconFlowProvider
import org.ezbook.server.tools.SettingUtils

/**
 * AI 管理器
 * 管理AI提供者，支持设置选择的AI提供者的url、模型、key
 */
class AiManager {



    // 所有可用的AI提供者列表
    private val providers = mapOf(
        "ChatGPT" to ChatGPTProvider(),
        "DeepSeek" to DeepSeekProvider(),
        "Gemini" to GeminiProvider(),
        "Kimi" to KimiProvider(),
        "智谱清言" to BigModelProvider(),
        "OpenRouter" to OpenRouterProvider(),
        "通义千问" to QWenProvider(),
        "硅基流动" to SiliconFlowProvider(),
        "小米MiMo" to MiMoProvider()
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
    private suspend fun getCurrentProvider(): BaseAIProvider {
        val providerName = SettingUtils.apiProvider()
        return getProvider(providerName)
    }

    fun getProvider(providerName: String): BaseAIProvider {
        return providers[providerName] ?: providers["DeepSeek"]!!
    }

    suspend fun getAvailableModels(providerName: String): List<String> =
        getProvider(providerName).getAvailableModels()

    suspend fun getProviderInfo(providerName: String): HashMap<String, String> {
        val provider = getProvider(providerName)
        return hashMapOf(
            "apiUri" to provider.apiUri,
            "apiModel" to provider.model,
            "createKeyUri" to provider.createKeyUri
        )
    }
    /**
     * 发送请求到当前AI服务
     * @param system 系统提示
     * @param user 用户提示
     * @return AI响应
     */
    suspend fun request(
        system: String,
        user: String,
        provider: BaseAIProvider? = null
    ): Result<String> {
        return (provider ?: getCurrentProvider()).request(system, user)
    }


    /**
     * 发送流式请求到当前AI服务
     * @param system 系统提示
     * @param user 用户提示
     * @param onChunk 接收到数据块时的回调函数
     * @return 是否成功开始流式请求
     */
    suspend fun requestStream(
        system: String,
        user: String,
        provider: BaseAIProvider? = null,
        onChunk: (String) -> Unit
    ) {
        (provider ?: getCurrentProvider()).request(system, user, onChunk)
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