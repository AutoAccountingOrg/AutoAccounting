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
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ezbook.server.Server

@Entity
class RuleModel {
    @PrimaryKey(autoGenerate = true)
    var id = 0
    // 是哪个App
    var app = ""
    // 规则类型 通知还是数据
    var type = ""
    // 规则内容
    var js = ""
    // 规则名称
    var name = ""
    // 创建人
    var creator = "" // system或者user，system是系统创建的，user是用户创建的，用户创建的可以删除，系统创建的不可以删除和修改
    // 结构数组
    var struct = "" // 类似于3.0版本的自定义规则一样，存储数据结构规则，如果是system，这个字段为空
    // 这个规则是否自动记录
    var autoRecord  = false
    // 这个规则是否启用
    var enabled = true
    companion object{
        suspend fun list(app: String, type: String, page: Int, limit: Int) : List<RuleModel> = withContext(Dispatchers.IO) {
            val response = Server.request("rule/list?page=$page&limit=$limit&app=$app&type=$type")
            val json = Gson().fromJson(response, JsonObject::class.java)

             runCatching { Gson().fromJson(json.getAsJsonArray("data"), Array<RuleModel>::class.java).toList() }.getOrNull() ?: emptyList()
        }

        suspend fun add(rule: RuleModel): Int  = withContext(Dispatchers.IO){
            val response = Server.request("rule/add", Gson().toJson(rule))
            val json = Gson().fromJson(response, JsonObject::class.java)
             Gson().fromJson(json.getAsJsonObject("data"), Int::class.java)
        }

        suspend fun update(rule: RuleModel) = withContext(Dispatchers.IO){
            Server.request("rule/update", Gson().toJson(rule))
        }

        suspend fun delete(id:Int) = withContext(Dispatchers.IO) {
            Server.request("rule/delete?id=$id")
        }

        suspend fun apps(): JsonObject = withContext(Dispatchers.IO) {
            val response = Server.request("rule/apps")
            val json = Gson().fromJson(response, JsonObject::class.java)
           runCatching { json.getAsJsonObject("data") }.getOrNull() ?: JsonObject()
        }
    }
}