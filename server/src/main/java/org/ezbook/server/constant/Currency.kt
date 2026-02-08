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
 * 币种常量 —— 完整列表
 *
 * 说明：
 * - 数据依据钱迹接口：https://api.qianjiapp.com/clientweb/currencylist
 * - 包含所有可用币种；用户通过设置页面勾选常用币种，下拉选择器仅展示常用项
 */
enum class Currency(
    @StringRes val currencyNameResId: Int,
    val currencyIconUrl: String
) {
    // 人民币（中国）
    CNY(R.string.currency_cny, currencyIcon("cny")),

    // ---- A ----
    AED(R.string.currency_aed, currencyIcon("aed")),
    AFN(R.string.currency_afn, currencyIcon("afn")),
    ALL(R.string.currency_all, currencyIcon("all")),
    AMD(R.string.currency_amd, currencyIcon("amd")),
    AOA(R.string.currency_aoa, currencyIcon("aoa")),
    ARS(R.string.currency_ars, currencyIcon("ars")),
    AUD(R.string.currency_aud, currencyIcon("aud")),
    AZN(R.string.currency_azn, currencyIcon("azn")),

    // ---- B ----
    BAM(R.string.currency_bam, currencyIcon("bam")),
    BBD(R.string.currency_bbd, currencyIcon("bbd")),
    BDT(R.string.currency_bdt, currencyIcon("bdt")),
    BGN(R.string.currency_bgn, currencyIcon("bgn")),
    BHD(R.string.currency_bhd, currencyIcon("bhd")),
    BIF(R.string.currency_bif, currencyIcon("bif")),
    BMD(R.string.currency_bmd, currencyIcon("bmd")),
    BND(R.string.currency_bnd, currencyIcon("bnd")),
    BOB(R.string.currency_bob, currencyIcon("bob")),
    BRL(R.string.currency_brl, currencyIcon("brl")),
    BSD(R.string.currency_bsd, currencyIcon("bsd")),
    BTN(R.string.currency_btn, currencyIcon("btn")),
    BWP(R.string.currency_bwp, currencyIcon("bwp")),
    BYN(R.string.currency_byn, currencyIcon("byn")),
    BZD(R.string.currency_bzd, currencyIcon("bzd")),

    // ---- C ----
    CAD(R.string.currency_cad, currencyIcon("cad")),
    CDF(R.string.currency_cdf, currencyIcon("cdf")),
    CHF(R.string.currency_chf, currencyIcon("chf")),
    CLP(R.string.currency_clp, currencyIcon("clp")),
    COP(R.string.currency_cop, currencyIcon("cop")),
    CRC(R.string.currency_crc, currencyIcon("crc")),
    CUP(R.string.currency_cup, currencyIcon("cup")),
    CVE(R.string.currency_cve, currencyIcon("cve")),
    CZK(R.string.currency_czk, currencyIcon("czk")),

    // ---- D ----
    DJF(R.string.currency_djf, currencyIcon("djf")),
    DKK(R.string.currency_dkk, currencyIcon("dkk")),
    DOP(R.string.currency_dop, currencyIcon("dop")),
    DZD(R.string.currency_dzd, currencyIcon("dzd")),

    // ---- E ----
    EGP(R.string.currency_egp, currencyIcon("egp")),
    ERN(R.string.currency_ern, currencyIcon("ern")),
    ETB(R.string.currency_etb, currencyIcon("etb")),
    EUR(R.string.currency_eur, currencyIcon("eur")),

    // ---- F ----
    FJD(R.string.currency_fjd, currencyIcon("fjd")),

    // ---- G ----
    GBP(R.string.currency_gbp, currencyIcon("gbp")),
    GEL(R.string.currency_gel, currencyIcon("gel")),
    GHS(R.string.currency_ghs, currencyIcon("ghs")),
    GMD(R.string.currency_gmd, currencyIcon("gmd")),
    GNF(R.string.currency_gnf, currencyIcon("gnf")),
    GTQ(R.string.currency_gtq, currencyIcon("gtq")),
    GYD(R.string.currency_gyd, currencyIcon("gyd")),

    // ---- H ----
    HKD(R.string.currency_hkd, currencyIcon("hkd")),
    HNL(R.string.currency_hnl, currencyIcon("hnl")),
    HRK(R.string.currency_hrk, currencyIcon("hrk")),
    HTG(R.string.currency_htg, currencyIcon("htg")),
    HUF(R.string.currency_huf, currencyIcon("huf")),

    // ---- I ----
    IDR(R.string.currency_idr, currencyIcon("idr")),
    ILS(R.string.currency_ils, currencyIcon("ils")),
    INR(R.string.currency_inr, currencyIcon("inr")),
    IQD(R.string.currency_iqd, currencyIcon("iqd")),
    IRR(R.string.currency_irr, currencyIcon("irr")),
    ISK(R.string.currency_isk, currencyIcon("isk")),

    // ---- J ----
    JMD(R.string.currency_jmd, currencyIcon("jmd")),
    JOD(R.string.currency_jod, currencyIcon("jod")),
    JPY(R.string.currency_jpy, currencyIcon("jpy")),

    // ---- K ----
    KES(R.string.currency_kes, currencyIcon("kes")),
    KGS(R.string.currency_kgs, currencyIcon("kgs")),
    KHR(R.string.currency_khr, currencyIcon("khr")),
    KMF(R.string.currency_kmf, currencyIcon("kmf")),
    KRW(R.string.currency_krw, currencyIcon("krw")),
    KWD(R.string.currency_kwd, currencyIcon("kwd")),
    KYD(R.string.currency_kyd, currencyIcon("kyd")),
    KZT(R.string.currency_kzt, currencyIcon("kzt")),

    // ---- L ----
    LAK(R.string.currency_lak, currencyIcon("lak")),
    LBP(R.string.currency_lbp, currencyIcon("lbp")),
    LKR(R.string.currency_lkr, currencyIcon("lkr")),
    LRD(R.string.currency_lrd, currencyIcon("lrd")),
    LSL(R.string.currency_lsl, currencyIcon("lsl")),
    LYD(R.string.currency_lyd, currencyIcon("lyd")),

    // ---- M ----
    MAD(R.string.currency_mad, currencyIcon("mad")),
    MDL(R.string.currency_mdl, currencyIcon("mdl")),
    MKD(R.string.currency_mkd, currencyIcon("mkd")),
    MMK(R.string.currency_mmk, currencyIcon("mmk")),
    MNT(R.string.currency_mnt, currencyIcon("mnt")),
    MOP(R.string.currency_mop, currencyIcon("mop")),
    MRU(R.string.currency_mru, currencyIcon("mru")),
    MUR(R.string.currency_mur, currencyIcon("mur")),
    MVR(R.string.currency_mvr, currencyIcon("mvr")),
    MWK(R.string.currency_mwk, currencyIcon("mwk")),
    MXN(R.string.currency_mxn, currencyIcon("mxn")),
    MYR(R.string.currency_myr, currencyIcon("myr")),
    MZN(R.string.currency_mzn, currencyIcon("mzn")),

    // ---- N ----
    NAD(R.string.currency_nad, currencyIcon("nad")),
    NGN(R.string.currency_ngn, currencyIcon("ngn")),
    NIO(R.string.currency_nio, currencyIcon("nio")),
    NOK(R.string.currency_nok, currencyIcon("nok")),
    NPR(R.string.currency_npr, currencyIcon("npr")),
    NZD(R.string.currency_nzd, currencyIcon("nzd")),

    // ---- O ----
    OMR(R.string.currency_omr, currencyIcon("omr")),

    // ---- P ----
    PAB(R.string.currency_pab, currencyIcon("pab")),
    PEN(R.string.currency_pen, currencyIcon("pen")),
    PGK(R.string.currency_pgk, currencyIcon("pgk")),
    PHP(R.string.currency_php, currencyIcon("php")),
    PKR(R.string.currency_pkr, currencyIcon("pkr")),
    PLN(R.string.currency_pln, currencyIcon("pln")),
    PYG(R.string.currency_pyg, currencyIcon("pyg")),

    // ---- Q ----
    QAR(R.string.currency_qar, currencyIcon("qar")),

    // ---- R ----
    RON(R.string.currency_ron, currencyIcon("ron")),
    RSD(R.string.currency_rsd, currencyIcon("rsd")),
    RUB(R.string.currency_rub, currencyIcon("rub")),
    RWF(R.string.currency_rwf, currencyIcon("rwf")),

    // ---- S ----
    SAR(R.string.currency_sar, currencyIcon("sar")),
    SBD(R.string.currency_sbd, currencyIcon("sbd")),
    SCR(R.string.currency_scr, currencyIcon("scr")),
    SDG(R.string.currency_sdg, currencyIcon("sdg")),
    SEK(R.string.currency_sek, currencyIcon("sek")),
    SGD(R.string.currency_sgd, currencyIcon("sgd")),
    SLE(R.string.currency_sle, currencyIcon("sle")),
    SLL(R.string.currency_sll, currencyIcon("sll")),
    SOS(R.string.currency_sos, currencyIcon("sos")),
    SRD(R.string.currency_srd, currencyIcon("srd")),
    SSP(R.string.currency_ssp, currencyIcon("ssp")),
    STN(R.string.currency_stn, currencyIcon("stn")),
    SYP(R.string.currency_syp, currencyIcon("syp")),
    SZL(R.string.currency_szl, currencyIcon("szl")),

    // ---- T ----
    THB(R.string.currency_thb, currencyIcon("thb")),
    TJS(R.string.currency_tjs, currencyIcon("tjs")),
    TMT(R.string.currency_tmt, currencyIcon("tmt")),
    TND(R.string.currency_tnd, currencyIcon("tnd")),
    TOP(R.string.currency_top, currencyIcon("top")),
    TRY(R.string.currency_try, currencyIcon("try")),
    TTD(R.string.currency_ttd, currencyIcon("ttd")),
    TVD(R.string.currency_tvd, currencyIcon("tvd")),
    TWD(R.string.currency_twd, currencyIcon("twd")),
    TZS(R.string.currency_tzs, currencyIcon("tzs")),

    // ---- U ----
    UAH(R.string.currency_uah, currencyIcon("uah")),
    UGX(R.string.currency_ugx, currencyIcon("ugx")),
    USD(R.string.currency_usd, currencyIcon("usd")),
    UYU(R.string.currency_uyu, currencyIcon("uyu")),
    UZS(R.string.currency_uzs, currencyIcon("uzs")),

    // ---- V ----
    VES(R.string.currency_ves, currencyIcon("ves")),
    VND(R.string.currency_vnd, currencyIcon("vnd")),
    VUV(R.string.currency_vuv, currencyIcon("vuv")),

    // ---- W ----
    WST(R.string.currency_wst, currencyIcon("wst")),

    // ---- X ----
    XAF(R.string.currency_xaf, currencyIcon("xaf")),
    XCD(R.string.currency_xcd, currencyIcon("xcd")),
    XDR(R.string.currency_xdr, currencyIcon("xdr")),
    XOF(R.string.currency_xof, currencyIcon("xof")),
    XPF(R.string.currency_xpf, currencyIcon("xpf")),

    // ---- Y ----
    YER(R.string.currency_yer, currencyIcon("yer")),

    // ---- Z ----
    ZAR(R.string.currency_zar, currencyIcon("zar")),
    ZMW(R.string.currency_zmw, currencyIcon("zmw")),
    ZWL(R.string.currency_zwl, currencyIcon("zwl"));

    /**
     * 获取货币名称
     */
    fun name(context: Context) = context.getString(currencyNameResId)

    /**
     * 获取货币图标 URL（钱迹样式）
     * 形如: https://res.qianjiapp.com/currency/cny.png
     */
    fun iconUrl(): String = currencyIconUrl

    companion object {
        /**
         * 默认常用币种代码集合（与旧版枚举保持一致，确保向后兼容）
         */
        val DEFAULT_SELECTED_CODES: Set<String> = setOf(
            "CNY", "USD", "EUR", "JPY", "GBP", "CHF",
            "AUD", "CAD", "NZD",
            "HKD", "TWD", "MOP", "KRW",
            "SGD", "THB", "MYR", "IDR", "VND", "INR"
        )

        /**
         * 获取所有枚举值的 hash（名称 -> 枚举）
         */
        fun getCurrencyMap(context: Context): HashMap<String, Any> {
            val map = HashMap<String, Any>()
            entries.forEach {
                map[it.name(context)] = it
            }
            return map
        }

        /**
         * 根据用户选中的币种代码过滤出常用币种列表
         * @param selectedCodes 用户选中的币种代码集合
         * @return 过滤后的币种列表，保持枚举定义顺序
         */
        fun selectedEntries(selectedCodes: Set<String>): List<Currency> =
            entries.filter { it.name in selectedCodes }
    }
}
