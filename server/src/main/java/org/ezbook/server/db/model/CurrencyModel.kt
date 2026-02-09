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

package org.ezbook.server.db.model

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

/**
 * 货币信息模型
 *
 * 存储在 BillInfoModel.currency 字段中（JSON 序列化）。
 * 记录账单产生时的汇率快照，确保事后可还原当时金额。
 *
 * @param code 账单原始币种代码（ISO 4217），如 "USD"、"JPY"
 * @param baseCurrency 本位币代码（ISO 4217），如 "CNY"；rate 即相对于此币种
 * @param rate 相对于本位币的汇率：1 单位 code = rate 单位 baseCurrency
 * @param timestamp 汇率获取时间戳（毫秒）
 */
data class CurrencyModel(
    val code: String = "CNY",
    val baseCurrency: String = "CNY",
    val rate: Double = 1.0,
    val timestamp: Long = 0
) {
    companion object {
        private val gson = Gson()

        /**
         * 从 JSON 字符串解析 CurrencyModel
         *
         * 兼容旧格式：如果输入是纯币种代码（如 "CNY"）而非 JSON，
         * 则自动构造 CurrencyModel，rate=1.0（默认同币种）。
         */
        fun fromJson(json: String): CurrencyModel {
            if (json.isBlank()) return CurrencyModel()
            return try {
                gson.fromJson(json, CurrencyModel::class.java)
                    ?: CurrencyModel()
            } catch (_: JsonSyntaxException) {
                // 旧格式：纯币种代码字符串
                CurrencyModel(code = json.uppercase().trim())
            }
        }
    }

    /** 序列化为 JSON 字符串 */
    fun toJson(): String = gson.toJson(this)
}
