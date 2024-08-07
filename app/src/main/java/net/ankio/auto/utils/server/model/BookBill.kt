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

package net.ankio.auto.utils.server.model

import com.google.gson.Gson
import com.google.gson.JsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.Logger
import net.ankio.common.constant.BillType

class BookBill {
    var id: Int? = null
    var amount: Double = 0.0
    var time: Long = 0
    var remark: String? = ""
    var billId: String = ""
    var type: Int = 0
    var book: String = ""
    var category: String = ""
    var accountFrom: String = ""
    var accountTo: String = ""

    fun toJson():String{
        return Gson().toJson(this)
    }
    companion object{


        suspend fun list(type:Int,book:String):Array<BookBill>?{
            val list = AppUtils.getService().sendMsg("bookbill/list",
                hashMapOf(
                    "page" to 0,
                    "size" to 0,
                    "book" to book,
                    "type" to type
                )
            )
            return runCatching {
                Gson().fromJson(list as JsonArray,Array<BookBill>::class.java)
            }.onFailure {
                Logger.e(it.message?:"",it)
            }.getOrNull()
        }


    }
}