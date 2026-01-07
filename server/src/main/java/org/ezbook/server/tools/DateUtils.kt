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

import java.util.TimeZone

/**
 * 日期时间工具
 *
 * 目标：将常见的日期时间字符串转换为 Unix 时间戳（毫秒/秒）。
 *
 * 解析策略（按优先级）：
 * 1) 纯数字：13位视为毫秒；10-12位视为秒
 * 2) ISO8601：优先解析 Instant/OffsetDateTime/LocalDateTime
 * 3) 常见格式：遍历一组常见的日期格式进行解析
 *
 * - 解析失败时抛出 IllegalArgumentException
 */
object DateUtils {

    /**
     * 将任意常见日期时间字符串解析为毫秒时间戳
     * @param input 输入字符串
     * @throws IllegalArgumentException 当输入不受支持时
     */
    fun toEpochMillis(input: String): Long {
        val text = input.trim()
        if (text.isEmpty()) throw IllegalArgumentException("Empty datetime string")

        // 1) 纯数字：长度>=13 视为毫秒；10-12 视为秒
        if (text.matches(Regex("^-?\\d+"))) {
            return when {
                text.length >= 13 -> text.toLong()
                text.length in 10..12 -> text.toLong() * 1000
                else -> throw IllegalArgumentException("Unsupported numeric epoch length: ${text.length}")
            }
        }

        // 2) 启发式自动识别（非纯数字）
        return parseHeuristically(text)
    }

    /**
     * 将常见日期时间字符串解析为秒时间戳（向下取整）
     */
    fun toEpochSeconds(input: String): Long = toEpochMillis(input) / 1000


    /**
     * 启发式日期时间解析核心（不处理输入时区、无 am/pm、无毫秒）
     *
     * 目标：不依赖固定模板，自动识别日期（年/月/日顺序）、时间（24h制）。
     * 策略：
     * 1) 规范化输入：替换中日韩时间单位与常见分隔，压缩空白，去除尾随时区标记。
     * 2) 支持紧凑数字：yyyyMMdd[HHmm[ss]]。
     * 3) 对带分隔的日期：优先定位 4 位年份，剩余日月根据 Locale（US -> MDY，其它 -> DMY）与 >12 的值判定。
     * 4) 时间部分支持：HH[:mm[:ss]] 或 HHmm[ss]。
     *
     * 失败时抛出 IllegalArgumentException，以契合既有行为。
     */
    private fun parseHeuristically(input: String): Long {
        val normalized = normalizeInput(input)
        val withoutZone = stripTrailingZoneInfo(normalized)

        // 拆分日期与时间（使用首个空格）
        val parts = withoutZone.split(" ", limit = 2)
        val datePart = parts.getOrNull(0)?.trim().orEmpty()
        val timePart = parts.getOrNull(1)?.trim().orEmpty()

        val (y, m, d) = parseDatePart(datePart)
            ?: throw IllegalArgumentException("Unsupported datetime format: $input")

        val (hh, mm, ss) = if (timePart.isEmpty()) Triple(0, 0, 0) else parseTimePart(timePart)
        // 基于本地时区（含夏令时）计算 epoch 毫秒
        return computeEpochMillisLocal(y, m, d, hh, mm, ss)
    }

    /**
     * 由 (year, month, day, hour, minute, second) 计算 epoch 毫秒，按本地时区解释。
     * 方式：先计算将该本地时间按 UTC 解读的毫秒，再减去本地时区在该瞬间的偏移。
     * 为了应对夏令时切换点的偏移变化，采用两步收敛：E1 = E0 - off(E0)；若 off(E1) 变化，则再用 off(E1)。
     */
    private fun computeEpochMillisLocal(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int
    ): Long {
        val e0 = computeEpochMillisUtc(year, month, day, hour, minute, second)
        val tz = TimeZone.getDefault()
        var off0 = tz.getOffset(e0)
        var e1 = e0 - off0
        val off1 = tz.getOffset(e1)
        if (off1 != off0) {
            off0 = off1
            e1 = e0 - off0
        }
        return e1
    }

    /**
     * 由 (year, month, day, hour, minute, second) 直接计算自 1970-01-01T00:00:00Z 起的毫秒数。
     * 不依赖任何 java.time API，纯数学，按 UTC 视角。
     */
    private fun computeEpochMillisUtc(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int
    ): Long {
        val days = daysSinceEpoch(year, month, day)
        val secondsOfDay = hour.toLong() * 3600L + minute.toLong() * 60L + second.toLong()
        return days * 86_400_000L + secondsOfDay * 1_000L
    }

