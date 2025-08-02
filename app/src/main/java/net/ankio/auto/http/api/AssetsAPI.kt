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
import org.ezbook.server.db.model.AssetsModel

/**
 * 资产相关的API接口
 * 提供资产的查询、添加和获取等操作
 */
object AssetsAPI {
    /**
     * 获取所有资产列表
     * @return 返回资产模型列表，如果请求失败则返回空列表
     */
    suspend fun list(): List<AssetsModel> = withContext(
        Dispatchers.IO
    ) {
        val response = LocalNetwork.get("assets/list")


        runCatching {
            val json = Gson().fromJson(response, JsonObject::class.java)
            Gson().fromJson(
                json.getAsJsonArray("data"),
                Array<AssetsModel>::class.java
            ).toList()
        }.getOrNull() ?: emptyList()
    }

    /**
     * 批量添加或更新资产数据
     * @param data 要添加或更新的资产数据列表
     * @param md5 数据的MD5校验值，用于验证数据完整性
     */
    suspend fun put(data: ArrayList<AssetsModel>, md5: String) {
        val json = Gson().toJson(data)
        LocalNetwork.post("assets/put?md5=$md5", json)
    }

    /**
     * 根据资产名称获取单个资产信息
     * @param name 资产名称
     * @return 返回对应的资产模型，如果未找到则返回null
     */
    suspend fun getByName(name: String): AssetsModel? {
        val response = LocalNetwork.get("assets/get?name=${Uri.encode(name)}")

        return runCatching {
            val json = Gson().fromJson(response, JsonObject::class.java)
            Gson().fromJson(
                json.getAsJsonObject("data"),
                AssetsModel::class.java
            )
        }.getOrNull()
    }

}