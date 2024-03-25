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

package net.ankio.common.constant

enum class AssetType {
    DEBT_EXPAND,//债务支出账户（借钱给别人）
    DEBT_INCOME,//债务收入账户（借别人钱）
    CASH, // 现金
    BANK_CARD, // 银行卡
    APP, // 软件账户，例如支付宝余额、微信余额、京东余额这种
    DIGITAL_CASH, // 数字货币，如比特币这些
    CREDIT, // 信用账户，花呗、白条、信用卡等
    INVEST, // 理财账户，股票、基金、余额宝、余利宝等
    RECHARGE // 储值账户，例如地铁卡、手机话费、游戏点卡等
}
