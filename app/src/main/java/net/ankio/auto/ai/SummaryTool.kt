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
import net.ankio.auto.http.api.BillAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.PrefManager
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * AI账单总结工具
 *
 * 功能概览：
 * 1. 根据指定时间范围生成账单总结报告
 * 2. 支持自定义Prompt模板
 * 3. 服务端生成摘要，客户端零计算
 */
object SummaryTool {

    private val logger = KotlinLogging.logger(this::class.java.name)


    /**
     * 生成自定义时间范围的账单总结
     *
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @param periodName 周期名称（用于显示）
     * @return AI生成的总结报告，失败时返回null
     */
    suspend fun generateCustomPeriodSummary(
        startTime: Long,
        endTime: Long,
        periodName: String
    ): String? {
        // 检查AI月度总结功能是否启用
        if (!PrefManager.aiMonthlySummary) {
            logger.warn { "AI月度总结功能未启用" }
            return null
        }

        // 获取服务端生成的摘要字符串
        val dataSummary = BillAPI.getBillSummary(startTime, endTime, periodName)
        if (dataSummary == null) {
            logger.error { "获取账单摘要失败" }
            return null
        }

        // 构建完整的用户输入
        val userInput = """
请根据以下账单数据生成财务总结报告，包括收支分析、分类统计和理财建议：

时间范围：$periodName

$dataSummary

请生成详细的财务分析报告。
        """.trimIndent()

        // 调用AI生成总结
        val result = AiAPI.request(PrefManager.aiSummaryPrompt, userInput)
        return if (result.isSuccess) result.getOrNull() else null
    }

}