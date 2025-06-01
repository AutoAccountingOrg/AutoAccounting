/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

package net.ankio.auto.http.api

import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.http.LocalNetwork
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.CategoryModel

/**
 * CategoryAPI 对象提供了与分类相关的网络请求功能
 * 包括获取分类列表、根据名称获取分类以及更新分类数据
 */
object CategoryAPI {

    /**
     * 获取指定账本、类型和父分类下的分类列表
     *
     * @param bookID 账本ID
     * @param type 账单类型
     * @param parent 父分类ID
     * @return 返回分类模型列表，如果请求失败则返回空列表
     */
    suspend fun list(
        bookID: String,
        type: BillType,
        parent: String,
    ): List<CategoryModel> = withContext(Dispatchers.IO) {
        val response = LocalNetwork.request("category/list?book=$bookID&type=$type&parent=$parent")

        runCatching {
            val json = Gson().fromJson(response, JsonObject::class.java)
            Gson().fromJson(
                json.getAsJsonArray("data"),
                Array<CategoryModel>::class.java
            ).toList()
        }.getOrNull() ?: emptyList()
    }

    /**
     * 根据分类名称获取分类信息
     *
     * @param name 分类名称
     * @param bookID 账本ID，默认为空字符串
     * @param type 账单类型，默认为空字符串
     * @return 返回分类模型，如果未找到则返回null
     */
    suspend fun getByName(
        name: String,
        bookID: String = "",
        type: String = ""
    ): CategoryModel? = withContext(Dispatchers.IO) {
        val response =
            LocalNetwork.request("category/get?name=${Uri.encode(name)}&book=$bookID&type=$type")

        runCatching {
            val json = Gson().fromJson(response, JsonObject::class.java)
            Gson().fromJson(
                json.getAsJsonObject("data"),
                CategoryModel::class.java
            )
        }.getOrNull()
    }

    /**
     * 更新分类数据
     *
     * @param data 要更新的分类数据列表
     * @param md5 数据的MD5校验值
     * @return 返回服务器响应结果
     */
    suspend fun put(data: ArrayList<CategoryModel>, md5: String) = withContext(Dispatchers.IO) {
        LocalNetwork.request("category/put?md5=$md5", Gson().toJson(data))
    }
}