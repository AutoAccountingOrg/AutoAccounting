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
import net.ankio.auto.storage.Logger
import org.ezbook.server.tools.runCatchingExceptCancel
import org.ezbook.server.db.model.RuleModel

object RuleManageAPI {
    /**
     * 根据条件查询
     * @param app 应用
     * @param type 类型
     * @param page 页码
     * @param limit 每页数量
     * @return 规则列表
     */
    suspend fun list(
        app: String,
        type: String,
        creator: String,
        page: Int,
        limit: Int,
        search: String = ""
    ): List<RuleModel> = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.get<List<RuleModel>>(
                "rule/list?page=$page&limit=$limit&app=$app&creator=${creator}&type=$type&search=${
                    Uri.encode(search)
                }"
            ).getOrThrow()
            resp.data ?: emptyList()
        }.getOrElse {
            Logger.e("list error: ${it.message}", it)
            emptyList()
        }
    }

    /**
     * 获取所有系统规则
     */
    suspend fun system(name: String): RuleModel? = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp =
                LocalNetwork.get<RuleModel>("rule/system?name=${Uri.encode(name)}").getOrThrow()
            resp.data
        }.getOrElse {
            Logger.e("system error: ${it.message}", it)
            null
        }
    }

    suspend fun deleteSystemRule() = withContext(Dispatchers.IO) {

        runCatchingExceptCancel {
            LocalNetwork.post<String>("rule/deleteSystemRule").getOrThrow()
        }.getOrElse {
            Logger.e("deleteSystemRule error: ${it.message}", it)

        }
    }

    suspend fun put(rule: RuleModel) = withContext(Dispatchers.IO) {

        runCatchingExceptCancel {
            LocalNetwork.post<String>("rule/put", Gson().toJson(rule)).getOrThrow()
        }.getOrElse {
            Logger.e("put error: ${it.message}", it)

        }
    }

    /**
     * 添加规则
     */
    suspend fun add(rule: RuleModel): Int = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.post<Int>("rule/add", Gson().toJson(rule)).getOrThrow()
            resp.data ?: 0
        }.getOrElse {
            Logger.e("add error: ${it.message}", it)
            0
        }
    }

    /**
     * 更新规则
     */
    suspend fun update(rule: RuleModel) = withContext(Dispatchers.IO) {

        runCatchingExceptCancel {
            LocalNetwork.post<String>("rule/update", Gson().toJson(rule)).getOrThrow()
        }.getOrElse {
            Logger.e("update error: ${it.message}", it)

        }
    }

    /**
     * 删除规则
     */
    suspend fun delete(id: Int) = withContext(Dispatchers.IO) {

        runCatchingExceptCancel {
            LocalNetwork.post<String>("rule/delete", Gson().toJson(mapOf("id" to id))).getOrThrow()
        }.getOrElse {
            Logger.e("delete error: ${it.message}", it)

        }
    }

    /**
     * 获取app列表
     */
    suspend fun apps(): JsonObject = withContext(Dispatchers.IO) {

        return@withContext runCatchingExceptCancel {
            val resp = LocalNetwork.get<JsonObject>("rule/apps").getOrThrow()
            resp.data ?: JsonObject()
        }.getOrElse {
            Logger.e("apps error: ${it.message}", it)
            JsonObject()
        }
    }
}