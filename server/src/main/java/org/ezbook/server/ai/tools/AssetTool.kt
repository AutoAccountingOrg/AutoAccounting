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

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.ezbook.server.ai.AiManager
import org.ezbook.server.constant.AssetsType
import org.ezbook.server.db.Db
import org.ezbook.server.tools.ServerLog
import org.ezbook.server.tools.runCatchingExceptCancel

class AssetTool {
    private val prompt = """
# Role
You select asset names strictly from Asset Data.

# Inputs
Fields (may be empty): asset1, asset2

# Asset Data
- A comma-separated list of valid asset names.
- You MUST choose exactly from this list. Do not invent, translate, or combine names.

# Output (strict JSON only)
- Return ONLY a JSON object with exactly two keys:
  {"asset1":"<name-or-empty>", "asset2":"<name-or-empty>"}
- If a clue has no match, set its value to an empty string: "".
- No extra fields, no explanations, no markdown, no text outside JSON.

# Matching rules (apply in order, independently for each clue)
1) Exact equality (case-sensitive)
2) Case-insensitive equality
3) Substring/contains match; prefer the candidate with the longest overlap
4) If multiple candidates tie, prefer the longer candidate name
5) If still uncertain, use ""

# Example Input
{"asset1":"中国银行储蓄卡","asset2":"支付宝"}

# Example Output
{"asset1":"中国银行","asset2":"支付宝"}

# Example Output (asset2 not found)
{"asset1":"中国银行","asset2":""}
""".trimIndent()

    suspend fun execute(asset1: String, asset2: String): JsonObject? {
        // 记录输入参数，便于问题复现与排查
        ServerLog.d("资产匹配请求：asset1=$asset1, asset2=$asset2")
        val data = Gson().toJson(
            hashMapOf(
                "asset1" to asset1,
                "asset2" to asset2
            )
        )
        val assets = Db.get().assetsDao().load()
        val assetsNames = assets
            .filter { it.type != AssetsType.BORROWER && it.type != AssetsType.CREDITOR }
            .map { it.name.trim() }
            .distinct()                         // 去重且保持原顺序
            .joinToString(",")
        // 记录候选资产规模，避免日志过长不打印全部列表
        ServerLog.d("资产候选统计：total=${assets.size}, usable=${assetsNames.split(',').size}")
        val user = """
Input:
- Raw Data: 
  ```
  $data
  ``` 
- Assets Data:
  ```
  $assetsNames
  ```     
        """.trimIndent()

        // 调用 AI 进行资产名匹配
        ServerLog.d("调用AI进行资产匹配...")


        // 解析 AI 响应为 JSON，如失败则记录错误并返回空
        return runCatchingExceptCancel {
            val resp = AiManager.getInstance().request(prompt, user).getOrThrow()
            // 打印原始响应（严格JSON预期），便于快速定位问题
            ServerLog.d("AI资产匹配原始响应：$resp")
            Gson().fromJson(resp, JsonObject::class.java)
        }
            .onFailure { ServerLog.e("AI资产匹配JSON解析失败：${it.message}", it) }
            .getOrNull()
            .also { ServerLog.d("AI资产匹配解析结果：$it") }
    }
}