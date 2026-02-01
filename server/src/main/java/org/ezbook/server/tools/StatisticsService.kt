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

package org.ezbook.server.tools

import org.ezbook.server.db.Db
import org.ezbook.server.db.model.CategoryStatsModel
import org.ezbook.server.db.model.removeParent
import java.util.Locale
import kotlin.math.abs

/**
 * 统计服务：根据时间范围智能选择聚合粒度，只统计基础收入支出。
 */
object StatisticsService {

    private const val DAYS_IN_MONTH = 31L
    private const val DAYS_IN_YEAR = 365L
    /**
     * 为WebView生成完整的消费分析数据
     * 返回Map格式，供summary.html使用
     */
    suspend fun buildSummaryForWebView(
        startTime: Long,
        endTime: Long,
        period: String
    ): Map<String, Any?> {
        val dao = Db.get().billInfoDao()
        val periodDays = ((endTime - startTime) / (24L * 3600_000L)).coerceAtLeast(1)

        // 查询本期数据
        val totalIncome = dao.getMonthlyIncome(startTime, endTime) ?: 0.0
        val totalExpense = dao.getMonthlyExpense(startTime, endTime) ?: 0.0
        val netIncome = totalIncome - totalExpense

        // 查询上期数据（用于同比）
        val duration = endTime - startTime
        val prevStart = startTime - duration
        val prevIncome = dao.getMonthlyIncome(prevStart, startTime) ?: 0.0
        val prevExpense = dao.getMonthlyExpense(prevStart, startTime) ?: 0.0

        // 查询分类统计（查询后删掉父类，仅保留子类；无子类则保留原分类）
        val expenseCategoryRows = dao.getExpenseCategoryStats(startTime, endTime)
        val incomeCategoryRows = dao.getIncomeCategoryStats(startTime, endTime)
        // 查询完成后删除父类（若有子类则只保留子类，否则保留原分类）
        val expenseCategories = expenseCategoryRows
            .map { it.copy(cateName = it.cateName.removeParent()) }
        val incomeCategories = incomeCategoryRows
            .map { it.copy(cateName = it.cateName.removeParent()) }

        // 查询商户统计
        val topMerchants = dao.getExpenseShopStats(startTime, endTime).take(20)

        // 交易总数与日均交易数
        val totalTransactions = dao.getBillsCountByTimeRange(startTime, endTime)
        val dailyTransactions =
            if (periodDays > 0) totalTransactions / periodDays.toDouble() else 0.0

        // 同比变化百分比（字符串形式，便于前端直接显示）
        val incomeChange = formatChange(totalIncome, prevIncome)
        val expenseChange = formatChange(totalExpense, prevExpense)

        // 日均统计（次数）
        val totalExpenseCount = expenseCategories.sumOf { it.count }
        val totalIncomeCount = incomeCategories.sumOf { it.count }
        val dailyExpenseFreq =
            if (periodDays > 0) totalExpenseCount / periodDays.toDouble() else 0.0
        val dailyIncomeFreq = if (periodDays > 0) totalIncomeCount / periodDays.toDouble() else 0.0

        // 收支趋势（服务端预聚合）
        val trendData = buildTrendData(startTime, endTime, periodDays)

        // 24小时消费分布（服务端预聚合）
        val hourStats = buildHourStats(startTime, endTime)

        // 消费时间洞察（服务端预计算）
        val timeInsight = buildTimeInsight(hourStats)

        // 消费生物钟热力图（服务端预计算）
        val heatmap = buildHeatmapData(startTime, endTime)

        // 金额分布（服务端预计算）
        val amountDistribution = buildAmountDistribution(startTime, endTime)

        // 消费象限（服务端预计算）
        val scatter = buildScatterData(expenseCategories)

        // 消费热词云（服务端预计算）
        val wordCloud = buildWordCloudData(expenseCategories)

        // 分类洞察（服务端预计算）
        val categoryInsight =
            buildCategoryInsight(expenseCategories, incomeCategories, totalExpense, totalIncome)

        // 消费洞察（服务端预计算）
        val insights = buildInsightsData(
            startTime,
            endTime,
            expenseCategories,
            totalExpense,
            netIncome,
            totalIncome,
            periodDays
        )

        // 构建数据结构（与summary.html的defaultData格式一致）
        return mapOf(
            "period" to period,
            "periodDays" to periodDays,
            "basicStats" to mapOf(
                "totalIncome" to totalIncome,
                "totalExpense" to totalExpense,
                "hasIncome" to (totalIncome > 0),
                "avgIncomePerDay" to (totalIncome / periodDays),
                "avgExpensePerDay" to (totalExpense / periodDays),
                "savingsRate" to if (totalIncome > 0) ((totalIncome - totalExpense) / totalIncome * 100) else 0.0,
                "expenseIncomeRatio" to if (totalIncome > 0) (totalExpense / totalIncome * 100) else 0.0
            ),
            "expenseByCategory" to expenseCategories.map {
                mapOf(
                    "category" to it.cateName,
                    "amount" to it.amount,
                    "percentage" to if (totalExpense > 0) (it.amount / totalExpense * 100) else 0.0,
                    "count" to it.count,
                    "avgPerDay" to (it.amount / periodDays)
                )
            },
            "incomeByCategory" to incomeCategories.map {
                mapOf(
                    "category" to it.cateName,
                    "amount" to it.amount,
                    "percentage" to if (totalIncome > 0) (it.amount / totalIncome * 100) else 0.0,
                    "count" to it.count,
                    "avgPerDay" to (it.amount / periodDays)
                )
            },
            "historicalData" to mapOf(
                "lastPeriod" to mapOf(
                    "totalIncome" to prevIncome,
                    "totalExpense" to prevExpense,
                    "savingsRate" to if (prevIncome > 0) ((prevIncome - prevExpense) / prevIncome * 100) else 0.0,
                    "expenseIncomeRatio" to if (prevIncome > 0) (prevExpense / prevIncome * 100) else 0.0
                ),
                "categoryHistory" to emptyList<Any>()
            ),
            "merchantAnalysis" to mapOf(
                "topByAmount" to topMerchants.map {
                    mapOf(
                        "merchant" to it.shopName,
                        "amount" to it.amount,
                        "count" to it.count
                    )
                },
                "topByFrequency" to topMerchants.sortedByDescending { it.count }.map {
                    mapOf(
                        "merchant" to it.shopName,
                        "amount" to it.amount,
                        "count" to it.count
                    )
                }
            ),
            // 账单明细不再下发，避免大数据量占用内存
            "bills" to emptyList<Any>(),
            "overview" to mapOf(
                "totalTransactions" to totalTransactions,
                "netIncome" to netIncome,
                "savingsRate" to if (totalIncome > 0) ((netIncome / totalIncome) * 100) else 0.0,
                "incomeChange" to incomeChange,
                "expenseChange" to expenseChange,
                "dailyTransactions" to dailyTransactions
            ),
            "dailyStats" to mapOf(
                "totalExpenseCount" to totalExpenseCount,
                "totalIncomeCount" to totalIncomeCount,
                "dailyExpenseFreq" to dailyExpenseFreq,
                "dailyIncomeFreq" to dailyIncomeFreq
            ),
            "trend" to trendData,
            "hourStats" to hourStats,
            "timeInsight" to timeInsight,
            "heatmap" to heatmap,
            "amountDistribution" to amountDistribution,
            "scatter" to scatter,
            "wordCloud" to wordCloud,
            "categoryInsight" to categoryInsight,
            "insights" to insights
        )
    }

