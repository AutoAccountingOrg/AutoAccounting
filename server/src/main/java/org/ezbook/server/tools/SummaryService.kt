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

package org.ezbook.server.tools

import org.ezbook.server.db.Db
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.db.model.BillSummaryModel
import org.ezbook.server.db.model.CategoryStatsModel
import org.ezbook.server.db.model.ShopStatsModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 账单摘要服务 - 服务端直接生成summary字符串
 * 使用SQL聚合，避免内存中处理大量数据
 */
object SummaryService {

    /**
     * 生成指定时间范围的账单摘要字符串
     *
     * @param startTime 开始时间戳（毫秒）
     * @param endTime 结束时间戳（毫秒）
     * @param periodName 周期名称
     * @return 格式化的摘要字符串
     */
    suspend fun generateSummary(startTime: Long, endTime: Long, periodName: String): String {
        val dao = Db.get().billInfoDao()

        // 使用SQL直接聚合基础数据
        val totalIncome = dao.getMonthlyIncome(startTime, endTime) ?: 0.0
        val totalExpense = dao.getMonthlyExpense(startTime, endTime) ?: 0.0
        val netIncome = totalIncome - totalExpense

        // 检查是否有数据
        if (totalIncome == 0.0 && totalExpense == 0.0) {
            return "该时间段暂无账单数据"
        }

        // 获取各类型数量
        val incomeCount = dao.getIncomeCount(startTime, endTime)
        val expenseCount = dao.getExpenseCount(startTime, endTime)
        val transferCount = dao.getTransferCount(startTime, endTime)

        // 获取完整统计数据（不能限制，保证统计准确性）
        val allCategoryStats = dao.getExpenseCategoryStats(startTime, endTime)
        val allShopStats = dao.getExpenseShopStats(startTime, endTime)

        // Service层决定显示多少统计项（统计数据通常不会太多）
        val categoryStats = allCategoryStats.take(10)
        val shopStats = allShopStats.take(10)

        // 获取样本数据（DAO层已限制数量，避免过多数据传输）
        val largeTransactions = dao.getLargeTransactions(startTime, endTime, 100.0, 20)
        val sampleBills = dao.getBillSamples(startTime, endTime, 40)

        return buildSummaryString(
            periodName,
            totalIncome, totalExpense,  // 基础金额
            incomeCount, expenseCount, transferCount,  // 数量统计
            categoryStats, shopStats,  // 分类统计
            largeTransactions, sampleBills  // 样本数据
        )
    }

    /**
     * 构建摘要字符串 - 减少不必要的参数传递
     */
    private fun buildSummaryString(
        periodName: String,
        totalIncome: Double, totalExpense: Double,  // 基础金额
        incomeCount: Int, expenseCount: Int, transferCount: Int,  // 数量统计
        categoryStats: List<CategoryStatsModel>, shopStats: List<ShopStatsModel>,  // 分类统计
        largeTransactions: List<BillSummaryModel>, sampleBills: List<BillSummaryModel>  // 样本数据
    ): String {
        // 内部计算净收入，不需要传参
        val netIncome = totalIncome - totalExpense
        // 格式化分类统计
        val categoryText = if (categoryStats.isNotEmpty()) {
            categoryStats.joinToString("\n") { stat ->
                "- ${stat.cateName}：¥${"%.2f".format(stat.amount)} (${stat.count} 笔)"
            }
        } else {
            "- 暂无数据"
        }

        // 格式化商户统计
        val shopText = if (shopStats.isNotEmpty()) {
            shopStats.joinToString("\n") { stat ->
                "- ${stat.shopName}：¥${"%.2f".format(stat.amount)} (${stat.count} 笔)"
            }
        } else {
            "- 暂无数据"
        }

        // 格式化大额交易
        val largeTransactionText = if (largeTransactions.isNotEmpty()) {
            """

💰 大额交易（≥100元，前20笔）：
${largeTransactions.joinToString("\n") { formatBillForAI(it) }}
"""
        } else {
            ""
        }

        return """
📊 账单总览：
- 总收入：¥${"%.2f".format(totalIncome)}
- 总支出：¥${"%.2f".format(totalExpense)}
- 净收入：¥${"%.2f".format(netIncome)}
- 收入笔数：$incomeCount
- 支出笔数：$expenseCount
- 转账笔数：$transferCount

📈 支出分类统计（前10）：
$categoryText

🏪 主要消费商户（前10）：
$shopText
$largeTransactionText
📋 详细账单数据：
${sampleBills.joinToString("\n") { formatBillForAI(it) }}
        """.trimIndent()
    }

    /**
     * 格式化账单数据供AI分析
     */
    private fun formatBillForAI(bill: BillSummaryModel): String {
        val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        val date = dateFormat.format(Date(bill.time))
        return "[$date] ${bill.type} ¥${"%.2f".format(bill.money)} ${bill.cateName} ${bill.shopName} ${bill.shopItem}"
    }
}