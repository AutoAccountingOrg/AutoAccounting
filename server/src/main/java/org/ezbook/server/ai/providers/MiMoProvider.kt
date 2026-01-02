package org.ezbook.server.ai.providers

/**
 * 小米MiMo API提供商实现
 * MiMo是小米推出的推理优先大语言模型，支持OpenAI兼容的API格式
 */
class MiMoProvider : BaseOpenAIProvider() {
    override val name: String = "mimo"

    override val createKeyUri: String = "https://platform.xiaomimimo.com/#/console/api-keys"

    override val apiUri: String = "https://api.xiaomimimo.com"

    override var model: String = "mimo-v2-flash"

    override suspend fun getAvailableModels(): List<String> {
        return listOf(
            "mimo-v2-flash"  // 当前主要模型
        )
    }
}