    /**
     * 计算同比变化百分比文本
     */
    private fun formatChange(current: Double, last: Double): String {
        if (last == 0.0) return if (current > 0) "+100%" else "0%"
        val change = ((current - last) / last * 100)
        return String.format(Locale.US, "%+.1f%%", change)
    }

    /**
     * 构建趋势数据（按日/按月/按年）
     */
    private suspend fun buildTrendData(
        startTime: Long,
        endTime: Long,
        periodDays: Long
    ): Map<String, Any?> {
        val dao = Db.get().billInfoDao()

        return when {
            periodDays > DAYS_IN_YEAR -> {
                val monthly = dao.getSimpleMonthlyTrend(startTime, endTime)
                val yearMap = mutableMapOf<String, Pair<Double, Double>>()
                monthly.forEach { row ->
                    val year = row.day.take(4)
                    val current = yearMap[year] ?: (0.0 to 0.0)
                    yearMap[year] = current.first + row.income!! to current.second + row.expense!!
                }
                val labels = yearMap.keys.sorted()
                val incomes = labels.map { yearMap[it]?.first ?: 0.0 }
                val expenses = labels.map { yearMap[it]?.second ?: 0.0 }
                mapOf("labels" to labels, "incomes" to incomes, "expenses" to expenses)
            }

            periodDays > DAYS_IN_MONTH -> {
                val monthly = dao.getSimpleMonthlyTrend(startTime, endTime)
                val labels = monthly.map { it.day }
                val incomes = monthly.map { it.income }
                val expenses = monthly.map { it.expense }
                mapOf("labels" to labels, "incomes" to incomes, "expenses" to expenses)
            }

            else -> {
                val daily = dao.getSimpleDailyTrend(startTime, endTime)
                val labels = daily.map { it.day }
                val incomes = daily.map { it.income }
                val expenses = daily.map { it.expense }
                mapOf("labels" to labels, "incomes" to incomes, "expenses" to expenses)
            }
        }
    }

