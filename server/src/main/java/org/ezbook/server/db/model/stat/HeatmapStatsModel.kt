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

package org.ezbook.server.db.model.stat

/**
 * 消费生物钟统计结果模型
 * - bucketDay: 星期桶（0=周日...6=周六）
 * - bucketHour: 小时桶（00-23）
 * - amount: 聚合金额
 */
data class HeatmapStatsModel(
    /** 星期桶（0-6） */
    val bucketDay: String,
    /** 小时桶（00-23） */
    val bucketHour: String,
    /** 聚合金额 */
    val amount: Double
)
