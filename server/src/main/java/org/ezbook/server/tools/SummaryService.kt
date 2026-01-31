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

import com.google.gson.Gson
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.BillSummaryModel
import org.ezbook.server.db.model.CategoryStatsModel
import org.ezbook.server.db.model.ShopStatsModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 账单摘要服务 - 生成 AI 分析所需的数据
 */
object SummaryService {
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * 周期数据
     */
    data class PeriodData(
        val income: Double,
        val expense: Double,
        val savingsRate: Double,
        val expenseIncomeRatio: Double,
        val expenseCategories: List<CategoryStatsModel>,
        val incomeCategories: List<CategoryStatsModel>,
        val shops: List<ShopStatsModel>,
        val bills: List<BillSummaryModel>
    )

    /**
     * 获取周期数据
     */
    suspend fun getPeriodData(startTime: Long, endTime: Long): PeriodData {
        val dao = Db.get().billInfoDao()

        val income = dao.getMonthlyIncome(startTime, endTime) ?: 0.0
        val expense = dao.getMonthlyExpense(startTime, endTime) ?: 0.0
        val netIncome = income - expense
        val savingsRate = if (income > 0) (netIncome / income) * 100 else 0.0
        val expenseIncomeRatio = if (income > 0) (expense / income) * 100 else 0.0

        return PeriodData(
            income = income,
            expense = expense,
            savingsRate = savingsRate,
            expenseIncomeRatio = expenseIncomeRatio,
            expenseCategories = dao.getExpenseCategoryStatsForAI(startTime, endTime).take(50),
            incomeCategories = dao.getIncomeCategoryStatsForAI(startTime, endTime).take(50),
            shops = dao.getExpenseShopStats(startTime, endTime).take(20),
            bills = dao.getBillSamples(startTime, endTime, 1000)
        )
    }

    /**
     * 生成财务分析数据 JSON
     */
    suspend fun generateSummary(startTime: Long, endTime: Long, periodName: String): String {
        val duration = endTime - startTime
        val prevStart = startTime - duration

        val current = getPeriodData(startTime, endTime)
        val last = getPeriodData(prevStart, startTime)

        return buildFinancialJson(periodName, startTime, endTime, current, last)
    }

    /**
     * 构建财务分析 JSON
     */
    private fun buildFinancialJson(
        periodName: String,
        startTime: Long,
        endTime: Long,
        current: PeriodData,
        last: PeriodData
    ): String {
        if (current.income == 0.0 && current.expense == 0.0) {
            return gson.toJson(
                mapOf(
                    "period" to periodName,
                    "basicStats" to mapOf("hasIncome" to false)
                )
            )
        }

        val periodDays = ((endTime - startTime) / (24L * 3600_000L)).coerceAtLeast(1)

        // 构建分类历史对比
        val categoryHistory = current.expenseCategories.map { curr ->
            val prev = last.expenseCategories.find { it.cateName == curr.cateName }
            mapOf(
                "category" to curr.cateName,
                "currentAmount" to curr.amount,
                "lastPeriodAmount" to (prev?.amount ?: 0.0)
            )
        }

        return gson.toJson(
            mapOf(
                "period" to periodName,
                "periodDays" to periodDays,
                "basicStats" to mapOf(
                    "totalIncome" to current.income,
                    "totalExpense" to current.expense,
                    "savingsRate" to current.savingsRate,
                    "expenseIncomeRatio" to current.expenseIncomeRatio,
                    "hasIncome" to (current.income > 0),
                    "avgIncomePerDay" to current.income / periodDays,
                    "avgExpensePerDay" to current.expense / periodDays
                ),
                "expenseByCategory" to current.expenseCategories.map {
                    mapOf(
                        "category" to it.cateName,
                        "amount" to it.amount,
                        "percentage" to if (current.expense > 0) (it.amount / current.expense) * 100 else 0.0,
                        "count" to it.count,
                        "avgPerDay" to it.amount / periodDays
                    )
                },
                "incomeByCategory" to current.incomeCategories.map {
                    mapOf(
                        "category" to it.cateName,
                        "amount" to it.amount,
                        "percentage" to if (current.income > 0) (it.amount / current.income) * 100 else 0.0,
                        "count" to it.count,
                        "avgPerDay" to it.amount / periodDays
                    )
                },
                "historicalData" to mapOf(
                    "lastPeriod" to mapOf(
                        "totalIncome" to last.income,
                        "totalExpense" to last.expense,
                        "savingsRate" to last.savingsRate,
                        "expenseIncomeRatio" to last.expenseIncomeRatio
                    ),
                    "categoryHistory" to categoryHistory
                ),
                "merchantAnalysis" to mapOf(
                    "topByAmount" to current.shops.map {
                        mapOf("merchant" to it.shopName, "amount" to it.amount, "count" to it.count)
                    },
                    "topByFrequency" to current.shops.sortedByDescending { it.count }.map {
                        mapOf("merchant" to it.shopName, "amount" to it.amount, "count" to it.count)
                    }
                ),
                "bills" to current.bills.map { bill ->
                    val calendar = Calendar.getInstance().apply { timeInMillis = bill.time }
                    mapOf(
                        "type" to bill.type,
                        "amount" to bill.money,
                        "category" to bill.cateName,
                        "merchant" to bill.shopName,
                        "item" to bill.shopItem,
                        "time" to bill.time,
                        "date" to dateFormat.format(Date(bill.time)),
                        "hour" to calendar.get(Calendar.HOUR_OF_DAY)
                    )
                }
            )
        )
    }

}
