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

package org.ezbook.server.models

/**
 * 统计数据：汇总信息
 * 提供总支出、总收入、结余、日均支出。
 */
data class SummaryDto(
    val totalExpense: Double,
    val totalIncome: Double,
    val net: Double,
    val dailyAvgExpense: Double
)

/**
 * 统计数据：趋势数据
 * labels 使用 MM-dd；同序返回 income 和 expense 数组。
 */
data class TrendDto(
    val labels: List<String>,
    val incomes: List<Double>,
    val expenses: List<Double>
)

/**
 * 统计数据：分类占比（父子结构）
 * percent 为 [0,1] 区间。
 */
data class CategoryItemDto(
    val name: String,
    val percent: Double,
    val count: Int = 0,
    val icon: String? = null,
    val children: List<CategoryItemDto> = emptyList()
)

/**
 * 统一统计响应。
 */
data class StatsResponse(
    val summary: SummaryDto,
    val trend: TrendDto,
    val expenseCategories: List<CategoryItemDto>,
    val incomeCategories: List<CategoryItemDto>
)


