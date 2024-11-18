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
import org.ezbook.server.db.model.LogModel
import org.ezbook.server.models.ResultModel

class LogRoute(private val session: ApplicationCall) {
    private val params: Parameters = session.request.queryParameters
    /**
     * 获取日志列表
     */
    fun list(): ResultModel {
        //remove expired data
        Db.get().logDao().clearOld()
        val page = params["page"]?.toInt() ?: 1
        val limit = params["limit"]?.toInt() ?: 10
        val offset = (page - 1) * limit
        val logs = Db.get().logDao().loadPage(limit, offset)
        return ResultModel(200, "OK", logs)
    }

    /**
     * 添加日志
     */
    suspend fun add(): ResultModel {
        val log = session.receive(LogModel::class)
        val id = Db.get().logDao().insert(log)
        return ResultModel(200, "OK",id)
    }

    /**
     * 清空日志
     */
    fun clear(): ResultModel {
        Db.get().logDao().clear()
        return ResultModel(200, "OK",null)
    }
}