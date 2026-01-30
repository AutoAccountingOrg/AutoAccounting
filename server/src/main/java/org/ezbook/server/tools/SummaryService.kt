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

import org.ezbook.server.constant.BillType
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.db.model.BillSummaryModel
import org.ezbook.server.db.model.CategoryStatsModel
import org.ezbook.server.db.model.ShopStatsModel
import org.ezbook.server.db.model.TrendRowModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 账单摘要服务 - 服务端直接生成summary字符串
 * 使用SQL聚合，避免内存中处理大量数据
 */
object SummaryService {
    /**
     * 拿铁因子的小额阈值（单位：元）。
     */
    private const val SMALL_EXPENSE_MAX = 30.0

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

        // 计算周期天数与日均支出
        val periodDays = ((endTime - startTime) / (24L * 3600_000L)).coerceAtLeast(1)
        val avgExpensePerDay = totalExpense / periodDays

        // 检查是否有数据
        if (totalIncome == 0.0 && totalExpense == 0.0) {
            return "该时间段暂无账单数据"
        }

        // 获取各类型数量
        val incomeCount = dao.getIncomeCount(startTime, endTime)
        val expenseCount = dao.getExpenseCount(startTime, endTime)
        val transferCount = dao.getTransferCount(startTime, endTime)

        // 计算笔均金额（用于“单价 vs 频次”的归因分析）
        val avgExpensePerTxn = if (expenseCount > 0) totalExpense / expenseCount else 0.0
        val avgIncomePerTxn = if (incomeCount > 0) totalIncome / incomeCount else 0.0

        // 获取负债相关金额（用于负债侵蚀度与压力测试的可用数据）
        val repaymentExpense = dao.sumAmountByTypes(
            startTime,
            endTime,
            listOf(BillType.ExpendRepayment)
        ) ?: 0.0
        val lendingExpense = dao.sumAmountByTypes(
            startTime,
            endTime,
            listOf(BillType.ExpendLending)
        ) ?: 0.0
        val lendingIncome = dao.sumAmountByTypes(
            startTime,
            endTime,
            listOf(BillType.IncomeLending)
        ) ?: 0.0

        // 获取完整统计数据（不能限制，保证统计准确性）
        val allCategoryStats = dao.getExpenseCategoryStatsForAI(startTime, endTime)
        val allShopStats = dao.getExpenseShopStats(startTime, endTime)

        // 获取趋势数据（用于图表展示）
        val dailyTrend = dao.getSimpleDailyTrend(startTime, endTime)

        // 获取行为规律统计（用于频次/节律图表）
        val weekdayStats = dao.getExpenseWeekdayStats(startTime, endTime)
        val hourStats = dao.getExpenseHourStats(startTime, endTime)

        // 获取小额高频支出（拿铁因子候选）
        val smallExpenseStats = dao.getSmallExpenseCategoryStats(
            startTime,
            endTime,
            SMALL_EXPENSE_MAX
        )

        // Service层决定显示多少统计项（统计数据通常不会太多）
        val categoryStats = allCategoryStats.take(10)
        val shopStats = allShopStats.take(10)

        // 获取资产账户列表（仅名称与类型，用于资产构成提示）
        val assets = Db.get().assetsDao().load()

        // 获取样本数据（DAO层已限制数量，避免过多数据传输）
        val largeTransactions = dao.getLargeTransactions(startTime, endTime, 100.0, 20)
        val sampleBills = dao.getBillSamples(startTime, endTime, 40)

        // 计算上一周期的基础对比数据（用于消费归因）
        val previousSummary = buildPreviousSummary(dao, startTime, endTime)

