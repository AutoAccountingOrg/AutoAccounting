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

package net.ankio.auto.xposed.hooks.qianji.filter

import android.content.Context
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.core.api.HookerClazz
import net.ankio.dex.model.Clazz
import java.util.Calendar

/**
 * 钱迹宿主 `DateFilter` 的轻量包装（Xposed 反射驱动）。
 *
 * 约束：
 * - 不复制业务逻辑，全部委托宿主实现（Never break userspace）。
 * - Kotlin 风格、方法职责单一，保持与 `BookFilter` 包装一致的 API 风格。
 */
class DataFilter private constructor(private var filterObj: Any?) {

    companion object : HookerClazz() {
        /** 宿主类名 */
        private const val CLAZZ = "com.mutangtech.qianji.filter.filters.DateFilter"

        /** 与 Manifest 对齐的精确类名规则 */
        override var rule = Clazz(name = this::class.java.name, nameRule = CLAZZ)

        // 宿主 flag 常量（只作为调用方可读的别名，不在本地计算逻辑）
        const val FLAG_LATEST_1_YEARS = 101
        const val FLAG_THIS_2_YEARS = 102
        const val FLAG_ALL = 103
        const val FLAG_THIS_3_YEARS = 104

        const val QUARTER_1 = 1
        const val QUARTER_2 = 2
        const val QUARTER_3 = 3
        const val QUARTER_4 = 4

        /** 新建一个空过滤器实例 */
        fun newInstance(): DataFilter = fromObject(XposedHelpers.newInstance(clazz()))

        /** 包装已有宿主对象为 `DataFilter` */
        fun fromObject(obj: Any): DataFilter {
            if (obj::class.java.name != CLAZZ) {
                throw IllegalArgumentException("${obj::class.java.name} must be instance of $CLAZZ")
            }
            return DataFilter(obj)
        }

        /** 拷贝 */
        fun copy(of: DataFilter): DataFilter =
            fromObject(XposedHelpers.callStaticMethod(clazz(), "copy", of.toObject())!!)

        /** 全时段过滤器 */
        fun newAllFilter(): DataFilter =
            fromObject(XposedHelpers.callStaticMethod(clazz(), "newAllFilter")!!)

        /** 当月过滤器（当前月份） */
        fun newMonthFilter(): DataFilter =
            fromObject(XposedHelpers.callStaticMethod(clazz(), "newMonthFilter")!!)

        /** 指定年月过滤器 */
        fun newMonthFilter(year: Int, month1Based: Int): DataFilter =
            fromObject(
                XposedHelpers.callStaticMethod(
                    clazz(),
                    "newMonthFilter",
                    year,
                    month1Based,
                )!!
            )

        /** 基于日历构造月份过滤器 */
        fun newMonthFromCalendar(calendar: Calendar): DataFilter =
            fromObject(
                XposedHelpers.callStaticMethod(
                    clazz(),
                    "newMonthFromCalendar",
                    calendar,
                )!!
            )

        /** 指定季度过滤器 */
        fun newQuarterFilter(year: Int, quarter: Int): DataFilter =
            fromObject(
                XposedHelpers.callStaticMethod(
                    clazz(),
                    "newQuarterFilter",
                    year,
                    quarter,
                )!!
            )

        /** 当年过滤器（当前年） */
        fun newYearFilter(): DataFilter =
            fromObject(XposedHelpers.callStaticMethod(clazz(), "newYearFilter")!!)

        /** 指定年份过滤器 */
        fun newYearFilter(year: Int): DataFilter =
            fromObject(XposedHelpers.callStaticMethod(clazz(), "newYearFilter", year)!!)

        /** 文案标题（直接委托宿主静态方法） */
        fun getTitle(flag: Int, context: Context): String? =
            XposedHelpers.callStaticMethod(clazz(), "getTitle", flag, context) as String?
    }

    /** 取出底层宿主对象 */
    fun toObject(): Any = filterObj!!

    /** 结束日期（Calendar） */
    fun getEnd(): Calendar? = XposedHelpers.callMethod(filterObj, "getEnd") as Calendar?

    /** 结束秒（long） */
    fun getEndInSecond(): Long = XposedHelpers.callMethod(filterObj, "getEndInSecond") as Long

    /** 键名：month/year/date */
    fun getKey(): String = XposedHelpers.callMethod(filterObj, "getKey") as String

