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
import org.ezbook.server.Server
import org.ezbook.server.constant.BillState
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.models.ResultModel

class BillRoute(private val session: ApplicationCall) {
    private val params: Parameters = session.request.queryParameters
    fun list(): ResultModel {
        Db.get().billInfoDao().deleteNoGroup()
        //删除一年之前的账单数据
        Db.get().billInfoDao().clearOld(System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000)
        //获取分页数据
        val page = params["page"]?.toInt() ?: 1
        val limit = params["limit"]?.toInt() ?: 10
        val offset = (page - 1) * limit
        val logs = Db.get().billInfoDao().loadPage(limit, offset)
        return ResultModel(200, "OK", logs)
    }


    suspend fun put(): ResultModel {
        val json = session.receive(BillInfoModel::class)
        val query = Db.get().billInfoDao().queryId(json.id)
        if (query != null) {
            Db.get().billInfoDao().update(json)
            return ResultModel(200, "OK", json.id)
        } else {
            val id = Db.get().billInfoDao().insert(json)
            return ResultModel(200, "OK", id)
        }
    }

    fun remove(): ResultModel {
        val params = session.parameters
        val id = params["id"]?.toLong() ?: 0
        Server.log("删除账单:$id")
        Db.get().billInfoDao().deleteId(id)
        Db.get().billInfoDao().deleteGroup(id)
        return ResultModel(200, "OK", 0)
    }

    fun sync(): ResultModel {
        val result = Db.get().billInfoDao().queryNoSync()
        return ResultModel(200, "OK", result)
    }

    fun status(): ResultModel {
        val params = session.parameters
        val id = params["id"]?.toLong() ?: 0
        val status = params["sync"]?.toBoolean() ?: false
        Db.get().billInfoDao().updateStatus(id, if (status) BillState.Synced else BillState.Edited)
        return ResultModel(200, "OK", 0)
    }

    fun group(): ResultModel {
        val params = session.parameters
        val id = params["id"]?.toLong() ?: 0
        val result = Db.get().billInfoDao().queryGroup(id)
        return ResultModel(200, "OK", result)
    }

    fun get(): ResultModel {
        val params = session.parameters
        val id = params["id"]?.toLong() ?: 0
        val result = Db.get().billInfoDao().queryId(id)
        return ResultModel(200, "OK", result)
    }

    fun clear(): ResultModel {
        Db.get().billInfoDao().clearOld(System.currentTimeMillis())
        return ResultModel(200,"OK")
    }

}