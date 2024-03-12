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

package net.ankio.common.model

import net.ankio.common.constant.Currency

/**
 * 自动记账传递给记账软件的数据模型
 */
data class AutoBillModel(
    val type: Int = 0,//账单类型，参考BillType类
    var currency: Currency = Currency.CNY,//货币类型
    val amount: Float = 0.00F,//金额
    var fee: Float = 0.00F,//手续费
    var timeStamp: Long = 0,//时间戳
    var cateName: String = "其他",//分类名称
    var extendData: String = "",//扩展数据
    var bookName: String = "默认账本",//账本名称
    var accountNameFrom: String = "",//转出账户名称
    var accountNameTo: String = "",//转入账户名称
    val remark: String,//备注
    val id: Int = 0,//账单id，自动记账生成
)