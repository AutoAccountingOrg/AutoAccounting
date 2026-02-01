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

package org.ezbook.server.db.model

/**
 * 分类统计数据模型
 * 用于承载分类统计查询的结果
 */
data class CategoryStatsModel(
    /**
     * 分类名称
     */
    val cateName: String,

    /**
     * 总金额
     */
    val amount: Double,

    /**
     * 账单数量
     */
    val count: Int
)

/**
 * 移除父类前缀，仅保留子类名称。
 * - 格式为 "父类-子类" 时返回子类
 * - 否则返回原字符串（去除首尾空格）
 */
fun String.removeParent(): String {
    val trimmed = trim()
    val idx = trimmed.indexOf('-')
    return if (idx in 1 until trimmed.lastIndex) {
        trimmed.substring(idx + 1).trim()
    } else {
        trimmed
    }
}

