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
 *  limitations under the License.
 */

package net.ankio.auto.ai

import net.ankio.auto.http.api.AiAPI
import net.ankio.auto.storage.Logger
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * AI JavaScript代码优化工具
 *
 * 功能概览：
 * 1. 优化账单解析JS规则代码
 * 2. 保持原有功能不变，仅优化实现方式
 * 3. 提高代码可读性和性能
 */
object JsTool {

    private val logger = KotlinLogging.logger(this::class.java.name)

    /** 系统提示词 - JavaScript代码优化专家 */
    private const val SYSTEM_PROMPT = """你是一个JavaScript代码优化专家，专门优化用于解析账单数据的JS规则。

任务要求：
1. 分析用户提供的JS代码，理解其解析逻辑
2. 优化代码结构，提高可读性和性能
3. 保持原有功能不变，只优化实现方式
4. 使用更好的正则表达式、字符串处理方法
5. Income表示收入，Transfer表示转账，Expend表示支出
6. 对于解析的商户名称和商品信息要尽可能详细；
7. 如果有卡号，请务必将卡号包含在资产中；
8. 保持函数签名和返回格式不变

代码要求：
- 函数名保持原样（如 let rule_xxxx = { get(data) { ... } }）
- 返回格式必须是 { type, money, shopName, shopItem, accountNameFrom, accountNameTo, fee, currency, time, channel } 或者 null
- 只返回优化后的完整JS代码，不要添加额外说明
- 代码要简洁、高效、可读性强"""

    /**
     * 优化JavaScript代码
     *
     * @param jsCode 原始JavaScript代码
     * @return 优化后的JavaScript代码，失败时返回null
     */
    suspend fun optimizeJsCode(jsCode: String): String? {
        // 检查输入是否有效
        if (jsCode.isBlank()) {
            logger.warn { "JavaScript代码为空" }
            return null
        }

        // 构建用户输入
        val userInput = """
请优化以下JavaScript代码：

```javascript
$jsCode
```

请返回优化后的完整JavaScript代码。
        """.trimIndent()

        // 调用AI优化代码
        val result = AiAPI.request(SYSTEM_PROMPT, userInput)
        return if (result.isSuccess) result.getOrNull() else null
    }
}
