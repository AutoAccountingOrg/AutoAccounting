/*
 * Copyright (C) 2026 ankio(ankio@ankio.net)
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

package net.ankio.auto.utils

import android.content.Context
import android.view.View
import net.ankio.auto.R
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.DateTimePickerDialog
import net.ankio.auto.ui.utils.ListPopupUtilsGeneric
import net.ankio.auto.ui.utils.ToastUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * 周期筛选工具类，用于统一管理各个页面的时间范围选择
 */
object PeriodSelector {

    /**
     * 周期类型枚举
     */
    enum class Period {
        LAST_WEEK,      // 最近一周
        LAST_30_DAYS,   // 最近30天
        THIS_MONTH,     // 这个月
        LAST_YEAR,      // 最近一年
        THIS_YEAR,      // 今年
        CUSTOM          // 自定义区间
    }

    /**
     * 周期数据类
     */
    data class PeriodData(
        val type: Period,
        val startTime: Long,
        val endTime: Long,
        val displayName: String
    )

    /**
     * 显示周期选择弹窗
     */
    fun show(
        context: Context,
        anchorView: View,
        currentStart: Long = 0L,
        currentEnd: Long = 0L,
        onSelected: (startTime: Long, endTime: Long, label: String) -> Unit
    ) {
        val periodMap = mapOf(
            context.getString(R.string.period_last_week) to Period.LAST_WEEK,
            context.getString(R.string.period_last_30_days) to Period.LAST_30_DAYS,
            context.getString(R.string.period_this_month) to Period.THIS_MONTH,
            context.getString(R.string.period_last_year) to Period.LAST_YEAR,
            context.getString(R.string.period_this_year) to Period.THIS_YEAR,
            context.getString(R.string.period_custom_range) to Period.CUSTOM
        )

        ListPopupUtilsGeneric.create<Period>(context)
            .setAnchor(anchorView)
            .setList(periodMap)
            .setOnItemClick { _, _, period ->
                if (period == Period.CUSTOM) {
                    showCustomRangePicker(context, currentStart, currentEnd, onSelected)
                    return@setOnItemClick
                }
                val periodData = calculatePeriodData(context, period)
                onSelected(periodData.startTime, periodData.endTime, periodData.displayName)
            }
            .show()
    }

    /**
     * 计算周期的时间范围
     */
    fun calculatePeriodData(context: Context, period: Period): PeriodData {
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()

        return when (period) {
            Period.LAST_WEEK -> {
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                resetTime(calendar)
                PeriodData(
                    period,
                    calendar.timeInMillis,
                    now,
                    context.getString(R.string.period_last_week)
                )
            }

            Period.LAST_30_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -29)
                resetTime(calendar)
                PeriodData(
                    period,
                    calendar.timeInMillis,
                    now,
                    context.getString(R.string.period_last_30_days)
                )
            }

            Period.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                resetTime(calendar)
                PeriodData(
                    period,
                    calendar.timeInMillis,
                    now,
                    context.getString(R.string.period_this_month)
                )
            }

            Period.LAST_YEAR -> {
                calendar.add(Calendar.DAY_OF_YEAR, -364)
                resetTime(calendar)
                PeriodData(
                    period,
                    calendar.timeInMillis,
                    now,
                    context.getString(R.string.period_last_year)
                )
            }

            Period.THIS_YEAR -> {
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                resetTime(calendar)
                PeriodData(
                    period,
                    calendar.timeInMillis,
                    now,
                    context.getString(R.string.period_this_year)
                )
            }

            Period.CUSTOM -> {
                PeriodData(period, now, now, context.getString(R.string.period_custom_range))
            }
        }
    }

    private fun resetTime(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    /**
     * 自定义区间选择
     */
    private fun showCustomRangePicker(
        context: Context,
        currentStart: Long,
        currentEnd: Long,
        onSelected: (startTime: Long, endTime: Long, label: String) -> Unit
    ) {
        BaseSheetDialog.create<DateTimePickerDialog>(context)
            .setTitle(context.getString(R.string.period_custom_range))
            .setRangeMode(true)
            .apply {
                if (currentStart != 0L && currentEnd != 0L) {
                    setDateRangeFromMillis(currentStart, currentEnd)
                }
            }
            .setOnDateRangeSelected { start, end ->
                if (end < start) {
                    ToastUtils.error(context.getString(R.string.time_range_invalid))
                    return@setOnDateRangeSelected
                }
                onSelected(start, end, formatRangeLabel(start, end))
            }
            .show()
    }

    /**
     * 格式化区间标题
     */
    fun formatRangeLabel(start: Long, end: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return "${format.format(Date(start))} ~ ${format.format(Date(end))}"
    }
}