    /**
     * 计算给定公历日期距离 1970-01-01 的天数（可为负）。
     * 实现采用 Howard Hinnant 的 days_from_civil 算法，适用于前后扩展的格里高利历。
     */
    private fun daysSinceEpoch(year: Int, month: Int, day: Int): Long {
        val y = year - if (month <= 2) 1 else 0
        val era = Math.floorDiv(y, 400)
        val yoe = y - era * 400
        val monthIndex = month + if (month > 2) -3 else 9 // Mar=0,...,Jan=10,Feb=11
        val doy = (153 * monthIndex + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + yoe / 400 + doy
        return era * 146097L + doe - 719468L
    }

    /**
     * 将自然语言/中日韩单位与常见分隔替换为标准形式：
     * - 日期分隔统一为 '-'，日期与时间用单个空格分隔
     * - 时间分隔统一为 ':'，毫秒以 '.'
     * - 'T' 视为空格
     */
    private fun normalizeInput(input: String): String {
        val lower = input.trim()
            .replace("T", " ")
            .replace("年", "-")
            .replace("月", "-")
            .replace("日", " ")
            .replace("号", " ")
            .replace("时", ":")
            .replace("点", ":")
            .replace("分", ":")
            .replace("秒", "")
            .replace("/", "-")
            .replace(".", "-")
        return lower.replace("\\s+".toRegex(), " ").trim()
    }

    /**
     * 去除尾随时区标记（Z、±HH:mm、±HHmm、±HH），不做任何时区换算。
     */
    private fun stripTrailingZoneInfo(input: String): String {
        var s = input.trim()
        if (s.endsWith("Z", ignoreCase = true)) s = s.removeSuffix("Z").trim()
        val tzMatch = Regex("([+-])(\\d{2})(?::?(\\d{2}))?$").find(s)
        if (tzMatch != null) {
            s = s.removeRange(tzMatch.range).trim()
        }
        return s
    }

    /**
     * 解析日期部分（带分隔符）。优先识别 4 位年份；月日歧义时：
     * - 若两者都 ≤12，使用 Locale：US -> MDY，其它 -> DMY；
     * - 若一个 >12，则该值为日。
     */
    /**
     * 解析日期部分（带分隔符/紧凑格式），不构造 LocalDate，直接返回 (year, month, day)。
     * 返回 null 表示无法解析或日期非法（如 2025-02-30）。
     */
    private fun parseDatePart(date: String): Triple<Int, Int, Int>? {
        if (date.isEmpty()) return null
        // 纯数字日期（可能是 yyyyMMdd 或 MMdd）
        if (date.matches(Regex("^\\d{8}$"))) {
            val y = date.substring(0, 4).toInt()
            val m = date.substring(4, 6).toInt()
            val d = date.substring(6, 8).toInt()
            return if (isValidDate(y, m, d)) Triple(y, m, d) else null
        }
        if (date.matches(Regex("^\\d{4}$"))) {
            // MMdd 格式，补充当前年份
            val y = currentYear()
            val m = date.substring(0, 2).toInt()
            val d = date.substring(2, 4).toInt()
            return if (isValidDate(y, m, d)) Triple(y, m, d) else null
        }

        val tokens = date.split("-").filter { it.isNotBlank() }

        // 只有月日（如 "01-05"），补充当前年份
        if (tokens.size == 2) {
            val a = tokens[0].toIntOrNull() ?: return null
            val b = tokens[1].toIntOrNull() ?: return null
            val (m, d) = resolveMonthDay(a, b)
            val y = currentYear()
            return if (isValidDate(y, m, d)) Triple(y, m, d) else null
        }
        
        if (tokens.size < 3) return null

        // 找出 4 位年份位置
        val yearIdx = tokens.indexOfFirst { it.length == 4 && it.all { c -> c.isDigit() } }
        val (y, m, d) = if (yearIdx >= 0) {
            val year = tokens[yearIdx].toInt()
            val rest = tokens.filterIndexed { idx, _ -> idx != yearIdx }
            val a = rest.getOrNull(0)?.toIntOrNull() ?: return null
            val b = rest.getOrNull(1)?.toIntOrNull() ?: return null
            // 年在前（YYYY-..-..）时固定按 年-月-日 解释，避免受 Locale 影响导致月日互换
            val (month, day) = if (yearIdx == 0) a to b else resolveMonthDay(a, b)
            Triple(year, month, day)
        } else {
            // 无 4 位年份：尝试三段推断（如 dd-MM-yy 或 dd-MM-yyyy 已在上分支）
            val a = tokens.getOrNull(0)?.toIntOrNull() ?: return null
            val b = tokens.getOrNull(1)?.toIntOrNull() ?: return null
            val c = tokens.getOrNull(2)?.toIntOrNull() ?: return null
            // 若第三段是 4 位，则为年；否则使用 2000-2099 假定（避免歧义引入异常）
            val year = when {
                tokens[2].length == 4 -> c
                tokens[0].length == 4 -> a
                else -> (if (c < 100) 2000 + c else c)
            }
            val firstTwo =
                if (tokens[0].length == 4) listOf(tokens[1].toInt(), tokens[2].toInt()) else listOf(
                    a,
                    b
                )
            val (month, day) = resolveMonthDay(firstTwo[0], firstTwo[1])
            Triple(year, month, day)
        }

        return if (isValidDate(y, m, d)) Triple(y, m, d) else null
    }

    /**
     * 校验给定年月日是否构成有效日期。
     */
    private fun isValidDate(year: Int, month: Int, day: Int): Boolean {
        if (month !in 1..12) return false
        val maxDay = maxDayOfMonth(year, month)
        return day in 1..maxDay
    }

    /**
     * 返回指定年月的最大天数，闰年 2 月为 29 天。
     */
    private fun maxDayOfMonth(year: Int, month: Int): Int {
        return when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> 0
        }
    }