    /**
     * 构建24小时消费统计数据
     */
    private suspend fun buildHourStats(startTime: Long, endTime: Long): Map<String, Any?> {
        val dao = Db.get().billInfoDao()
        val hourStats = dao.getExpenseHourStats(startTime, endTime)
        val hourMap = hourStats.associateBy { it.bucket }

        val hours = (0..23).map { String.format(Locale.US, "%02d", it) }
        val amounts = (0..23).map { hour ->
            val key = String.format(Locale.US, "%02d", hour)
            hourMap[key]?.amount ?: 0.0
        }
        val counts = (0..23).map { hour ->
            val key = String.format(Locale.US, "%02d", hour)
            hourMap[key]?.count ?: 0
        }

        return mapOf("hours" to hours, "amounts" to amounts, "counts" to counts)
    }

    /**
     * 构建消费时间洞察数据
     */
    private fun buildTimeInsight(hourStats: Map<String, Any?>): Map<String, Any?> {
        val hours = hourStats["hours"] as? List<String> ?: emptyList()
        val amounts = hourStats["amounts"] as? List<Double> ?: emptyList()
        val counts = hourStats["counts"] as? List<Int> ?: emptyList()

        var peakIndex = 0
        var peakAmount = 0.0
        amounts.forEachIndexed { index, amount ->
            if (amount > peakAmount) {
                peakAmount = amount
                peakIndex = index
            }
        }

        val nightAmount = amounts.filterIndexed { index, _ -> index !in 6..<22 }.sum()
        val dayAmount = amounts.filterIndexed { index, _ -> index in 6..21 }.sum()
        val total = nightAmount + dayAmount
        val nightRatio = if (total > 0) (nightAmount / total * 100) else 0.0

        val (patternText, patternTag) = when {
            nightRatio > 30 -> "夜猫子型" to "tag-danger"
            nightRatio > 20 -> "深夜消费较多" to "tag-warning"
            else -> "消费较规律" to "tag-success"
        }

        return mapOf(
            "peakHour" to (hours.getOrNull(peakIndex) ?: "00:00"),
            "peakAmount" to peakAmount,
            "peakCount" to (counts.getOrNull(peakIndex) ?: 0),
            "nightRatio" to nightRatio,
            "patternText" to patternText,
            "patternTag" to patternTag
        )
    }

    /**
     * 构建消费生物钟热力图数据（2小时为一个桶）
     */
    private suspend fun buildHeatmapData(startTime: Long, endTime: Long): Map<String, Any?> {
        val dao = Db.get().billInfoDao()
        val weekDays = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
        val hours = (0..11).map { index ->
            val startHour = index * 2
            String.format(Locale.US, "%02d:00", startHour)
        }
        val matrix = Array(7) { DoubleArray(12) }
        val rows = dao.getExpenseHeatmapStats(startTime, endTime)

        rows.forEach { row ->
            val dayIndex = row.bucketDay.toIntOrNull() ?: -1
            val hourIndex = row.bucketHour.toIntOrNull() ?: -1
            val bucketIndex = hourIndex / 2
            if (dayIndex in 0..6 && bucketIndex in 0..11) {
                matrix[dayIndex][bucketIndex] += row.amount
            }
        }

        val values = mutableListOf<List<Any>>()
        var maxValue = 0.0
        for (day in 0..6) {
            for (hour in 0..11) {
                val amount = matrix[day][hour]
                if (amount > maxValue) {
                    maxValue = amount
                }
                values.add(listOf(hour, day, amount))
            }
        }

        return mapOf(
            "weekDays" to weekDays,
            "hours" to hours,
            "values" to values,
            "maxValue" to maxValue
        )
    }

