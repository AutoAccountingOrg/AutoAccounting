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

package org.ezbook.server.ai.providers

/**
 * 通义千问 API提供商实现
 */
class QWenProvider : BaseOpenAIProvider() {
    override val name: String = "qwen"

    override val createKeyUri: String =
        "https://bailian.console.aliyun.com/cn-beijing/?tab=model#/api-key"
    override suspend fun getAvailableModels(): List<String> {
        return listOf(
            model,       // 速度快、成本低
            "qwen-plus",        // 能力、成本均衡
            "qwen-max",         // 推理能力最强
            "qwen-max-latest",  // max 的滚动最新快照
            "qwen-long",        // 128K~1M 长上下文
            "qwen-plus-long"    // Plus 版的长上下文
        )
    }

    override val apiUri: String = "https://dashscope.aliyuncs.com/compatible-mode"

    override var model: String = "qwen-turbo"


}