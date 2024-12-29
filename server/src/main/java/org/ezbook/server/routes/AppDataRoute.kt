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

package org.ezbook.server.routes

import io.ktor.application.ApplicationCall
import io.ktor.http.Parameters
import io.ktor.request.receive
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.AppDataModel
import org.ezbook.server.models.ResultModel

class AppDataRoute(private val session: ApplicationCall) {
    private val params: Parameters = session.request.queryParameters

    /**
     * 获取规则列表
     */
    suspend fun list(): ResultModel {

        // 清理过期数据
        Db.get().dataDao().clearOld()

        val page = params["page"]?.toInt() ?: 1
        val limit = params["limit"]?.toInt() ?: 10

        val app = params["app"] ?: ""
        var type: String? = params["type"] ?: ""
        val match = params["match"]?.toBoolean() ?: false
        var search: String? = params["search"] ?: ""
        if (type == "") type = null

        if (search == "") search = null

        val offset = (page - 1) * limit

        val logs = Db.get().dataDao().load(limit, offset, app, match, type, search)
        return ResultModel(200, "OK", logs)
    }

    /**
     * 添加规则，
     */
    suspend fun clear(): ResultModel {
        Db.get().dataDao().clear()
        return ResultModel(200, "OK")
    }

    /**
     * 获取app列表
     */
    suspend fun apps(): ResultModel {
        val apps = Db.get().dataDao().queryApps()
        val map = hashMapOf<String, Int>()
        apps.forEach {
            if (it !in map) {
                map[it] = 1
            } else {
                map[it] = map[it]!! + 1
            }
        }
        return ResultModel(200, "OK", map)
    }

    suspend fun put(): ResultModel {
        val data = session.receive(AppDataModel::class)
        if (data.id == 0L) {
            Db.get().dataDao().insert(data)
        } else {
            Db.get().dataDao().update(data)
        }
        return ResultModel(200, "OK")
    }

    suspend fun delete(): ResultModel {
        val id = params["id"]?.toLong() ?: 0
        Db.get().dataDao().delete(id)
        return ResultModel(200, "OK")
    }

}