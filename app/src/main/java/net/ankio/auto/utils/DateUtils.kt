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
import java.util.Date
import java.util.Locale

/**
 * 格式化时间工具类
 */
object DateUtils {


    fun getTime(format: String, time: Long): String {
        val adjustedTime = if (time.toString().length == 10) time * 1000 else time
        return SimpleDateFormat(format, Locale.getDefault()).format(Date(adjustedTime))
    }



    fun stampToDate(
        time: Long,
        format: String,
    ): String {
        val simpleDateFormat = SimpleDateFormat(format, Locale.getDefault())
        val date = Date(time)
        return simpleDateFormat.format(date)
    }



    fun getTime(t: Long): String {
        return getTime("yyyy-MM-dd HH:mm:ss", t)
    }

}
