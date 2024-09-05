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
import org.ezbook.server.constant.DataType

@Entity
class AppDataModel {
    @PrimaryKey(autoGenerate = true)
    var id = 0L
    /**
     * 对于App数据，就是Hook得到的数据一般是Json：{} 具体情况具体分析
     * 对于短信数据获取到的是短信内容 {msg:xxx,body:''}
     * 对于通知数据获取到的是如下json:{title:xxx,content:xxx},偷懒省略引号
     * 对于无障碍抓取的数据，也是json
     */
    var data: String = ""

    /**
     * 指的是数据类型
     */
    var type: DataType = DataType.DATA

    /**
     * 包名
     */
    var app: String = "" // 源自APP

    /**
     * 时间
     */
    var time: Long = 0 // 时间

    /**
     * 是否匹配规则
     */
    var match: Boolean = false

    /**
     * 匹配到的规则名称
     * */
    var rule: String = ""

    /**
     * 关联github issue
     */
    var issue: Int = 0

    companion object{
        /**
         * 根据条件查询
         * @param app 应用
         * @param type 类型
         * @param page 页码
         * @param limit 每页数量
         * @return 规则列表
         */
        suspend fun list(app: String, type: String, page: Int, limit: Int, search:String) : List<AppDataModel> = withContext(Dispatchers.IO) {
            val response = Server.request("data/list?page=$page&limit=$limit&app=$app&type=$type&search=${Uri.encode(search)}")
            val json = Gson().fromJson(response, JsonObject::class.java)

            runCatching { Gson().fromJson(json.getAsJsonArray("data"), Array<AppDataModel>::class.java).toList() }.getOrNull() ?: emptyList()
        }

        suspend fun clear() = withContext(Dispatchers.IO){
            Server.request("data/clear")
        }

        suspend fun put(data: AppDataModel) = withContext(Dispatchers.IO){
            Server.request("data/put", Gson().toJson(data))
        }

        suspend fun delete(id: Long) = withContext(Dispatchers.IO){
            Server.request("data/delete?id=$id")
        }

        suspend fun apps(): JsonObject = withContext(Dispatchers.IO) {
            val response = Server.request("data/apps")
            val json = Gson().fromJson(response, JsonObject::class.java)
            runCatching { json.getAsJsonObject("data") }.getOrNull() ?: JsonObject()
        }
    }

    override fun toString(): String {
        return "AppDataModel(id=$id, data='', type=$type, app='$app', time=$time, match=$match, rule='$rule', issue=$issue)"
    }

}