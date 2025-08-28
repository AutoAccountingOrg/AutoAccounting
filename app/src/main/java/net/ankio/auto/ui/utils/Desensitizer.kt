/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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

package net.ankio.auto.ui.utils

/**
 * 脱敏结果数据类
 * @param masked 脱敏后的字符串
 * @param changes 脱敏变更记录，包含原始值和脱敏后的值
 */
data class DesensitizeResult(
    val masked: String,
    val changes: List<Pair<String, String>>
)

/**
 * 脱敏规则数据类
 * @param regex 匹配正则表达式
 * @param replacer 替换函数
 */
private data class Rule(
    val regex: Regex,
    val replacer: (MatchResult) -> String
)

/**
 * 数据脱敏工具类
 * 提供对敏感数据的脱敏处理功能，包括手机号、身份证、银行卡号等
 */
object Desensitizer {

    /** 数字 → 0；其余字符照抄，保持格式和长度 */
    private val zeroDigits: (MatchResult) -> String = { mr ->
        buildString {
            for (ch in mr.value) append(if (ch.isDigit()) '0' else ch)
        }
    }

    /**
     * 生成与原始值相同长度的数字替换值
     * 保持第一位数字特征，其余位用0填充
     */
    private fun generateSameLengthDigits(original: String): String {
        return buildString {
            for (i in original.indices) {
                val ch = original[i]
                when {
                    ch.isDigit() -> {
                        // 第一位保持原有特征（如手机号1开头），其余位用0
                        append(if (i == 0) ch else '0')
                    }

                    else -> append(ch)
                }
            }
        }
    }

    /**
     * 生成与原始值相同长度的字母数字混合替换值
     * 保持格式特征
     */
    private fun generateSameFormatAlphaNum(original: String, prefix: String = ""): String {
        return buildString {
            if (prefix.isNotEmpty()) {
                append(prefix)
            }
            for (i in (if (prefix.isNotEmpty()) prefix.length else 0) until original.length) {
                val ch = original[i]
                when {
                    ch.isDigit() -> append('0')
                    ch.isLetter() -> append(if (ch.isUpperCase()) 'X' else 'x')
                    else -> append(ch)
                }
            }
        }
    }

    /**
     * 生成相同长度的中文姓名
     */
    private fun generateSameLengthName(original: String): String {
        return when (original.length) {
            2 -> "张三"
            3 -> "张三丰"
            4 -> "欧阳修文"
            else -> "张" + "三".repeat(original.length - 1)
        }
    }

    /** 仅当出现货币符号 / 单位时才匹配金额 */
    private val amountRegex =
        "(?xi)(?: [¥￥€]\\s*\\d+(?:,\\d{3})*(?:\\.\\d{1,2})? | \\d+(?:,\\d{3})*(?:\\.\\d{1,2})?\\s*(?:元|块|人民币|美元|USD|CNY|EUR) )".toRegex()

    /**
     * 脱敏规则列表
     * 使用 CopyOnWriteArrayList 保证线程安全
     */
    private val rules = java.util.concurrent.CopyOnWriteArrayList(
        listOf(
            // 手机号：保持1开头，其余位用0填充，保持11位
            Rule("\\b1[3-9]\\d{9}\\b".toRegex()) { mr -> generateSameLengthDigits(mr.value) },

            // 邮箱：保持原始长度和格式
            Rule("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}".toRegex()) { mr ->
                val parts = mr.value.split("@")
                if (parts.size == 2) {
                    val localPart = "x".repeat(parts[0].length.coerceAtLeast(1))
                    val domainParts = parts[1].split(".")
                    val domain = domainParts.mapIndexed { index, part ->
                        if (index == domainParts.lastIndex) part // 保持顶级域名
                        else "x".repeat(part.length.coerceAtLeast(1))
                    }.joinToString(".")
                    "$localPart@$domain"
                } else {
                    "example@example.com"
                }
            },

            // 身份证：保持18位格式，前6位地区码保持，其余用0填充
            Rule("\\b\\d{6}(19|20)?\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]\\b".toRegex()) { mr ->
                val original = mr.value
                when (original.length) {
                    15 -> "110101000000000" // 15位身份证
                    18 -> "110101000000000000" // 18位身份证，最后一位可能是X
                    else -> generateSameLengthDigits(original)
                }
            },

            // 银行卡：保持原始位数，首位保持，其余用0填充
            Rule("\\b\\d{16,19}\\b".toRegex()) { mr -> generateSameLengthDigits(mr.value) },

            // 括号内数字：保持位数，全部用0填充
            Rule("(?<=[(（])\\d{4}(?=[)）])".toRegex()) { mr -> "0".repeat(mr.value.length) },

            // 中文姓名：根据长度生成相应的姓名
            Rule("\\b[\\u4E00-\\u9FA5]{2,4}\\b".toRegex()) { mr -> generateSameLengthName(mr.value) },

            // 护照：保持首字母和长度
            Rule("\\b[EGPSeqg]\\d{8}\\b".toRegex()) { mr ->
                generateSameFormatAlphaNum(mr.value, mr.value.substring(0, 1))
            },

            // 港澳台通行证：保持首字母和长度
            Rule("\\b[HMhm]\\d{8,10}\\b".toRegex()) { mr ->
                generateSameFormatAlphaNum(mr.value, mr.value.substring(0, 1))
            },

            // 金额：保持格式和长度
            Rule(amountRegex, zeroDigits)
        )
    )

    /**
     * 动态增加自定义脱敏规则
     * @param regex 匹配正则表达式
     * @param replacer 替换函数，返回脱敏后的占位值
     */
    fun register(regex: Regex, replacer: (MatchResult) -> String) {
        rules += Rule(regex, replacer)
    }

    /**
     * 对字符串进行全面脱敏处理
     * @param src 原始字符串
     * @return 脱敏结果，包含脱敏后的字符串和变更记录
     */
    fun maskAll(src: String): DesensitizeResult {
        var out: String = src
        val log = mutableListOf<Pair<String, String>>()

        for (rule in rules) {
            out = rule.regex.replace(out) { mr ->
                val repl = rule.replacer(mr)
                log += mr.value to repl
                repl
            }
        }
        return DesensitizeResult(out, log)
    }
}
