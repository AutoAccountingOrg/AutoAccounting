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
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.db.Db
import org.ezbook.server.log.ServerLog
import org.ezbook.server.tools.SettingUtils
import org.ezbook.server.tools.runCatchingExceptCancel

class AssetTool {

    /**
     * 获取资产映射提示词
     * 优先使用用户自定义的提示词，如果为空则使用默认值
     */
    private suspend fun getPrompt(): String {
        val customPrompt = SettingUtils.aiAssetMappingPrompt()
        return customPrompt.ifBlank {
            DefaultData.AI_ASSET_MAPPING_PROMPT
        }
    }

    /**
     * 执行资产映射
     * @param asset1 目标账户线索
     * @param asset2 来源账户线索
     * @param app 来源应用
     * @param billType 账单类型
     */
    suspend fun execute(
        asset1: String,
        asset2: String,
        app: String,
        billType: org.ezbook.server.constant.BillType
    ): JsonObject? {
        val prompt = getPrompt()
        // 记录输入参数，便于问题复现与排查
        ServerLog.d("资产匹配请求：asset1=$asset1, asset2=$asset2, app=$app, billType=$billType")
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
        // 组装上下文信息，帮助 AI 区分同名资产
        val user = """
Input:
- Context:
  - Source App: $app
  - Bill Type: $billType
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