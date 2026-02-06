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

package org.ezbook.server.tools

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import org.ezbook.server.db.model.CurrencyModel
import org.ezbook.server.log.ServerLog
import java.util.concurrent.TimeUnit

/**
 * 汇率服务
 *
 * 职责：
 * - 从 CDN 获取汇率数据
 * - 内存缓存当天汇率，避免重复请求
 * - 构造 CurrencyModel 供账单使用
 *
 * 汇率 API：https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/{base}.json
 * 返回格式：{ "date": "2024-01-15", "{base}": { "usd": 0.139, ... } }
 * 含义：1 单位 base = x 单位 target
 */
object CurrencyService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /** 缓存：baseCurrency → { targetCurrency → rate } */
    private var cachedRates: Map<String, Double>? = null

    /** 缓存对应的本位币 */
    private var cachedBaseCurrency: String? = null

    /** 缓存日期标识，按天失效 */
    private var cachedDate: String? = null

    /**
     * 构造 CurrencyModel
     *
     * @param currencyCode 账单币种代码（如 "USD"）
     * @param baseCurrency 本位币代码（如 "CNY"）
     * @return CurrencyModel，rate 为该币种相对于本位币的汇率
     */
    suspend fun buildCurrencyModel(
        currencyCode: String,
        baseCurrency: String
    ): CurrencyModel {
        val code = currencyCode.uppercase().trim()
        val base = baseCurrency.uppercase().trim()

        // 同币种不需要汇率
        if (code == base) {
            return CurrencyModel(code = code, rate = 1.0, timestamp = System.currentTimeMillis())
        }

        // 获取汇率：1 单位 code = ? 单位 base
        val rate = fetchRate(code, base)
        return CurrencyModel(code = code, rate = rate, timestamp = System.currentTimeMillis())
    }

    /**
     * 获取汇率：1 单位 billCurrency = ? 单位 baseCurrency
     *
     * API 返回的是 1 base = x target 的格式，
     * 所以 1 target = 1/x base，即 rate = 1/apiRate
     *
     * @return 汇率值；获取失败返回 0.0
     */
    private suspend fun fetchRate(billCurrency: String, baseCurrency: String): Double {
        val rates = getRates(baseCurrency)
        val apiRate = rates[billCurrency.lowercase()]
        if (apiRate == null || apiRate == 0.0) {
            ServerLog.e("汇率未找到：$billCurrency -> $baseCurrency，回退CNY")
            // 回退：尝试用 CNY 作为本位币
            if (baseCurrency != "CNY") {
                val cnyRates = getRates("CNY")
                val cnyRate = cnyRates[billCurrency.lowercase()]
                if (cnyRate != null && cnyRate != 0.0) {
                    return 1.0 / cnyRate
                }
            }
            return 0.0
        }
        // API: 1 baseCurrency = apiRate billCurrency
        // 所以: 1 billCurrency = 1/apiRate baseCurrency
        return 1.0 / apiRate
    }

    /**
     * 获取以 baseCurrency 为基准的全部汇率表（带缓存）
     */
    private fun getRates(baseCurrency: String): Map<String, Double> {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())

        // 缓存命中
        if (cachedRates != null &&
            cachedBaseCurrency == baseCurrency &&
            cachedDate == today
        ) {
            return cachedRates!!
        }

        // 从 API 获取
        val rates = fetchRatesFromApi(baseCurrency)
        if (rates.isNotEmpty()) {
            cachedRates = rates
            cachedBaseCurrency = baseCurrency
            cachedDate = today
        }
        return rates
    }

    /**
     * 从 CDN API 获取汇率数据
     */
    private fun fetchRatesFromApi(baseCurrency: String): Map<String, Double> {
        val base = baseCurrency.lowercase()
        val url =
            "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/$base.json"

        return runCatching {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    ServerLog.d("汇率API请求失败：${response.code}")
                    return@runCatching emptyMap()
                }

                val body = response.body?.string() ?: return@runCatching emptyMap()
                val json = gson.fromJson(body, JsonObject::class.java)

                // 解析: { "date": "...", "{base}": { "usd": 0.139, ... } }
                val ratesObj = json.getAsJsonObject(base)
                    ?: return@runCatching emptyMap()

                val result = mutableMapOf<String, Double>()
                ratesObj.entrySet().forEach { (key, value) ->
                    runCatching {
                        result[key] = value.asDouble
                    }
                }
                result
            }
        }.onFailure {
            ServerLog.e("获取汇率失败：${it.message}", it)
        }.getOrDefault(emptyMap())
    }
}
