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
 * 讯飞星火 API提供商实现
 */
class SparkProvider : BaseOpenAIProvider() {
    override val name: String = "spark"

    override val createKeyUri: String = "https://console.xfyun.cn/services/cbm"

    override val apiUri: String = "https://spark-api-open.xf-yun.com/v1"

    override var model: String = "lite"

    override suspend fun getAvailableModels(): List<String> {
        return listOf(
            model
        ) //只有lite版本好用
    }
} 