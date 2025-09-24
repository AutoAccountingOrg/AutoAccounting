/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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
import net.ankio.auto.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 格式化时间工具类
 */
object DateUtils {


    fun dateToStamp(dateString: String, format: String): Long {
        return try {
            val simpleDateFormat = SimpleDateFormat(format, Locale.getDefault())
            simpleDateFormat.timeZone = TimeZone.getDefault()
            val date = simpleDateFormat.parse(dateString)
            date?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }


    fun stampToDate(
        time: Long,
        format: String,
    ): String {
        val simpleDateFormat = SimpleDateFormat(format, Locale.getDefault())
        val date = Date(time)
        return simpleDateFormat.format(date)
    }

    fun stampToDate(time: Long): String {
        return stampToDate(time, "yyyy-MM-dd HH:mm:ss")
    }

    fun twoMonthsLater(): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.MONTH, 2) // 增加两个月
        return calendar.timeInMillis
    }

    /**
     * 将年月日时分秒转换为时间戳
     *
     * @param year 年份
     * @param month 月份（1-12）
     * @param day 日期
     * @param hour 小时（0-23）
     * @param minute 分钟（0-59）
     * @param second 秒（0-59）
     * @return 时间戳（毫秒）
     */
    fun getTime(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day, hour, minute, second) // month - 1 因为 Calendar 月份从0开始
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * 格式化时间范围显示
     * @param context 上下文，用于获取字符串资源
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @return 格式化后的时间范围字符串
     */
    fun formatTimeRange(context: Context, startTime: Long, endTime: Long): String {
        val startDate = stampToDate(startTime, "yyyy-MM-dd")
        val endDate = stampToDate(endTime, "yyyy-MM-dd")

        return if (startDate == endDate) {
            startDate // 同一天只显示一个日期
        } else {
            context.getString(R.string.date_range_simple, startDate, endDate)
        }
    }


}
