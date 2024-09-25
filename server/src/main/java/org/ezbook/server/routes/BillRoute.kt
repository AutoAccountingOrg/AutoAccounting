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

import com.google.gson.Gson
import org.ezbook.server.Server
import org.ezbook.server.constant.BillState
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.BillInfoModel
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.response.Response

class BillRoute(private val session: IHTTPSession) {

    fun list(): Response {
        Db.get().billInfoDao().deleteNoGroup()
        //删除一年之前的账单数据
        Db.get().billInfoDao().clearOld(System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000)
        //获取分页数据
        val params = session.parameters
        val page = params["page"]?.firstOrNull()?.toInt() ?: 1
        val limit = params["limit"]?.firstOrNull()?.toInt() ?: 10
        val state = params["state"]?.firstOrNull() ?: ""
        val offset = (page - 1) * limit
        val logs = Db.get().billInfoDao().loadPage(limit, offset,state.split(",").filter { it.isNotEmpty() })
        return Server.json(200, "OK", logs)
    }


    fun put(): Response {
        val json = Gson().fromJson(Server.reqData(session), BillInfoModel::class.java)
        val query = Db.get().billInfoDao().queryId(json.id)
        if (query != null) {
            Db.get().billInfoDao().update(json)
            return Server.json(200, "OK", json.id)
        } else {
            val id = Db.get().billInfoDao().insert(json)
            return Server.json(200, "OK", id)
        }
    }

    fun remove(): Response {
        val params = session.parameters
        val id = params["id"]?.firstOrNull()?.toLong() ?: 0
        Server.log("删除账单:$id")
        Db.get().billInfoDao().deleteId(id)
        Db.get().billInfoDao().deleteGroup(id)
        return Server.json(200, "OK", 0)
    }

    fun sync(): Response {
        val result = Db.get().billInfoDao().queryNoSync()
        return Server.json(200, "OK", result)
    }

    fun status(): Response {
        val params = session.parameters
        val id = params["id"]?.firstOrNull()?.toLong() ?: 0
        val status = params["sync"]?.firstOrNull()?.toBoolean() ?: false
        Db.get().billInfoDao().updateStatus(id, if (status) BillState.Synced else BillState.Edited)
        return Server.json(200, "OK", 0)
    }

    fun group(): Response {
        val params = session.parameters
        val id = params["id"]?.firstOrNull()?.toLong() ?: 0
        val result = Db.get().billInfoDao().queryGroup(id)
        return Server.json(200, "OK", result)
    }

    fun get(): Response {
        val params = session.parameters
        val id = params["id"]?.firstOrNull()?.toLong() ?: 0
        val result = Db.get().billInfoDao().queryId(id)
        return Server.json(200, "OK", result)
    }

}