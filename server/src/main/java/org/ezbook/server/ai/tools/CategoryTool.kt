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

import org.ezbook.server.ai.AiManager
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.db.Db
import org.ezbook.server.log.ServerLog
import org.ezbook.server.tools.SettingUtils
import org.ezbook.server.tools.runCatchingExceptCancel

class CategoryTool {

    /**
     * 获取分类识别提示词
     * 优先使用用户自定义的提示词，如果为空则使用默认值
     */
    private suspend fun getPrompt(): String {
        val customPrompt = SettingUtils.aiCategoryRecognitionPrompt()
        return customPrompt.ifBlank {
            DefaultData.AI_CATEGORY_RECOGNITION_PROMPT
        }
    }

    /**
     * 执行分类识别
     * @param data 原始账单数据(JSON字符串)
     * @param app 来源应用
     * @param dataType 数据来源类型
     */
    suspend fun execute(
        data: String,
        app: String,
        dataType: org.ezbook.server.constant.DataType
    ): String? {
        val prompt = getPrompt()
        // 记录输入摘要，避免日志过长
        ServerLog.d("分类匹配请求：data=${data.take(120)}, app=$app, dataType=$dataType")

        val categories = Db.get().categoryDao().all()
        // 记录分类候选规模
        ServerLog.d("分类候选统计：total=${categories.size}")
        val categoryNames = categories.joinToString(",") { it.name.toString() }
        // 组装上下文信息，帮助 AI 进行语义分类
        val user = """
Input:
- Context:
  - Source App: $app
  - Data Type: $dataType
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
        ServerLog.d("调用AI进行分类匹配...")

        return runCatchingExceptCancel {
            val resp = AiManager.getInstance().request(prompt, user).getOrThrow()
            if (resp.isEmpty()) {
                // AI 无返回
                ServerLog.d("AI分类返回空响应")
                return null
            }
            // 记录 AI 的原始输出（期望为单行纯文本）
            ServerLog.d("AI分类结果：$resp")
            resp
        }.getOrDefault("其他")
    }
}