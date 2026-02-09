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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.ezbook.server.db.model.CurrencyModel
import org.ezbook.server.log.ServerLog
import java.util.concurrent.TimeUnit

/**
 * 汇率服务
 *
 * 职责：
 * - 从 CDN 获取汇率数据（IO 线程安全）
 * - 缓存当天的 CurrencyModel 表，避免重复请求和重复换算
 * - 提供单个/全量 CurrencyModel 查询
 *
 * 所有公开方法均为 suspend，内部自行切 IO 线程，调用方无需关心线程。
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

    /** 缓存：币种代码(大写) → CurrencyModel（rate 含义：1 单位该币种 = rate 单位本位币） */
    private var cachedModels: Map<String, CurrencyModel>? = null

    /** 缓存对应的本位币 */
    private var cachedBaseCurrency: String? = null

    /** 缓存日期标识，按天失效 */
    private var cachedDate: String? = null

    /**
     * 获取所有币种的 CurrencyModel 表（带缓存，按天失效）
     *
     * @param baseCurrency 本位币代码（如 "CNY"）
     * @return Map<币种代码(大写), CurrencyModel>；获取失败返回空 Map
     */
    suspend fun getModels(baseCurrency: String): Map<String, CurrencyModel> {
        val base = baseCurrency.uppercase().trim()
        val today = todayString()

        // 缓存命中（纯内存读取，无需切线程）
        cachedModels?.let { models ->
            if (cachedBaseCurrency == base && cachedDate == today) {
                ServerLog.d("CurrencyService cache hit | base=$base | date=$today | count=${models.size}")
                return models
            }
        }

        // 从 API 构建并缓存（网络 IO）
        ServerLog.d("CurrencyService cache miss, fetching from API | base=$base")
        val models = withContext(Dispatchers.IO) {
            buildModelsFromApi(base)
        }
        if (models.isNotEmpty()) {
            cachedModels = models
            cachedBaseCurrency = base
            cachedDate = today
            ServerLog.d("CurrencyService cached ${models.size} models | base=$base | date=$today")
        } else {
            ServerLog.e("CurrencyService fetch returned empty | base=$base")
        }
        return models
    }

    /**
     * 获取单个币种的 CurrencyModel
     *
     * @param currencyCode 账单币种代码（如 "USD"）
     * @param baseCurrency 本位币代码（如 "CNY"）
     * @return CurrencyModel，rate 为该币种相对于本位币的汇率；同币种 rate=1.0
     */
    suspend fun getModel(currencyCode: String, baseCurrency: String): CurrencyModel {
        val code = currencyCode.uppercase().trim()
        val base = baseCurrency.uppercase().trim()
        val now = System.currentTimeMillis()

        // 同币种
        if (code == base) {
            ServerLog.d("CurrencyService getModel: same currency $code, rate=1.0")
            return CurrencyModel(code = code, baseCurrency = base, rate = 1.0, timestamp = now)
        }

        // 从缓存/API获取
        val models = getModels(base)
        models[code]?.let {
            ServerLog.d("CurrencyService getModel: $code -> $base | rate=${it.rate}")
            return it
        }

        // 回退：尝试用 CNY 作为本位币
        ServerLog.e("CurrencyService rate not found: $code -> $base, fallback to CNY")
        if (base != "CNY") {
            val cnyModels = getModels("CNY")
            cnyModels[code]?.let {
                ServerLog.d("CurrencyService fallback success: $code -> CNY | rate=${it.rate}")
                return it
            }
        }

        ServerLog.e("CurrencyService fallback also failed: $code -> CNY, returning rate=0.0")
        return CurrencyModel(code = code, baseCurrency = base, rate = 0.0, timestamp = now)
    }

    /**
     * 向后兼容：构造 CurrencyModel（供 BillService 调用）
     */
    suspend fun buildCurrencyModel(
        currencyCode: String,
        baseCurrency: String
    ): CurrencyModel = getModel(currencyCode, baseCurrency)

    /**
     * 从 API 获取原始汇率并转换为 CurrencyModel 表
     * 调用方应确保在 IO 线程执行
     */
    private fun buildModelsFromApi(baseCurrency: String): Map<String, CurrencyModel> {
        val rawRates = fetchRatesFromApi(baseCurrency)
        if (rawRates.isEmpty()) {
            ServerLog.e("CurrencyService buildModels: raw rates empty for $baseCurrency")
            return emptyMap()
        }

        val now = System.currentTimeMillis()
        val result = mutableMapOf<String, CurrencyModel>()

        rawRates.forEach { (code, apiRate) ->
            // API: 1 baseCurrency = apiRate targetCurrency
            // 转换: 1 targetCurrency = 1/apiRate baseCurrency
            if (apiRate > 0) {
                val upperCode = code.uppercase()
                result[upperCode] = CurrencyModel(
                    code = upperCode,
                    baseCurrency = baseCurrency,
                    rate = 1.0 / apiRate,
                    timestamp = now
                )
            }
        }

        // 本位币自身
        result[baseCurrency] = CurrencyModel(
            code = baseCurrency,
            baseCurrency = baseCurrency,
            rate = 1.0,
            timestamp = now
        )

        ServerLog.d("CurrencyService built ${result.size} models from API | base=$baseCurrency")
        return result
    }

    /**
     * 从 CDN API 获取原始汇率数据
     */
    private fun fetchRatesFromApi(baseCurrency: String): Map<String, Double> {
        val base = baseCurrency.lowercase()
        val url =
            "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/$base.json"

        ServerLog.d("CurrencyService fetching: $url")

        return runCatching {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    ServerLog.e("CurrencyService API failed: HTTP ${response.code}")
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

                ServerLog.d("CurrencyService API success: ${result.size} rates parsed")
                result
            }
        }.onFailure {
            ServerLog.e("CurrencyService fetch error: ${it.message}", it)
        }.getOrDefault(emptyMap())
    }

    /** 今天的日期字符串，用于缓存失效判断 */
    private fun todayString(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
}
