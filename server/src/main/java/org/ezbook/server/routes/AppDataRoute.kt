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
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.AppDataModel
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.response.Response

class AppDataRoute(private val session: IHTTPSession) {
    /**
     * 获取规则列表
     */
    fun list(): Response {

        // 清理过期数据
        Db.get().dataDao().clearOld()


        val params = session.parameters
        val page = params["page"]?.firstOrNull()?.toInt() ?: 1
        val limit = params["limit"]?.firstOrNull()?.toInt() ?: 10

        val app = params["app"]?.firstOrNull() ?: ""
        var type: String? = params["type"]?.firstOrNull() ?: ""
        val match = params["match"]?.firstOrNull()?.toBoolean() ?: false
        var search: String? = params["search"]?.firstOrNull() ?: ""
        if (type == "") type = null

        if (search == "") search = null

        val offset = (page - 1) * limit

        val logs = Db.get().dataDao().load(limit, offset, app, match,type, search)
        return Server.json(200, "OK", logs)
    }

    /**
     * 添加规则，
     */
    fun clear(): Response {
        Db.get().dataDao().clear()
        return Server.json(200, "OK")
    }

    /**
     * 获取app列表
     */
    fun apps(): Response {
        val apps = Db.get().dataDao().queryApps()
        val map = hashMapOf<String, Int>()
        apps.forEach {
            if (it !in map) {
                map[it] = 1
            } else {
                map[it] = map[it]!! + 1
            }
        }
        return Server.json(200, "OK", map)
    }

    fun put(): Response {
        val data = Gson().fromJson(Server.reqData(session), AppDataModel::class.java)
        if (data.id == 0L) {
            Db.get().dataDao().insert(data)
        } else {
            Db.get().dataDao().update(data)
        }
        return Server.json(200, "OK")
    }

    fun delete(): Response {
        val id = session.parameters["id"]?.firstOrNull()?.toLong() ?: 0
        Db.get().dataDao().delete(id)
        return Server.json(200, "OK")
    }

}