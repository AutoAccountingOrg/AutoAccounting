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
import org.ezbook.server.models.CategoryItemDto
import org.ezbook.server.models.StatsResponse
import org.ezbook.server.models.SummaryDto
import org.ezbook.server.models.TrendDto
import java.util.Calendar
import java.util.Locale

/**
 * 统计服务：根据时间范围智能选择聚合粒度，只统计基础收入支出。
 */
object StatisticsService {

    private const val DAYS_IN_WEEK = 7L
    private const val DAYS_IN_MONTH = 31L
    private const val DAYS_IN_YEAR = 365L

    /**
     * 生成指定时间范围的统计数据。
     * 时间范围 ≤ 月：按天聚合；时间范围 ≤ 年：按月聚合
     */
    suspend fun buildStats(startTime: Long, endTime: Long): StatsResponse {
        val dao = Db.get().billInfoDao()
        val daySpan = (endTime - startTime) / (24 * 3600 * 1000L)

        val summary = buildSummary(dao, startTime, endTime)
        val trend = if (daySpan <= DAYS_IN_MONTH) {
            buildDailyTrend(dao, startTime, endTime)
        } else {
            buildMonthlyTrend(dao, startTime, endTime)
        }
        val expenseCategories = buildExpenseCategories(dao, startTime, endTime)
        val incomeCategories = buildIncomeCategories(dao, startTime, endTime)

        return StatsResponse(
            summary = summary,
            trend = trend,
            expenseCategories = expenseCategories,
            incomeCategories = incomeCategories
        )
    }

    /**
     * 汇总统计：只统计Income和Expend，使用money字段
     */
    private suspend fun buildSummary(
        dao: org.ezbook.server.db.dao.BillInfoDao,
        startTime: Long,
        endTime: Long
    ): SummaryDto {
        val totalIncome = dao.getMonthlyIncome(startTime, endTime) ?: 0.0
        val totalExpense = dao.getMonthlyExpense(startTime, endTime) ?: 0.0
        val days = ((endTime - startTime) / (24L * 3600_000L)).coerceAtLeast(1)
        val dailyAvgExpense = totalExpense / days

        return SummaryDto(
            totalExpense = totalExpense,
            totalIncome = totalIncome,
            net = totalIncome - totalExpense,
            dailyAvgExpense = dailyAvgExpense
        )
    }

    /**
     * 按天趋势统计
     */
    private suspend fun buildDailyTrend(
        dao: org.ezbook.server.db.dao.BillInfoDao,
        startTime: Long,
        endTime: Long
    ): TrendDto {
        val rows = dao.getSimpleDailyTrend(startTime, endTime)
        return fillTrendGaps(rows, startTime, endTime, true)
    }

    /**
     * 按月趋势统计
     */
    private suspend fun buildMonthlyTrend(
        dao: org.ezbook.server.db.dao.BillInfoDao,
        startTime: Long,
        endTime: Long
    ): TrendDto {
        val rows = dao.getSimpleMonthlyTrend(startTime, endTime)
        return fillTrendGaps(rows, startTime, endTime, false)
    }

    /**
     * 支出分类统计：按子类优先统计，包含图标和计数
     */
    private suspend fun buildExpenseCategories(
        dao: org.ezbook.server.db.dao.BillInfoDao,
        startTime: Long,
        endTime: Long
    ): List<CategoryItemDto> {
        val rows = dao.getExpenseCategoryStats(startTime, endTime)
        return buildCategoryStats(rows)
    }

    /**
     * 收入分类统计：按子类优先统计，包含图标和计数
     */
    private suspend fun buildIncomeCategories(
        dao: org.ezbook.server.db.dao.BillInfoDao,
        startTime: Long,
        endTime: Long
    ): List<CategoryItemDto> {
        val rows = dao.getIncomeCategoryStats(startTime, endTime)
        return buildCategoryStats(rows)
    }

    /**
     * 构建分类统计数据：优先按子类统计，获取图标
     */
    private suspend fun buildCategoryStats(rows: List<org.ezbook.server.db.model.CategoryAggregateRow>): List<CategoryItemDto> {
        if (rows.isEmpty()) return emptyList()

        val categoryDao = Db.get().categoryDao()
        val total = rows.sumOf { it.amount ?: 0.0 }.coerceAtLeast(1e-6)

        // 按父类分组，但优先使用子类数据
        val grouped = rows.groupBy { it.parent.ifEmpty { "其他" } }

        return grouped.map { (parent, list) ->
            val parentAmount = list.sumOf { it.amount ?: 0.0 }
            val parentCount = list.sumOf { it.count }
            val parentPercent = (parentAmount / total).coerceIn(0.0, 1.0)

            // 获取父类图标
            val parentIcon = categoryDao.getByName(null, null, parent)?.icon

            // 构建子类列表（只包含有子类名称的记录）
            val children = list.filter { it.child.isNotEmpty() }.map { row ->
                val childAmount = row.amount ?: 0.0
                val childPercent = (childAmount / total).coerceIn(0.0, 1.0)

                // 获取子类图标
                val childIcon = categoryDao.getByName(null, null, row.child)?.icon

                CategoryItemDto(
                    name = row.child,
                    percent = childPercent,
                    count = row.count,
                    icon = childIcon
                )
            }

            CategoryItemDto(
                name = parent,
                percent = parentPercent,
                count = parentCount,
                icon = parentIcon,
                children = children
            )
        }.sortedByDescending { it.percent }
    }

    /**
     * 填充趋势数据的空白时间点
     */
    private fun fillTrendGaps(
        rows: List<org.ezbook.server.db.model.TrendRowModel>,
        startTime: Long,
        endTime: Long,
        isDaily: Boolean
    ): TrendDto {
        val dataMap = rows.associateBy { it.day }
        val labels = mutableListOf<String>()
        val incomes = mutableListOf<Double>()
        val expenses = mutableListOf<Double>()

        val cal = Calendar.getInstance().apply { timeInMillis = startTime }
        val endCal = Calendar.getInstance().apply { timeInMillis = endTime }

        if (isDaily) {
            normalizeToDayStart(cal)
            normalizeToDayStart(endCal)
            while (cal.before(endCal) || cal.timeInMillis == endCal.timeInMillis) {
                val key = formatDay(cal)
                val row = dataMap[key]
                labels.add(key.substring(5)) // MM-dd
                incomes.add(row?.income ?: 0.0)
                expenses.add(row?.expense ?: 0.0)
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else {
            normalizeToMonthStart(cal)
            normalizeToMonthStart(endCal)
            while (cal.before(endCal) || cal.timeInMillis == endCal.timeInMillis) {
                val key = formatMonth(cal)
                val row = dataMap[key]
                labels.add(key.substring(5)) // MM
                incomes.add(row?.income ?: 0.0)
                expenses.add(row?.expense ?: 0.0)
                cal.add(Calendar.MONTH, 1)
            }
        }

        return TrendDto(labels = labels, incomes = incomes, expenses = expenses)
    }

    private fun normalizeToDayStart(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }

    private fun normalizeToMonthStart(cal: Calendar) {
        cal.set(Calendar.DAY_OF_MONTH, 1)
        normalizeToDayStart(cal)
    }

    private fun formatDay(cal: Calendar): String {
        return String.format(
            Locale.US, "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun formatMonth(cal: Calendar): String {
        return String.format(
            Locale.US, "%04d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1
        )
    }
}


