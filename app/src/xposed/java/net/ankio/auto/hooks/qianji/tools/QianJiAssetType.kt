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

package net.ankio.auto.hooks.qianji.tools

object QianJiAssetType {
    /**
     * 投资币
     */
    const val STYPE_INVEST_COIN: Int = 46

    /**
     * 投资基金
     */
    const val SType_Debt: Int = 51

    /**
     * 债权人
     */
    const val SType_Debt_Wrapper: Int = 61

    /**
     * 花呗
     */
    const val SType_HuaBei: Int = 22

    /**
     * 借款
     */
    const val SType_Loan: Int = 52

    /**
     * 借款人
     */
    const val SType_Loan_Wrapper: Int = 62

    /**
     * 支付宝
     */
    const val SType_Money_Alipay: Int = 13

    /**
     * 银行卡
     */
    const val SType_Money_Card: Int = 12

    /**
     * 微信
     */
    const val SType_Money_WeiXin: Int = 14

    /**
     * 余额宝
     */
    const val SType_Money_YEB: Int = 103

    /**
     * 信用卡
     */
    const val Type_Credit: Int = 2

    /**
     * 债务
     */
    const val Type_DebtLoan: Int = 5

    /**
     * 债务包装
     */
    const val Type_DebtLoan_Wrapper: Int = 6

    /**
     * 投资
     */
    const val Type_Invest: Int = 4

    /**
     * 现金
     */
    const val Type_Money: Int = 1

    /**
     * 充值
     */
    const val Type_Recharge: Int = 3

}