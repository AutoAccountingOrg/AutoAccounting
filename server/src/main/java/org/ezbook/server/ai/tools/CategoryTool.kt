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

package org.ezbook.server.ai.tools

import io.github.oshai.kotlinlogging.KotlinLogging
import org.ezbook.server.ai.AiManager
import org.ezbook.server.db.Db

class CategoryTool {

    private val logger = KotlinLogging.logger(this::class.java.name)

    private val prompt = """
# Role
You select exactly one category name from Category Data.

# Inputs
Fields: ruleName, shopName, shopItem

# Category Data
- A comma-separated list of valid category names.
- You MUST choose one exactly from this list. Do not invent, translate, or combine names.
- Exception: if uncertain after matching, output 其他.

# Output
- Raw text, single line: the chosen category name only.
- No quotes, no JSON, no explanations, no comments, no extra whitespace.
- If uncertain, output 其他.

# Matching rules (apply in order)
1) Exact equality (case-sensitive): compare against shopItem, then shopName, then ruleName.
2) Case-insensitive equality.
3) Substring/contains match. Prefer the candidate with the longest overlap.
4) If still uncertain, output 其他.

# Tie-breakers
- Prefer shopItem over shopName over ruleName.
- Prefer longer and more specific matches.
- Except the fallback 其他, never output a name that is not in Category Data.

# Example Input
{"shopName": "钱塘江超市", "shopItem": "上好佳薯片", "ruleName": "支付宝红包"}

# Example Output
购物
""".trimIndent()

    suspend fun execute(data: String): String? {
        // 记录输入摘要，避免日志过长
        logger.debug { "分类匹配请求：data=${data.take(120)}" }

        val categories = Db.get().categoryDao().all()
        // 记录分类候选规模
        logger.debug { "分类候选统计：total=${categories.size}" }
        val categoryNames = categories.joinToString(",") { it.name.toString() }
        val user = """
Input:
- Raw Data: 
  ```
  $data
  ```
- Category Data:
  ```
  $categoryNames
  ```      
        """.trimIndent()

        // 调用 AI 进行分类选择
        logger.debug { "调用AI进行分类匹配..." }
        val resp = AiManager.getInstance().request(prompt, user).getOrThrow()
        if (resp.isEmpty()) {
            // AI 无返回
            logger.debug { "AI分类返回空响应" }
            return null
        }
        // 记录 AI 的原始输出（期望为单行纯文本）
        logger.debug { "AI分类结果：$resp" }
        return resp
    }
}