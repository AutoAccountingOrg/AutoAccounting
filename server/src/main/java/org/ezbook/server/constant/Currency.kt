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
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.ezbook.server.R

/**
 * 此处的币种依据钱迹：https://api.qianjiapp.com/clientweb/currencylist
 */
enum class Currency(
    @StringRes val currencyNameResId: Int,
    @DrawableRes val currencyIconResId: Int
) {
    CNY(R.string.currency_cny, R.drawable.currency_cny),
    AED(R.string.currency_aed, R.drawable.currency_aed),
    AFN(R.string.currency_afn, R.drawable.currency_afn),
    ALL(R.string.currency_all, R.drawable.currency_all),
    AMD(R.string.currency_amd, R.drawable.currency_amd),
    AOA(R.string.currency_aoa, R.drawable.currency_aoa),
    ARS(R.string.currency_ars, R.drawable.currency_ars),
    AUD(R.string.currency_aud, R.drawable.currency_aud),
    AZN(R.string.currency_azn, R.drawable.currency_azn),
    BAM(R.string.currency_bam, R.drawable.currency_bam),
    BBD(R.string.currency_bbd, R.drawable.currency_bbd),
    BDT(R.string.currency_bdt, R.drawable.currency_bdt),
    BGN(R.string.currency_bgn, R.drawable.currency_bgn),
    BHD(R.string.currency_bhd, R.drawable.currency_bhd),
    BIF(R.string.currency_bif, R.drawable.currency_bif),
    BMD(R.string.currency_bmd, R.drawable.currency_bmd),
    BND(R.string.currency_bnd, R.drawable.currency_bnd),
    BOB(R.string.currency_bob, R.drawable.currency_bob),
    BRL(R.string.currency_brl, R.drawable.currency_brl),
    BSD(R.string.currency_bsd, R.drawable.currency_bsd),
    BTN(R.string.currency_btn, R.drawable.currency_btn),
    BWP(R.string.currency_bwp, R.drawable.currency_bwp),
    BYN(R.string.currency_byn, R.drawable.currency_byn),
    BZD(R.string.currency_bzd, R.drawable.currency_bzd),
    CAD(R.string.currency_cad, R.drawable.currency_cad),
    CDF(R.string.currency_cdf, R.drawable.currency_cdf),
    CHF(R.string.currency_chf, R.drawable.currency_chf),
    CLP(R.string.currency_clp, R.drawable.currency_clp),
    COP(R.string.currency_cop, R.drawable.currency_cop),
    CRC(R.string.currency_crc, R.drawable.currency_crc),
    CUP(R.string.currency_cup, R.drawable.currency_cup),
    CVE(R.string.currency_cve, R.drawable.currency_cve),
    CZK(R.string.currency_czk, R.drawable.currency_czk),
    DJF(R.string.currency_djf, R.drawable.currency_djf),
    DKK(R.string.currency_dkk, R.drawable.currency_dkk),
    DOP(R.string.currency_dop, R.drawable.currency_dop),
    DZD(R.string.currency_dzd, R.drawable.currency_dzd),
    EGP(R.string.currency_egp, R.drawable.currency_egp),
    ERN(R.string.currency_ern, R.drawable.currency_ern),
    ETB(R.string.currency_etb, R.drawable.currency_etb),
    EUR(R.string.currency_eur, R.drawable.currency_eur),
    FJD(R.string.currency_fjd, R.drawable.currency_fjd),
    GBP(R.string.currency_gbp, R.drawable.currency_gbp),
    GEL(R.string.currency_gel, R.drawable.currency_gel),
    GHS(R.string.currency_ghs, R.drawable.currency_ghs),
    GMD(R.string.currency_gmd, R.drawable.currency_gmd),
    GNF(R.string.currency_gnf, R.drawable.currency_gnf),
    GTQ(R.string.currency_gtq, R.drawable.currency_gtq),
    GYD(R.string.currency_gyd, R.drawable.currency_gyd),
    HKD(R.string.currency_hkd, R.drawable.currency_hkd),
    HNL(R.string.currency_hnl, R.drawable.currency_hnl),
    HRK(R.string.currency_hrk, R.drawable.currency_hrk),
    HTG(R.string.currency_htg, R.drawable.currency_htg),
    HUF(R.string.currency_huf, R.drawable.currency_huf),
    IDR(R.string.currency_idr, R.drawable.currency_idr),
    ILS(R.string.currency_ils, R.drawable.currency_ils),
    INR(R.string.currency_inr, R.drawable.currency_inr),
    IQD(R.string.currency_iqd, R.drawable.currency_iqd),
    IRR(R.string.currency_irr, R.drawable.currency_irr),
    ISK(R.string.currency_isk, R.drawable.currency_isk),
    JMD(R.string.currency_jmd, R.drawable.currency_jmd),
    JOD(R.string.currency_jod, R.drawable.currency_jod),
    JPY(R.string.currency_jpy, R.drawable.currency_jpy),
    KES(R.string.currency_kes, R.drawable.currency_kes),
    KGS(R.string.currency_kgs, R.drawable.currency_kgs),
    KHR(R.string.currency_khr, R.drawable.currency_khr),
    KMF(R.string.currency_kmf, R.drawable.currency_kmf),
    KRW(R.string.currency_krw, R.drawable.currency_krw),
    KWD(R.string.currency_kwd, R.drawable.currency_kwd),
    KYD(R.string.currency_kyd, R.drawable.currency_kyd),
    KZT(R.string.currency_kzt, R.drawable.currency_kzt),
    LAK(R.string.currency_lak, R.drawable.currency_lak),
    LBP(R.string.currency_lbp, R.drawable.currency_lbp),
    LKR(R.string.currency_lkr, R.drawable.currency_lkr),
    LRD(R.string.currency_lrd, R.drawable.currency_lrd),
    LSL(R.string.currency_lsl, R.drawable.currency_lsl),
    LYD(R.string.currency_lyd, R.drawable.currency_lyd),
    MAD(R.string.currency_mad, R.drawable.currency_mad),
    MDL(R.string.currency_mdl, R.drawable.currency_mdl),
    MKD(R.string.currency_mkd, R.drawable.currency_mkd),
    MMK(R.string.currency_mmk, R.drawable.currency_mmk),
    MNT(R.string.currency_mnt, R.drawable.currency_mnt),
    MOP(R.string.currency_mop, R.drawable.currency_mop),
    MRU(R.string.currency_mru, R.drawable.currency_mru),
    MUR(R.string.currency_mur, R.drawable.currency_mur),
    MVR(R.string.currency_mvr, R.drawable.currency_mvr),
    MWK(R.string.currency_mwk, R.drawable.currency_mwk),
    MXN(R.string.currency_mxn, R.drawable.currency_mxn),
    MYR(R.string.currency_myr, R.drawable.currency_myr),
    MZN(R.string.currency_mzn, R.drawable.currency_mzn),
    NAD(R.string.currency_nad, R.drawable.currency_nad),
    NGN(R.string.currency_ngn, R.drawable.currency_ngn),
    NIO(R.string.currency_nio, R.drawable.currency_nio),
    NOK(R.string.currency_nok, R.drawable.currency_nok),
    NPR(R.string.currency_npr, R.drawable.currency_npr),
    NZD(R.string.currency_nzd, R.drawable.currency_nzd),
    OMR(R.string.currency_omr, R.drawable.currency_omr),
    PAB(R.string.currency_pab, R.drawable.currency_pab),
    PEN(R.string.currency_pen, R.drawable.currency_pen),
    PGK(R.string.currency_pgk, R.drawable.currency_pgk),
    PHP(R.string.currency_php, R.drawable.currency_php),
    PKR(R.string.currency_pkr, R.drawable.currency_pkr),
    PLN(R.string.currency_pln, R.drawable.currency_pln),
    PYG(R.string.currency_pyg, R.drawable.currency_pyg),
    QAR(R.string.currency_qar, R.drawable.currency_qar),
    RON(R.string.currency_ron, R.drawable.currency_ron),
    RSD(R.string.currency_rsd, R.drawable.currency_rsd),
    RUB(R.string.currency_rub, R.drawable.currency_rub),
    RWF(R.string.currency_rwf, R.drawable.currency_rwf),
    SAR(R.string.currency_sar, R.drawable.currency_sar),
    SBD(R.string.currency_sbd, R.drawable.currency_sbd),
    SCR(R.string.currency_scr, R.drawable.currency_scr),
    SDG(R.string.currency_sdg, R.drawable.currency_sdg),
    SEK(R.string.currency_sek, R.drawable.currency_sek),
    SGD(R.string.currency_sgd, R.drawable.currency_sgd),
    SLE(R.string.currency_sle, R.drawable.currency_sle),
    SLL(R.string.currency_sll, R.drawable.currency_sll),
    SOS(R.string.currency_sos, R.drawable.currency_sos),
    SRD(R.string.currency_srd, R.drawable.currency_srd),
    SSP(R.string.currency_ssp, R.drawable.currency_ssp),
    STN(R.string.currency_stn, R.drawable.currency_stn),
    SYP(R.string.currency_syp, R.drawable.currency_syp),
    SZL(R.string.currency_szl, R.drawable.currency_szl),
    THB(R.string.currency_thb, R.drawable.currency_thb),
    TJS(R.string.currency_tjs, R.drawable.currency_tjs),
    TMT(R.string.currency_tmt, R.drawable.currency_tmt),
    TND(R.string.currency_tnd, R.drawable.currency_tnd),
    TOP(R.string.currency_top, R.drawable.currency_top),
    TRY(R.string.currency_try, R.drawable.currency_try),
    TTD(R.string.currency_ttd, R.drawable.currency_ttd),
    TVD(R.string.currency_tvd, R.drawable.currency_tvd),
    TWD(R.string.currency_twd, R.drawable.currency_twd),
    TZS(R.string.currency_tzs, R.drawable.currency_tzs),
    UAH(R.string.currency_uah, R.drawable.currency_uah),
    UGX(R.string.currency_ugx, R.drawable.currency_ugx),
    USD(R.string.currency_usd, R.drawable.currency_usd),
    UYU(R.string.currency_uyu, R.drawable.currency_uyu),
    UZS(R.string.currency_uzs, R.drawable.currency_uzs),
    VES(R.string.currency_ves, R.drawable.currency_ves),
    VND(R.string.currency_vnd, R.drawable.currency_vnd),
    VUV(R.string.currency_vuv, R.drawable.currency_vuv),
    WST(R.string.currency_wst, R.drawable.currency_wst),
    XAF(R.string.currency_xaf, R.drawable.currency_xaf),
    XCD(R.string.currency_xcd, R.drawable.currency_xcd),
    XDR(R.string.currency_xdr, R.drawable.currency_xdr),
    XOF(R.string.currency_xof, R.drawable.currency_xof),
    XPF(R.string.currency_xpf, R.drawable.currency_xpf),
    YER(R.string.currency_yer, R.drawable.currency_yer),
    ZAR(R.string.currency_zar, R.drawable.currency_zar),
    ZMW(R.string.currency_zmw, R.drawable.currency_zmw),
    ZWL(R.string.currency_zwl, R.drawable.currency_zwl);

    /**
     * 获取货币名称
     */
    fun name(context: Context) = context.getString(currencyNameResId)

    /**
     * 获取货币图标
     */
    fun icon(context: Context) = context.getDrawable(currencyIconResId)

    companion object {
        //获取所有枚举值的hash
        fun getCurrencyMap(context: Context): HashMap<String, Any> {
            val map = HashMap<String, Any>()
            entries.forEach {
                map[it.name(context)] = it
            }
            return map
        }
    }
}