    /**
     * 构建金额分布统计数据
     */
    private suspend fun buildAmountDistribution(startTime: Long, endTime: Long): Map<String, Any?> {
        val dao = Db.get().billInfoDao()
        val buckets = listOf("0-50", "50-100", "100-200", "200-500", "500-1000", "1000+")
        val rows = dao.getExpenseAmountDistribution(startTime, endTime)
        val countMap = rows.associate { it.bucket to it.count }

        return mapOf(
            "labels" to buckets,
            "counts" to buckets.map { bucket -> countMap[bucket] ?: 0 }
        )
    }

    /**
     * 构建消费象限数据
     */
    private fun buildScatterData(expenseCategories: List<CategoryStatsModel>): Map<String, Any?> {
        if (expenseCategories.isEmpty()) {
            return mapOf("points" to emptyList<Any>())
        }

        val freqList = expenseCategories.map { it.count }.sorted()
        val avgList = expenseCategories.map { it.amount / it.count }.sorted()
        val medianFreq = freqList[freqList.size / 2]
        val medianAvg = avgList[avgList.size / 2]

        val points = expenseCategories.map { cat ->
            val avgPrice = if (cat.count > 0) (cat.amount / cat.count) else 0.0
            val color = when {
                cat.count > medianFreq && avgPrice > medianAvg -> "#ef4444"
                cat.count > medianFreq && avgPrice <= medianAvg -> "#f59e0b"
                cat.count <= medianFreq && avgPrice > medianAvg -> "#8b5cf6"
                else -> "#10b981"
            }
            val size = kotlin.math.sqrt(cat.count * avgPrice) / 10 + 10
            mapOf(
                "name" to cat.cateName,
                "freq" to cat.count,
                "avgPrice" to avgPrice,
                "amount" to cat.amount,
                "color" to color,
                "size" to size
            )
        }

        return mapOf(
            "medianFreq" to medianFreq,
            "medianAvg" to medianAvg,
            "points" to points
        )
    }

    /**
     * 构建消费热词云数据
     */
    private fun buildWordCloudData(
        expenseCategories: List<CategoryStatsModel>
    ): Map<String, Any?> {
        val colors =
            listOf("#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#ec4899", "#06b6d4")
        val words = mutableListOf<Map<String, Any>>()


        expenseCategories.forEach { cat ->
            words.add(
                mapOf(
                    "name" to cat.cateName,
                    "value" to (cat.amount * 1.5),
                    "color" to colors[abs(cat.cateName.hashCode()) % colors.size]
                )
            )
        }

        return mapOf("words" to words)
    }

    /**
     * 构建分类洞察数据
     */
    private fun buildCategoryInsight(
        expenseCategories: List<CategoryStatsModel>,
        incomeCategories: List<CategoryStatsModel>,
        totalExpense: Double,
        totalIncome: Double
    ): Map<String, Any?> {
        val expenseTop = expenseCategories.firstOrNull()
        val incomeTop = incomeCategories.firstOrNull()

        return mapOf(
            "expense" to mapOf(
                "category" to (expenseTop?.cateName ?: ""),
                "amount" to (expenseTop?.amount ?: 0.0),
                "percentage" to if (totalExpense > 0) ((expenseTop?.amount
                    ?: 0.0) / totalExpense * 100) else 0.0,
                "count" to (expenseTop?.count ?: 0),
                "avgPerTransaction" to if ((expenseTop?.count
                        ?: 0) > 0
                ) (expenseTop!!.amount / expenseTop.count) else 0.0
            ),
            "income" to mapOf(
                "category" to (incomeTop?.cateName ?: ""),
                "amount" to (incomeTop?.amount ?: 0.0),
                "percentage" to if (totalIncome > 0) ((incomeTop?.amount
                    ?: 0.0) / totalIncome * 100) else 0.0,
                "count" to (incomeTop?.count ?: 0),
                "avgPerTransaction" to if ((incomeTop?.count
                        ?: 0) > 0
                ) (incomeTop!!.amount / incomeTop.count) else 0.0
            )
        )
    }

