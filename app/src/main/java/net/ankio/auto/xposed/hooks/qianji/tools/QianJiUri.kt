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

package net.ankio.auto.xposed.hooks.qianji.tools

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ezbook.server.db.model.BillInfoModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object QianJiUri {

    private fun dateToStamp(
        time: String,
        format: String,
    ): Long {
        val simpleDateFormat = SimpleDateFormat(format, Locale.getDefault())
        try {
            val date: Date = checkNotNull(simpleDateFormat.parse(time))
            return date.time
        } catch (e: Throwable) {
            return 0
        }
    }

    fun toAuto(uri: Uri): BillInfoModel {
        val type = uri.getQueryParameter("type")?.toInt() ?: 0
        val amount = uri.getQueryParameter("money")?.toDouble() ?: 0.00
        val time = dateToStamp(uri.getQueryParameter("time")!!, "yyyy-MM-dd HH:mm:ss")
        val remark = uri.getQueryParameter("remark") ?: ""
        val cateName = uri.getQueryParameter("catename") ?: ""
        val bookName = uri.getQueryParameter("bookname") ?: "日常生活"
        val accountNameFrom = uri.getQueryParameter("accountname") ?: ""
        val accountNameTo = uri.getQueryParameter("accountname2") ?: ""
        val fee = uri.getQueryParameter("fee")?.toDouble() ?: 0.00
        val extendData = uri.getQueryParameter("extendData") ?: ""
        val id = uri.getQueryParameter("id")?.toLong() ?: 0L
        val currency = uri.getQueryParameter("currency") ?: "CNY"
        val billInfo = BillInfoModel()
        billInfo.type = QianJiBillType.toAuto(type)
        billInfo.money = amount
        billInfo.time = time
        billInfo.remark = remark
        billInfo.cateName = cateName
        billInfo.bookName = bookName
        billInfo.accountNameFrom = accountNameFrom
        billInfo.accountNameTo = accountNameTo
        billInfo.fee = fee
        billInfo.extendData = extendData
        billInfo.id = id
        billInfo.currency = currency
        return billInfo
    }
}


private fun formatTime(time: Long): String {
    // 时间格式为yyyy-MM-dd HH:mm:ss
    val date = Date(time)
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    return sdf.format(date)

}