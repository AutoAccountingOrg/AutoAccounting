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

    fun getTime(format: String): String {
        return getTime(format, 0)
    }

    fun getTime(format: String, day: Int): String {
        val time = System.currentTimeMillis() + day * 24 * 60 * 60 * 1000L
        return SimpleDateFormat(format, Locale.getDefault()).format(Date(time))
    }

    fun getShortTime(date: Long, format: String): String {
        return SimpleDateFormat(format, Locale.getDefault()).format(Date(date))
    }

    fun getTime(format: String, time: Long): String {
        return SimpleDateFormat(format, Locale.getDefault()).format(Date(time))
    }

    /*
     * 将时间转换为时间戳
     */
    fun dateToStamp(time: String, format: String): Long {
        val simpleDateFormat = SimpleDateFormat(format, Locale.getDefault())
        return try {
            val date = simpleDateFormat.parse(time)
            date?.time ?: 0
        } catch (e: Throwable) {
            0
        }
    }

    fun stampToDate(time: Long, format: String): String {
        val simpleDateFormat = SimpleDateFormat(format, Locale.getDefault())
        val date = Date(time)
        return simpleDateFormat.format(date)
    }

    fun stampToDate(time: String, format: String): String {
        val simpleDateFormat = SimpleDateFormat(format, Locale.getDefault())
        val date = Date(time.toLong())
        return simpleDateFormat.format(date)
    }

    fun getAnyTime(time: String): Long {
        return try {
            if ("undefined" in time || time.isEmpty()) {
                throw Throwable("not useful date")
            }
            val t: Long
            t = if ((time.length == 10 || time.length == 13) && !time.contains(" ")) {
                var parsedTime = time.toLong()
                if (time.length == 10) {
                    parsedTime *= 1000
                }
                parsedTime
            } else {
                var format = ""
                var t2: Array<String>
                var modifiedTime = time.replace("号", "日")

                if ("日" in modifiedTime) {
                    format += "yyyy年MM月dd日"
                    val strings = modifiedTime.split("日")
                    var t3 = strings[0]
                    if (!t3.contains("月")) {
                        val month = getTime("MM")
                        t3 = "$month 月 $t3"
                    }
                    if (!t3.contains("年")) {
                        val year = getTime("yyyy")
                        t3 = "$year 年 $t3"
                    }
                    modifiedTime = "$t3 日 ${strings[1]}"
                } else if ("-" in modifiedTime) {
                    format += "yyyy-MM-dd"
                    val strings = modifiedTime.split("-")
                    if (strings.size == 2) {
                        modifiedTime = getTime("yyyy-") + modifiedTime
                    }
                } else if ("/" in modifiedTime) {
                    format += "yyyy/MM/dd"
                    val strings = modifiedTime.split("/")
                    if (strings.size == 2) {
                        modifiedTime = getTime("yyyy/") + modifiedTime
                    }
                }

                if (!format.contains("dd")) {
                    format = "yyyy-MM-dd"
                    modifiedTime = getTime("yyyy-MM-dd ") + modifiedTime
                }

                if (" " in modifiedTime) {
                    format += " "
                }

                if (":" in modifiedTime) {
                    t2 = modifiedTime.split(":").toTypedArray()
                    format += if (t2.size == 3) {
                        "HH:mm:ss"
                    } else {
                        "HH:mm"
                    }
                }
                if ("时" in modifiedTime) {
                    format += "HH时"
                }
                if ("分" in modifiedTime) {
                    format += "mm分"
                }
                if ("秒" in modifiedTime) {
                    format += "ss秒"
                }

                println("Time原始数据：$modifiedTime 计算格式化数据:$format")
                dateToStamp(modifiedTime, format)
            }
            t
        } catch (e: Throwable) {
            dateToStamp(getTime("yyyy-MM-dd HH:mm:ss"), "yyyy-MM-dd HH:mm:ss")
        }
    }

    fun getTime(t: Long): String {
        return getTime("yyyy-MM-dd HH:mm:ss", t)
    }

    fun afterDay(s: String, s1: String): Boolean {
        return try {
            val simpleDateFormat = SimpleDateFormat(s,Locale.CHINA)
            val afterDay = simpleDateFormat.parse(s1)
            val now = Date()
            now.after(afterDay)
        } catch (e: Exception) {
            false
        }
    }
}