        return buildSummaryString(
            periodName,
            startTime,
            endTime,
            totalIncome, totalExpense,  // 基础金额
            incomeCount, expenseCount, transferCount,  // 数量统计
            periodDays, avgExpensePerDay, avgExpensePerTxn, avgIncomePerTxn,  // 周期均值
            previousSummary,  // 对比周期
            repaymentExpense, lendingExpense, lendingIncome,  // 负债与借贷相关金额
            categoryStats, shopStats,  // 分类统计
            smallExpenseStats,  // 小额高频统计
            weekdayStats, hourStats,  // 行为规律统计
            dailyTrend,  // 趋势数据
            assets,  // 资产账户
            largeTransactions, sampleBills  // 样本数据
        )
    }

    /**
     * 构建上一周期的汇总数据（用于“单价 vs 频次”的归因比较）。
     */
    private suspend fun buildPreviousSummary(
        dao: org.ezbook.server.db.dao.BillInfoDao,
        startTime: Long,
        endTime: Long
    ): PreviousSummary {
        val duration = endTime - startTime
        if (duration <= 0L) {
            return PreviousSummary.empty()
        }

        val prevStart = startTime - duration
        val prevEnd = startTime
        val prevIncome = dao.getMonthlyIncome(prevStart, prevEnd) ?: 0.0
        val prevExpense = dao.getMonthlyExpense(prevStart, prevEnd) ?: 0.0
        val prevIncomeCount = dao.getIncomeCount(prevStart, prevEnd)
        val prevExpenseCount = dao.getExpenseCount(prevStart, prevEnd)
        val prevDays = (duration / (24L * 3600_000L)).coerceAtLeast(1)
        val prevAvgExpensePerDay = prevExpense / prevDays
        val prevAvgExpensePerTxn = if (prevExpenseCount > 0) {
            prevExpense / prevExpenseCount
        } else {
            0.0
        }

        return PreviousSummary(
            incomeTotal = prevIncome,
            expenseTotal = prevExpense,
            incomeCount = prevIncomeCount,
            expenseCount = prevExpenseCount,
            avgExpensePerDay = prevAvgExpensePerDay,
            avgExpensePerTxn = prevAvgExpensePerTxn
        )
    }

    /**
     * 上一周期汇总数据结构。
     */
    private data class PreviousSummary(
        val incomeTotal: Double,
        val expenseTotal: Double,
        val incomeCount: Int,
        val expenseCount: Int,
        val avgExpensePerDay: Double,
        val avgExpensePerTxn: Double
    ) {
        companion object {
            /**
             * 空对象，避免空值分支。
             */
            fun empty(): PreviousSummary = PreviousSummary(0.0, 0.0, 0, 0, 0.0, 0.0)
        }
    }

    /**
     * 构建摘要字符串 - 减少不必要的参数传递
     */
    private fun buildSummaryString(
        periodName: String,
        startTime: Long,
        endTime: Long,
        totalIncome: Double, totalExpense: Double,  // 基础金额
        incomeCount: Int, expenseCount: Int, transferCount: Int,  // 数量统计
        periodDays: Long,
        avgExpensePerDay: Double,
        avgExpensePerTxn: Double,
        avgIncomePerTxn: Double,  // 周期均值
        previousSummary: PreviousSummary,  // 对比周期
        repaymentExpense: Double,
        lendingExpense: Double,
        lendingIncome: Double,  // 负债与借贷相关金额
        categoryStats: List<CategoryStatsModel>, shopStats: List<ShopStatsModel>,  // 分类统计
        smallExpenseStats: List<CategoryStatsModel>,  // 小额高频统计
        weekdayStats: List<org.ezbook.server.db.model.TimeBucketStatsModel>,  // 周内规律
        hourStats: List<org.ezbook.server.db.model.TimeBucketStatsModel>,  // 日内规律
        dailyTrend: List<TrendRowModel>,  // 趋势数据
        assets: List<org.ezbook.server.db.model.AssetsModel>,  // 资产账户
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

        // 格式化小额高频支出（拿铁因子候选）
        val smallExpenseText = if (smallExpenseStats.isNotEmpty()) {
            smallExpenseStats.take(10).joinToString("\n") { stat ->
                "- ${stat.cateName}：¥${"%.2f".format(stat.amount)} (${stat.count} 笔)"
            }
        } else {
            "- 暂无数据"
        }

        // 格式化周内行为分布
        val weekdayText = if (weekdayStats.isNotEmpty()) {
            weekdayStats.joinToString("\n") { stat ->
                "- 周${stat.bucket}：¥${"%.2f".format(stat.amount)} (${stat.count} 笔)"
            }
        } else {
            "- 暂无数据"
        }

        // 格式化日内行为分布
        val hourText = if (hourStats.isNotEmpty()) {
            hourStats.joinToString("\n") { stat ->
                "- ${stat.bucket}点：¥${"%.2f".format(stat.amount)} (${stat.count} 笔)"
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

        // 格式化趋势数据（用于图表展示）
        val trendText = if (dailyTrend.isNotEmpty()) {
            dailyTrend.joinToString("\n") { row ->
                "- ${row.day}：收入=${formatNumber(row.income)}，支出=${formatNumber(row.expense)}"
            }
        } else {
            "- 暂无数据"
        }

        // 格式化资产账户（仅输出名称与类型）
        val assetsText = if (assets.isNotEmpty()) {
            assets.joinToString("\n") { asset ->
                "- ${asset.name} (${asset.type})"
            }
        } else {
            "- 暂无数据"
        }

        // 构建供AI使用的结构化数据（JSON样式，便于提取用于图表）
        val chartDataJson = buildChartDataJson(
            periodName,
            startTime,
            endTime,
            totalIncome,
            totalExpense,
            netIncome,
            incomeCount,
            expenseCount,
            transferCount,
            periodDays,
            avgExpensePerDay,
            avgExpensePerTxn,
            avgIncomePerTxn,
            previousSummary,
            repaymentExpense,
            lendingExpense,
            lendingIncome,
            categoryStats,
            shopStats,
            smallExpenseStats,
            weekdayStats,
            hourStats,
            dailyTrend,
            assets,
            largeTransactions,
            sampleBills
        )

        return """
📊 账单总览：
- 总收入：¥${"%.2f".format(totalIncome)}
- 总支出：¥${"%.2f".format(totalExpense)}
- 净收入：¥${"%.2f".format(netIncome)}
- 收入笔数：$incomeCount
- 支出笔数：$expenseCount
- 转账笔数：$transferCount
- 周期天数：$periodDays
- 日均支出：¥${"%.2f".format(avgExpensePerDay)}
- 笔均支出：¥${"%.2f".format(avgExpensePerTxn)}
- 笔均收入：¥${"%.2f".format(avgIncomePerTxn)}

📉 上一周期对比：
- 上期总收入：¥${"%.2f".format(previousSummary.incomeTotal)}
- 上期总支出：¥${"%.2f".format(previousSummary.expenseTotal)}
- 上期收入笔数：${previousSummary.incomeCount}
- 上期支出笔数：${previousSummary.expenseCount}
- 上期日均支出：¥${"%.2f".format(previousSummary.avgExpensePerDay)}
- 上期笔均支出：¥${"%.2f".format(previousSummary.avgExpensePerTxn)}
- 还款支出：¥${"%.2f".format(repaymentExpense)}
- 借出支出：¥${"%.2f".format(lendingExpense)}
- 借入收入：¥${"%.2f".format(lendingIncome)}

📈 支出分类统计（前10）：
$categoryText

🏪 主要消费商户（前10）：
$shopText

☕ 拿铁因子（小额高频支出，≤${SMALL_EXPENSE_MAX}元，前10）：
$smallExpenseText

🧭 行为规律（按星期分布）：
$weekdayText

⏰ 行为规律（按小时分布）：
$hourText

📅 收支趋势（按日）：
$trendText

💼 资产账户列表（名称与类型）：
$assetsText
$largeTransactionText
📋 详细账单数据：
${sampleBills.joinToString("\n") { formatBillForAI(it) }}

🧾 结构化数据（JSON，用于图表与AI精算）：
$chartDataJson
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

    /**
     * 构建AI可直接消费的结构化数据（JSON样式）。
     * 保持字段稳定，避免提示词解析时出现歧义。
     */
    private fun buildChartDataJson(
        periodName: String,
        startTime: Long,
        endTime: Long,
        totalIncome: Double,
        totalExpense: Double,
        netIncome: Double,
        incomeCount: Int,
        expenseCount: Int,
        transferCount: Int,
        periodDays: Long,
        avgExpensePerDay: Double,
        avgExpensePerTxn: Double,
        avgIncomePerTxn: Double,
        previousSummary: PreviousSummary,
        repaymentExpense: Double,
        lendingExpense: Double,
        lendingIncome: Double,
        categoryStats: List<CategoryStatsModel>,
        shopStats: List<ShopStatsModel>,
        smallExpenseStats: List<CategoryStatsModel>,
        weekdayStats: List<org.ezbook.server.db.model.TimeBucketStatsModel>,
        hourStats: List<org.ezbook.server.db.model.TimeBucketStatsModel>,
        dailyTrend: List<TrendRowModel>,
        assets: List<org.ezbook.server.db.model.AssetsModel>,
        largeTransactions: List<BillSummaryModel>,
        sampleBills: List<BillSummaryModel>
    ): String {
        val categoriesJson = categoryStats.joinToString(",") { stat ->
            """{"name":"${stat.cateName}","amount":${formatNumber(stat.amount)},"count":${stat.count}}"""
        }
        val shopsJson = shopStats.joinToString(",") { stat ->
            """{"name":"${stat.shopName}","amount":${formatNumber(stat.amount)},"count":${stat.count}}"""
        }
        val trendJson = dailyTrend.joinToString(",") { row ->
            """{"day":"${row.day}","income":${formatNumber(row.income)},"expense":${formatNumber(row.expense)}}"""
        }
        val assetsJson = assets.joinToString(",") { asset ->
            """{"name":"${asset.name}","type":"${asset.type}"}"""
        }
        val smallExpenseJson = smallExpenseStats.joinToString(",") { stat ->
            """{"name":"${stat.cateName}","amount":${formatNumber(stat.amount)},"count":${stat.count}}"""
        }
        val weekdayJson = weekdayStats.joinToString(",") { stat ->
            """{"weekday":"${stat.bucket}","amount":${formatNumber(stat.amount)},"count":${stat.count}}"""
        }
        val hourJson = hourStats.joinToString(",") { stat ->
            """{"hour":"${stat.bucket}","amount":${formatNumber(stat.amount)},"count":${stat.count}}"""
        }
        val largeJson = largeTransactions.joinToString(",") { bill ->
            """{"time":${bill.time},"type":"${bill.type}","money":${formatNumber(bill.money)},"cate":"${bill.cateName}","shop":"${bill.shopName}","item":"${bill.shopItem}"}"""
        }
        val samplesJson = sampleBills.joinToString(",") { bill ->
            """{"time":${bill.time},"type":"${bill.type}","money":${formatNumber(bill.money)},"cate":"${bill.cateName}","shop":"${bill.shopName}","item":"${bill.shopItem}"}"""
        }

        return """
{
  "period":"$periodName",
  "startTime":$startTime,
  "endTime":$endTime,
  "summary":{
    "incomeTotal":${formatNumber(totalIncome)},
    "expenseTotal":${formatNumber(totalExpense)},
    "netTotal":${formatNumber(netIncome)},
    "incomeCount":$incomeCount,
    "expenseCount":$expenseCount,
    "transferCount":$transferCount,
    "periodDays":$periodDays,
    "avgExpensePerDay":${formatNumber(avgExpensePerDay)},
    "avgExpensePerTxn":${formatNumber(avgExpensePerTxn)},
    "avgIncomePerTxn":${formatNumber(avgIncomePerTxn)}
  },
  "previousSummary":{
    "incomeTotal":${formatNumber(previousSummary.incomeTotal)},
    "expenseTotal":${formatNumber(previousSummary.expenseTotal)},
    "incomeCount":${previousSummary.incomeCount},
    "expenseCount":${previousSummary.expenseCount},
    "avgExpensePerDay":${formatNumber(previousSummary.avgExpensePerDay)},
    "avgExpensePerTxn":${formatNumber(previousSummary.avgExpensePerTxn)}
  },
  "debt":{
    "repaymentExpense":${formatNumber(repaymentExpense)},
    "lendingExpense":${formatNumber(lendingExpense)},
    "lendingIncome":${formatNumber(lendingIncome)}
  },
  "categories":[$categoriesJson],
  "shops":[$shopsJson],
  "smallExpense":{
    "maxAmount":${formatNumber(SMALL_EXPENSE_MAX)},
    "items":[$smallExpenseJson]
  },
  "weekdayStats":[$weekdayJson],
  "hourStats":[$hourJson],
  "dailyTrend":[$trendJson],
  "assets":[$assetsJson],
  "largeTransactions":[$largeJson],
  "samples":[$samplesJson]
}
        """.trimIndent()
    }

    /**
     * 数值格式化（用于JSON数值输出，统一保留两位小数）。
     */
    private fun formatNumber(value: Double?): String {
        val safeValue = value ?: 0.0
        return String.format(Locale.US, "%.2f", safeValue)
    }
}