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

object DefaultData {
    val WECHAT_PACKAGE: String = "com.tencent.mm"
    val SMS_FILTER = listOf(
        "银行", "信用卡", "借记卡", "公积金",
        "元", "￥", "¥", "人民币",
        "消费", "支付", "支出", "转出", "取出", "取款",
        "收入", "转入", "存入", "存款", "退款",
        "还款", "贷款", "借款", "逾期",
        "转账",
        "账户", "余额",
        "交易", "动账", "账单",
    ).joinToString(",")

    val NOTICE_FILTER = listOf(
        "cmb.pb",// 招商银行
        "cn.gov.pbc.dcep",// 数字人民币
        "com.sankuai.meituan",// 美团
        "com.unionpay",// 云闪付

    ).joinToString(",")

    // 钱迹
    val BOOK_APP = "com.mutangtech.qianji"

    val SETTING_ASSET_MANAGER = false
    val SETTING_FEE = false
    val SETTING_BOOK_MANAGER = false
    val SETTING_CURRENCY_MANAGER = false
    val SETTING_DEBT = false
    val SETTING_REIMBURSEMENT = false
    val SETTING_REMIND_BOOK = false


    val LAST_BACKUP_TIME = 0L
    val AUTO_BACKUP = false
    val USE_WEBDAV = false
    val WEBDAV_HOST = ""
    val WEBDAV_USER = ""
    val WEBDAV_PASSWORD = ""
    val DEBUG_MODE = false
    val FLOAT_TIMEOUT_OFF = "10"
    val DEFAULT_BOOK_NAME = "默认账本"

    val EXPENSE_COLOR_RED = 0

    val CATEGORY_SHOW_PARENT = false

    val SEND_ERROR_REPORT = true

    val LOCAL_BACKUP_PATH = ""

    val SHOW_RULE_NAME = true

    val CHECK_RULE_UPDATE = true
    val CHECK_APP_UPDATE = true

    val USE_AI = false
    val AUTO_GROUP = true
    val USE_ROUND_STYLE = false
    val SHOW_SUCCESS_POPUP = true
    val AUTO_ASSET = false
    val AUTO_CREATE_CATEGORY = false
    val SYNC_TYPE = SyncType.WhenOpenApp.name


}