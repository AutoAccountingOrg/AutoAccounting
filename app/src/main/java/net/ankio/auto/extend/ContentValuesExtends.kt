/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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

package net.ankio.auto.extend

import android.content.ContentValues
import net.ankio.auto.constant.BillType
import net.ankio.auto.constant.Currency
import net.ankio.auto.database.table.BillInfo

fun ContentValues.toBillInfo(): BillInfo {
    val billInfo = BillInfo()
    billInfo.type = BillType.valueOf(getAsString("type"))
    billInfo.currency = Currency.valueOf(getAsString("currency"))
    billInfo.money = getAsFloat("money")
    billInfo.fee = getAsFloat("fee")
    billInfo.timeStamp = getAsLong("timeStamp")
    billInfo.shopName = getAsString("shopName")
    billInfo.shopItem = getAsString("shopItem")
    billInfo.cateName = getAsString("cateName")
    billInfo.reimbursement = getAsBoolean("reimbursement")
    billInfo.bookName = getAsString("bookName")
    billInfo.accountNameTo = getAsString("accountNameTo")
    billInfo.channel = getAsString("channel")
    billInfo.accountNameFrom = getAsString("accountNameFrom")
    billInfo.accountNameFrom = getAsString("accountNameFrom")
    billInfo.accountNameFrom = getAsString("accountNameFrom")
    billInfo.accountNameFrom = getAsString("accountNameFrom")
    billInfo.accountNameFrom = getAsString("accountNameFrom")
    billInfo.accountNameFrom = getAsString("accountNameFrom")
    billInfo.accountNameFrom = getAsString("accountNameFrom")
    return billInfo
}