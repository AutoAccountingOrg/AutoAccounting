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

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 格式化时间工具类
 */
object DateUtils {


    fun dateToStamp(dateString: String, format: String): Long{
        return try {
            val simpleDateFormat = SimpleDateFormat(format, Locale.getDefault())
            simpleDateFormat.timeZone = TimeZone.getDefault()
            val date = simpleDateFormat.parse(dateString)
            date?.time?:System.currentTimeMillis()
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

    fun stampToDate(time: Long):String{
        return stampToDate(time,"yyyy-MM-dd HH:mm:ss")
    }

    fun twoMonthsLater(): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.MONTH, 2) // 增加两个月
        return calendar.timeInMillis
    }

}
