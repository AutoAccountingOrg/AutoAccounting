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
 * 硅基流动 API提供商实现
 */
class SiliconFlowProvider : BaseOpenAIProvider() {
    override val name: String = "sf"

    override val createKeyUri: String = "https://cloud.siliconflow.cn/me/account/ak"


    override val apiUri: String = "https://api.siliconflow.cn"

    override var model: String = "Qwen/QwQ-32B"


}