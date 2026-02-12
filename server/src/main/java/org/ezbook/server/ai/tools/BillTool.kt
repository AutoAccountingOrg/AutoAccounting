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
import com.google.gson.JsonParser
import org.ezbook.server.ai.AiManager
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.DataType
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.tools.DateUtils
import org.ezbook.server.log.ServerLog
import org.ezbook.server.tools.SettingUtils
import org.ezbook.server.tools.removeMarkdown
import org.ezbook.server.tools.runCatchingExceptCancel

class BillTool {

    /**
     * 获取账单识别提示词
     * 优先使用用户自定义的提示词，如果为空则使用默认值
     */
    private suspend fun getPrompt(): String {
        val customPrompt = SettingUtils.aiBillRecognitionPrompt()
        return customPrompt.ifBlank {
            DefaultData.AI_BILL_RECOGNITION_PROMPT
        }
    }

    suspend fun execute(
        data: String,
        app: String,
        dataType: DataType,
        image: String = ""
    ): BillInfoModel? {
        val prompt = getPrompt()
        val categories = Db.get().categoryDao().all()
        val expendCategoryNames = categories
            .filter { it.type.name.startsWith(BillType.Expend.name) }
            .joinToString(",") { it.name.toString() }
        val incomeCategoryNames = categories
            .filter { it.type.name.startsWith(BillType.Income.name) }
            .joinToString(",") { it.name.toString() }
        val rawDataBlock = if (image.isNotBlank()) {
            if (data.isNotBlank()) "OCR 辅助文本：\n  ```\n  $data\n  ```"
            else "见下方图片，请直接识别图片中的账单信息。"
        } else {
            "  ```\n  $data\n  ```"
        }
        val user = """
Input:
- Context:
  - Source App: $app
  - Data Type: $dataType
- Raw Data: 
$rawDataBlock
- Category Data:
  - Expend:
    ```
    $expendCategoryNames
    ```
  - Income:
    ```
    $incomeCategoryNames
    ```      
        """.trimIndent()

        return runCatchingExceptCancel {
            val resp = AiManager.getInstance().request(prompt, user, image = image)
            val data = resp.getOrThrow()

            val bill = data.removeMarkdown()
            ServerLog.d("AI分析结果: $bill")
            // 提取 timeText 并转换为时间戳（毫秒）。为空或解析失败则为 0。
            val json = JsonParser.parseString(bill).asJsonObject
            val timeText = if (json.has("timeText")) json.get("timeText").asString else ""
            val now = System.currentTimeMillis()
            val ts =
                if (timeText.isNotBlank()) kotlin.runCatching { DateUtils.toEpochMillis(timeText) }
                    .getOrElse { now } else now

            val billInfoModel = Gson().fromJson(bill, BillInfoModel::class.java)
            if (ts > 0) billInfoModel.time = ts
            if (billInfoModel.money < 0) billInfoModel.money = -billInfoModel.money
            if (billInfoModel.money == 0.0) return null
            billInfoModel
        }.onFailure {
            ServerLog.e("AI分析结果解析失败: $it", it)
        }.getOrNull()
    }
}