    /** 月份（1-12） */
    fun getMonth(): Int = XposedHelpers.callMethod(filterObj, "getMonth") as Int

    /** 起始日期（Calendar） */
    fun getStart(): Calendar? = XposedHelpers.callMethod(filterObj, "getStart") as Calendar?

    /** 起始秒（long） */
    fun getStartInSecond(): Long = XposedHelpers.callMethod(filterObj, "getStartInSecond") as Long

    /** 字符串值：如 "year" -> "2025"，"date" -> "start,end" */
    fun value(): String = XposedHelpers.callMethod(filterObj, "getValue") as String

    /** 年份 */
    fun getYear(): Int = XposedHelpers.callMethod(filterObj, "getYear") as Int

    /** 是否全时段 */
    fun isAllTime(): Boolean = XposedHelpers.callMethod(filterObj, "isAllTime") as Boolean

    /** 是否当前月 */
    fun isCurrentMonth(): Boolean = XposedHelpers.callMethod(filterObj, "isCurrentMonth") as Boolean

    /** 是否当前年 */
    fun isCurrentYear(): Boolean = XposedHelpers.callMethod(filterObj, "isCurrentYear") as Boolean

    /** 是否起止日期区间模式 */
    fun isDateRangeFilter(): Boolean =
        XposedHelpers.callMethod(filterObj, "isDateRangeFilter") as Boolean

    /** 是否上个月 */
    fun isLastMonth(): Boolean = XposedHelpers.callMethod(filterObj, "isLastMonth") as Boolean

    /** 是否去年 */
    fun isLastYear(): Boolean = XposedHelpers.callMethod(filterObj, "isLastYear") as Boolean

    /** 是否最近一年 */
    fun isLatest1Years(): Boolean = XposedHelpers.callMethod(filterObj, "isLatest1Years") as Boolean

    /** 是否按月份过滤 */
    fun isMonthFilter(): Boolean = XposedHelpers.callMethod(filterObj, "isMonthFilter") as Boolean

    /** 是否季度过滤 */
    fun isQuarter(): Boolean = XposedHelpers.callMethod(filterObj, "isQuarter") as Boolean

    /** 是否当两年 */
    fun isThis2Years(): Boolean = XposedHelpers.callMethod(filterObj, "isThis2Years") as Boolean

    /** 是否当三年 */
    fun isThis3Years(): Boolean = XposedHelpers.callMethod(filterObj, "isThis3Years") as Boolean

    /** 是否有效 */
    fun isValidate(): Boolean = XposedHelpers.callMethod(filterObj, "isValidate") as Boolean

    /** 是否按年份过滤 */
    fun isYearFilter(): Boolean = XposedHelpers.callMethod(filterObj, "isYearFilter") as Boolean

    /** 设置 flag（会清除年月与起止） */
    fun setFlag(flag: Int): DataFilter = apply {
        XposedHelpers.callMethod(filterObj, "setFlag", flag)
    }

    /** 设置月份过滤 */
    fun setMonthFilter(year: Int, month1Based: Int): DataFilter = apply {
        XposedHelpers.callMethod(filterObj, "setMonthFilter", year, month1Based)
    }

    /** 设置月份过滤（从日历） */
    fun setMonthFilter(calendar: Calendar): DataFilter = apply {
        XposedHelpers.callMethod(filterObj, "setMonthFilter", calendar)
    }

    /** 切换到上/下一月 */
    fun newMonthSwitch(prev: Boolean): DataFilter =
        fromObject(XposedHelpers.callMethod(filterObj, "newMonthSwitch", prev)!!)

    /** 设置起止区间过滤（flag 可指定 ALL/THIS_2_YEARS/THIS_3_YEARS 等） */
    fun setTimeRangeFilter(start: Calendar?, end: Calendar?, flag: Int = -1): DataFilter = apply {
        if (flag == -1) {
            XposedHelpers.callMethod(filterObj, "setTimeRangeFilter", start, end)
        } else {
            XposedHelpers.callMethod(filterObj, "setTimeRangeFilter", start, end, flag)
        }
    }

    /** 设置年份过滤 */
    fun setYearFilter(year: Int): DataFilter = apply {
        XposedHelpers.callMethod(filterObj, "setYearFilter", year)
    }
}