    /**
     * 闰年判定：公历规则。
     */
    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    /**
     * 获取当前年份
     */
    private fun currentYear(): Int {
        val now = System.currentTimeMillis()
        val tz = TimeZone.getDefault()
        val offset = tz.getOffset(now)
        val localMillis = now + offset
        // 从 epoch 毫秒反推年份（近似，足够日常使用）
        val days = (localMillis / 86_400_000L).toInt() + 719468
        val era = Math.floorDiv(days, 146097)
        val doe = days - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        return yoe + era * 400
    }

    /**
     * 解析月日顺序：
     * - 若一方 > 12，则该值为日（另一方为月）。
     * - 否则默认使用 MDY（“月在前、日在后”）。
     */
    private fun resolveMonthDay(x: Int, y: Int): Pair<Int, Int> {
        return when {
            x > 12 && y in 1..12 -> y to x // x 是日
            y > 12 && x in 1..12 -> x to y // y 是日
            else -> x to y // 默认 MDY
        }
    }

    /**
     * 解析时间部分，不使用 LocalTime，直接返回 (hour, minute, second)。
     */
    private fun parseTimePart(timeRaw: String): Triple<Int, Int, Int> {
        val s = timeRaw.trim().lowercase()

        // 带分隔的时间 HH[:mm[:ss]]
        val sep = Regex("^(\\d{1,2})(?::(\\d{1,2}))?(?::(\\d{1,2}))?$")
        val m1 = sep.matchEntire(s)
        if (m1 != null) {
            val hh = m1.groupValues[1].toInt()
            val mm = m1.groupValues.getOrNull(2)?.takeIf { it.isNotEmpty() }?.toInt() ?: 0
            val ss = m1.groupValues.getOrNull(3)?.takeIf { it.isNotEmpty() }?.toInt() ?: 0
            return Triple(hh, mm, ss)
        }

        // 紧凑时间 HH、HHmm 或 HHmmss
        if (s.matches(Regex("^\\d{2}$"))) {
            val hh = s.substring(0, 2).toInt()
            return Triple(hh, 0, 0)
        }
        if (s.matches(Regex("^\\d{4}$"))) {
            val hh = s.substring(0, 2).toInt()
            val mm = s.substring(2, 4).toInt()
            return Triple(hh, mm, 0)
        }
        if (s.matches(Regex("^\\d{6}$"))) {
            val hh = s.substring(0, 2).toInt()
            val mm = s.substring(2, 4).toInt()
            val ss = s.substring(4, 6).toInt()
            return Triple(hh, mm, ss)
        }

        // 无法解析时间
        throw IllegalArgumentException("Unsupported time format: $timeRaw")
    }
}