    /**
     * 构建消费洞察数据
     */
    private suspend fun buildInsightsData(
        startTime: Long,
        endTime: Long,
        expenseCategories: List<CategoryStatsModel>,
        totalExpense: Double,
        netIncome: Double,
        totalIncome: Double,
        periodDays: Long
    ): Map<String, Any?> {
        val dao = Db.get().billInfoDao()
        val topCategory = expenseCategories.firstOrNull()
        val totalExpenseCount = expenseCategories.sumOf { it.count }
        val avgExpensePerTransaction =
            if (totalExpenseCount > 0) totalExpense / totalExpenseCount else 0.0

        // 拿铁因子（小额消费）
        val smallStats = dao.getSmallExpenseStats(startTime, endTime, 100.0)
        val latteAmount = smallStats.amount
        val latteCount = smallStats.count

        // 大额消费
        val largeStats = dao.getLargeExpenseStats(startTime, endTime, 1000.0)
        val largeAmount = largeStats.amount
        val largeRatio = if (totalExpense > 0) (largeAmount / totalExpense * 100) else 0.0

        // 周末消费
        val weekendStats = dao.getWeekendExpenseSum(startTime, endTime)
        val weekendAmount = weekendStats.amount
        val weekendRatio = if (totalExpense > 0) (weekendAmount / totalExpense * 100) else 0.0

        // 消费画像与标签
        val profile = mapOf(
            "text" to resolveProfileText(topCategory?.cateName),
            "category" to (topCategory?.cateName ?: "理性消费"),
            "avgExpense" to avgExpensePerTransaction
        )
        val tags = buildInsightTags(netIncome, totalIncome, totalExpenseCount, periodDays)

        return mapOf(
            "profile" to profile,
            "tags" to tags,
            "latteFactor" to mapOf(
                "amount" to latteAmount,
                "count" to latteCount,
                "coffee" to if (latteAmount > 0) (latteAmount / 30) else 0.0,
                "suggestion" to ""
            ),
            "largeExpense" to mapOf(
                "amount" to largeAmount,
                "count" to largeStats.count,
                "ratio" to largeRatio,
                "suggestion" to ""
            ),
            "weekend" to mapOf(
                "amount" to weekendAmount,
                "ratio" to weekendRatio,
                "suggestion" to ""
            )
        )
    }

    /**
     * 构建洞察标签
     */
    private fun buildInsightTags(
        netIncome: Double,
        totalIncome: Double,
        totalExpenseCount: Int,
        periodDays: Long
    ): List<Map<String, String>> {
        val tags = mutableListOf<Map<String, String>>()
        val savingsRate = if (totalIncome > 0) (netIncome / totalIncome * 100) else 0.0
        val avgFreq = if (periodDays > 0) totalExpenseCount / periodDays.toDouble() else 0.0

        when {
            savingsRate > 30 -> tags.add(mapOf("text" to "储蓄达人", "class" to "tag-success"))
            savingsRate < 10 -> tags.add(mapOf("text" to "月光族", "class" to "tag-danger"))
        }

        when {
            avgFreq > 5 -> tags.add(mapOf("text" to "高频消费", "class" to "tag-warning"))
            avgFreq < 2 -> tags.add(mapOf("text" to "理性消费", "class" to "tag-success"))
        }

        return tags
    }

    /**
     * 根据消费分类判断画像类型
     */
    private fun resolveProfileText(category: String?): String {
        val name = category?.trim().orEmpty()
        if (name.isEmpty()) return "理性消费者"

        // 正则匹配：覆盖常见命名差异
        val regexMatch = listOf(
            Regex("美食|餐|外卖") to "美食爱好者",
            Regex("交通|出行") to "出行达人",
            Regex("购物") to "购物狂热者",
            Regex("鞋子|衣服|服饰|穿搭") to "潮流穿搭派",
            Regex("住房|房租|水电") to "安居乐业型",
            Regex("娱乐|电影") to "文艺青年",
            Regex("游戏") to "游戏玩家",
            Regex("医疗|健康") to "健康关注者",
            Regex("教育|学习") to "终身学习者",
            Regex("数码|电子") to "数码达人",
            Regex("旅行|酒店") to "旅行达人",
            Regex("亲子|母婴") to "暖心父母",
            Regex("健身|运动") to "活力健身派",
            Regex("宠物") to "铲屎官"
        )
        regexMatch.firstOrNull { (regex, _) -> regex.containsMatchIn(name) }
            ?.let { return it.second }

        return "理性消费者"
    }

}


