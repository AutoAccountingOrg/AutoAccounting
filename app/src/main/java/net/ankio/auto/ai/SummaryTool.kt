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
import java.util.*

/**
 * AI账单总结工具
 *
 * 功能概览：
 * 1. 根据指定时间范围生成账单总结报告
 * 2. 支持自定义Prompt模板
 * 3. 服务端生成摘要，客户端零计算
 */
object SummaryTool {

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
            Logger.w("AI月度总结功能未启用")
            return null
        }

        // 获取服务端生成的摘要字符串
        val dataSummary = BillAPI.getBillSummary(startTime, endTime, periodName)
        if (dataSummary == null) {
            Logger.e("获取账单摘要失败")
            return null
        }

        // 获取用户自定义Prompt
        val customPrompt = PrefManager.aiSummaryPrompt.ifBlank {
            "请根据以下账单数据生成财务总结报告，包括收支分析、分类统计和理财建议："
        }

        // 构建完整的用户输入
        val userInput = """
$customPrompt

时间范围：$periodName

$dataSummary

请生成详细的财务分析报告。
        """.trimIndent()

        // 调用AI生成总结
        return try {
            AiAPI.request(buildSystemPrompt(), userInput)
        } catch (e: Exception) {
            Logger.e("AI总结生成失败: ${e.message}", e)
            null
        }
    }

    /**
     * 生成自定义时间范围的账单总结（流式版本）
     *
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @param periodName 周期名称（用于显示）
     * @param onChunk 接收到数据块时的回调函数
     * @param onComplete 完成时的回调函数
     * @param onError 出错时的回调函数
     */
    suspend fun generateCustomPeriodSummaryStream(
        startTime: Long,
        endTime: Long,
        periodName: String,
        onChunk: (String) -> Unit,
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        // 检查AI月度总结功能是否启用
        if (!PrefManager.aiMonthlySummary) {
            Logger.w("AI月度总结功能未启用")
            onError("AI月度总结功能未启用")
            return
        }

        // 获取服务端生成的摘要字符串
        val dataSummary = BillAPI.getBillSummary(startTime, endTime, periodName)
        if (dataSummary == null) {
            Logger.e("获取账单摘要失败")
            onError("获取账单摘要失败")
            return
        }

        // 获取用户自定义Prompt
        val customPrompt = PrefManager.aiSummaryPrompt.ifBlank {
            "请根据以下账单数据生成财务总结报告，包括收支分析、分类统计和理财建议："
        }

        // 构建完整的用户输入
        val userInput = """
$customPrompt

时间范围：$periodName

$dataSummary

请生成详细的财务分析报告。
        """.trimIndent()

        // 调用AI生成流式总结
        try {
            AiAPI.requestStream(buildSystemPrompt(), userInput, onChunk, onComplete, onError)
        } catch (e: Exception) {
            Logger.e("AI流式总结生成失败: ${e.message}", e)
            onError(e.message ?: "Unknown error")
        }
    }

    /**
     * 生成月度账单总结（保持向后兼容）
     *
     * @param year 年份
     * @param month 月份 (1-12)
     * @return AI生成的总结报告，失败时返回null
     */
    suspend fun generateMonthlySummary(year: Int, month: Int): String? {
        // 计算月度时间范围
        val calendar = Calendar.getInstance()

        // 月初时间戳
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        // 月末时间戳
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endTime = calendar.timeInMillis

        // 使用新的通用方法
        return generateCustomPeriodSummary(startTime, endTime, "${year}年${month}月")
    }

    /**
     * 构建AI系统提示
     */
    private fun buildSystemPrompt(): String {
        return """
你是一个专业的财务分析师，擅长分析个人账单数据并提供有价值的财务建议。

任务要求：
1. 分析用户提供的账单数据，特别关注大额交易（≥100元）
2. 生成结构化的财务总结报告
3. 提供实用的理财建议和消费优化建议
4. 识别异常消费模式和潜在的节省机会
5. 重点分析大额支出的合理性和必要性

报告结构要求：
1. 📊 收支概览 - 总收入、总支出、结余情况
2. 💰 大额交易分析 - 重点关注100元以上的收支项目
3. 📈 消费分析 - 主要消费分类、消费趋势分析
4. 🏪 商户分析 - 主要消费商户、消费频次分析
5. 💡 理财建议 - 基于数据的个性化建议，特别针对大额支出优化
6. ⚠️ 风险提醒 - 异常消费或需要注意的地方

输出要求：
- 使用中文回复
- 数据准确，分析客观
- 建议实用可行
- 语言简洁易懂
- 适当使用emoji增强可读性
- 使用HTML而不是Markdown进行输出
- HTML建议使用卡片的形式展示，要丰富多彩，需要适配夜间模式
        """.trimIndent()
    }
}