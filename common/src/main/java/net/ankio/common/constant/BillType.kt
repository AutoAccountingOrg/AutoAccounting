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

enum class BillType(val value: Int)  {
    Expend(0),//支出
    ExpendReimbursement( 4),//支出（记作报销）
    ExpendLending(5),//支出（借出）
    ExpendRepayment(6),//支出（还款销账）


    Income(1),//收入
    IncomeLending(7),//收入（借入）
    IncomeRepayment(8),//收入（还款销账）
    IncomeReimbursement( 9),//收入（报销）
    Transfer(2);//转账
    fun toInt(): Int = value

    companion object {
        // 将整数转换为枚举值
        fun fromInt(value: Int): BillType {
            return entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException("Invalid value: $value")
        }
    }
}