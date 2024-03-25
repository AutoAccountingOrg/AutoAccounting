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

data class BillModel(
    var amount: Double = 0.0,//金额
    var time: Long = 0,//时间
    var remark: String = "",//备注
    var id: String = "",//账单id，自动记账进行报销、销账的时候需要用到
    var type: Int = 0,//账单类型，只有 0 支出 1 收入，（包括报销、债务
    var book: String = "",//账本名称
    var category: String = "",//分类名称
    var accountFrom: String = "",//转出账户名称
    var accountTo: String = "",//转入账户名称
){
    override fun toString(): String {
       return "BillModel(amount=$amount, time=$time, remark='$remark', id='$id', type=$type, book='$book', category='$category', accountFrom='$accountFrom', accountTo='$accountTo')"
    }
}
