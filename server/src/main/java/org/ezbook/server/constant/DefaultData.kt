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
    val SMS_FILTER = listOf(
        "银行", "信用卡",  "借记卡",  "公积金",
        "元", "￥",  "¥",  "人民币",
        "消费", "支付",  "支出",  "转出", "取出", "取款",
        "收入", "转入",  "存入", "存款", "退款",
        "还款", "贷款", "借款", "逾期",
        "转账",
        "账户", "余额",
        "交易", "动账", "账单",
    ).joinToString(",")

    val NOTICE_FILTER = listOf(
        "",//银行

    ).joinToString(",")
}