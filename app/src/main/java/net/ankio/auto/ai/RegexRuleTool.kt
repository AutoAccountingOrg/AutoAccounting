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

import com.google.gson.Gson
import com.google.gson.JsonObject
import net.ankio.auto.http.api.AiAPI
import net.ankio.auto.storage.Logger

/**
 * AI 正则规则生成工具
 *
 * 功能：根据账单数据自动生成正则表达式规则
 * 用途：帮助用户快速创建账单解析规则，无需手写正则
 */
object RegexRuleTool {

    /**
     * 系统提示词 - 账单数据正则规则生成专家
     *
     * 核心任务：根据账单文本数据生成精确的正则表达式规则
     */
    private const val SYSTEM_PROMPT = """你是账单数据正则规则生成专家。根据用户提供的账单数据，生成能准确提取信息的正则表达式。

## 核心规则

1. **正则表达式要求**
   - 必须使用捕获组 `()` 提取关键信息
   - 在JSON字段中用 `$1`、`$2`... 引用捕获组
   - 正则必须能匹配完整账单数据，不能只匹配部分

2. **字段提取规则**
   - money: 金额（必填，纯数字，如 "125.50"）
   - shopName: 商户名称（尽可能详细，如 "美团外卖-肯德基"）
   - shopItem: 商品信息（尽可能详细，如 "香辣鸡腿堡套餐"）
   - accountNameFrom: 付款账户（支出/转账时必填，如 "招商银行(尾号1234)"）
   - accountNameTo: 收款账户（收入/转账时必填）
   - fee: 手续费（负数）、优惠（正数）（可选，纯数字）
   - time: 时间（格式保持原样，如 "12月29日 14:30"）

3. **交易类型**
   - type: "Expend" (支出) / "Income" (收入) / "Transfer" (转账)
   - currency: 默认 "CNY"，除非明确标注其他币种

4. **输出格式**
   - 严格的JSON格式，不要markdown代码块包裹
   - 空值字段输出空字符串 ""，不要省略字段
   - name字段填写规则名称（简短描述，如 "美团外卖消费"）

## 输出示例

{
    "name": "微信支付-美团外卖",
    "regex": "美团外卖.*?商户：(.+?)\\s.*?商品：(.+?)\\s.*?金额：(\\d+\\.\\d+)元.*?(\\d+月\\d+日 \\d+:\\d+)",
    "type": "Expend",
    "money": "$3",
    "shopName": "$1",
    "shopItem": "$2",
    "time": "$4",
    "accountNameFrom": "",
    "accountNameTo": "",
    "fee": "",
    "currency": "CNY"
}

## 约束
- 不要输出任何解释性文字，只输出JSON
- 如果数据无法解析，返回 {"error": "原因"}
"""

    /**
     * 根据账单数据生成正则规则
     *
     * @param billData 账单原始数据文本
     * @param existingRegex 现有正则表达式（可选，用于优化现有规则）
     * @return 正则规则JSON对象，失败时返回null
     */
    suspend fun generateRegexRule(billData: String, existingRegex: String = ""): JsonObject? {
        // 检查输入是否有效
        if (billData.isBlank()) {
            Logger.w("账单数据为空")
            return null
        }

        // 构建用户输入提示词
        val userInput = buildString {
            append("## 账单数据\n$billData")
            if (existingRegex.isNotBlank()) {
                append("\n\n## 现有正则（需要优化）\n$existingRegex")
            }
        }

        // 调用AI生成规则，链式处理结果
        return AiAPI.request(SYSTEM_PROMPT, userInput)
            .getOrNull()
            ?.replace("```json", "")
            ?.replace("```", "")
            ?.let { cleanedJson ->
                runCatching { Gson().fromJson(cleanedJson, JsonObject::class.java) }.getOrNull()
            }
    }

}

