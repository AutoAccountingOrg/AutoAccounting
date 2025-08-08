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

package org.ezbook.server.constant

import android.content.Context
import androidx.annotation.StringRes
import org.ezbook.server.R

// 顶层常量与方法，避免在枚举常量初始化阶段访问未初始化的 companion 对象
private const val CURRENCY_ICON_BASE_URL: String = "https://res.qianjiapp.com/currency"
private fun currencyIcon(code: String): String =
    "$CURRENCY_ICON_BASE_URL/${code}.png"

/**
 * 币种常量
 *
 * 说明：
 * - 数据最初依据钱迹接口：https://api.qianjiapp.com/clientweb/currencylist
 * - 为保持简洁与高频使用体验，仅保留常用币种，减少不必要的枚举项与选择列表长度
 */
enum class Currency(
    @StringRes val currencyNameResId: Int,
    val currencyIconUrl: String
) {
    // 人民币（中国）
    CNY(R.string.currency_cny, currencyIcon("cny")),

    // 主流国际货币
    USD(R.string.currency_usd, currencyIcon("usd")),
    EUR(R.string.currency_eur, currencyIcon("eur")),
    JPY(R.string.currency_jpy, currencyIcon("jpy")),
    GBP(R.string.currency_gbp, currencyIcon("gbp")),
    CHF(R.string.currency_chf, currencyIcon("chf")),

    // 常用英语系国家/地区
    AUD(R.string.currency_aud, currencyIcon("aud")),
    CAD(R.string.currency_cad, currencyIcon("cad")),
    NZD(R.string.currency_nzd, currencyIcon("nzd")),

    // 周边及华语地区
    HKD(R.string.currency_hkd, currencyIcon("hkd")),
    TWD(R.string.currency_twd, currencyIcon("twd")),
    MOP(R.string.currency_mop, currencyIcon("mop")),
    KRW(R.string.currency_krw, currencyIcon("krw")),

    // 东南亚/南亚常用
    SGD(R.string.currency_sgd, currencyIcon("sgd")),
    THB(R.string.currency_thb, currencyIcon("thb")),
    MYR(R.string.currency_myr, currencyIcon("myr")),
    IDR(R.string.currency_idr, currencyIcon("idr")),
    VND(R.string.currency_vnd, currencyIcon("vnd")),
    INR(R.string.currency_inr, currencyIcon("inr"));

    /**
     * 获取货币名称
     */
    fun name(context: Context) = context.getString(currencyNameResId)

    /**
     * 获取货币图标 URL（钱迹样式）
     * 形如: https://res.qianjiapp.com/currency/cny.png!
     */
    fun iconUrl(): String = currencyIconUrl

    companion object {
        // 获取所有枚举值的hash
        fun getCurrencyMap(context: Context): HashMap<String, Any> {
            val map = HashMap<String, Any>()
            entries.forEach {
                map[it.name(context)] = it
            }
            return map
        }

    }
}