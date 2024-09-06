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

package org.ezbook.server.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ezbook.server.Server
import org.ezbook.server.constant.BillType


/**
 * 记账软件里面的账单
 * 主要是 需要报销的、欠别人钱的、别人欠自己钱的
 */
@Entity
class BookBillModel {




    @PrimaryKey(autoGenerate = true)
    var id = 0L
    /**
     * 账单类型
     * 1. 支出报销(BillType.ExpendRepayment)
     * 2. 借出(BillType.ExpendLending)
     * 3. 借入(BillType.IncomeLending)
     */
    var type: BillType = BillType.ExpendRepayment

    var money: Double = 0.00

    var time: Long = 0

    var remark: String = ""

    var remoteId: String = ""

    var remoteBookId: String = ""

    var category: String = ""

    companion object{
        suspend fun list(type: BillType) : List<BookNameModel> = withContext(
            Dispatchers.IO) {
            val response = Server.request("bill/book/list?type=${type.name}")
            val json = Gson().fromJson(response, JsonObject::class.java)

            runCatching { Gson().fromJson(json.getAsJsonArray("data"), Array<BookNameModel>::class.java).toList() }.getOrNull() ?: emptyList()
        }

        //TODO
        fun put(bills: ArrayList<BookBillModel>, md5: String, expendRepayment: BillType) {

        }

    }
}