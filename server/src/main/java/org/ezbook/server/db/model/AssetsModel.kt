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

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ezbook.server.Server
import org.ezbook.server.constant.AssetsType
import org.ezbook.server.constant.Currency

@Entity
class AssetsModel {
    // 账户列表
    @PrimaryKey(autoGenerate = true)
    var id = 0L
    var name: String = "" // 账户名

    /**
     * 这里的图标是url链接或存储的base64图片
     */
    var icon: String = "" // 图标
    var sort = 0
    var type: AssetsType = AssetsType.NORMAL // 账户类型
    var extras: String = "" // 额外信息，例如银行卡的卡号等
    var currency: Currency = Currency.CNY // 货币类型

    companion object {
        /**
         * 根据条件查询
         * @return 规则列表
         */
        suspend fun list(): List<AssetsModel> = withContext(
            Dispatchers.IO
        ) {
            val response = Server.request("assets/list")
            val json = Gson().fromJson(response, JsonObject::class.java)

            runCatching {
                Gson().fromJson(
                    json.getAsJsonArray("data"),
                    Array<AssetsModel>::class.java
                ).toList()
            }.getOrNull() ?: emptyList()
        }

        suspend fun put(data: ArrayList<AssetsModel>, md5: String) {
            val json = Gson().toJson(data)
            Server.request("assets/put?md5=$md5", json)
        }

        suspend fun getByName(name: String): AssetsModel? {
            val response = Server.request("assets/get?name=${Uri.encode(name)}")
            val json = Gson().fromJson(response, JsonObject::class.java)
            return runCatching {
                Gson().fromJson(
                    json.getAsJsonObject("data"),
                    AssetsModel::class.java
                )
            }.getOrNull()
        }
    }
}
