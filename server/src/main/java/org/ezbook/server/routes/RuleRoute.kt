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
import org.ezbook.server.db.model.RuleModel
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.response.Response

class RuleRoute(private val session: IHTTPSession) {
    /**
     * 获取规则列表
     */
    fun list(): Response {
        val params = session.parameters
        val page = params["page"]?.firstOrNull()?.toInt() ?: 1
        val limit = params["limit"]?.firstOrNull()?.toInt() ?: 10

        val app = params["app"]?.firstOrNull() ?: ""
        var type: String? = params["type"]?.firstOrNull()?:""
        var search: String? = params["search"]?.firstOrNull()?:""

        val offset = (page - 1) * limit

        if (type == "")  type = null

        if (search == "")  search = null

        val logs =  Db.get().ruleDao().loadByAppAndFilters(limit, offset,app,type,search)


        return Server.json(200, "OK", logs)
    }

    /**
     * 添加规则，
     */
    fun add(): Response {
        val data = Server.reqData(session)
        val json = Gson().fromJson(data, RuleModel::class.java)
        val id = Db.get().ruleDao().insert(json)
        return Server.json(200, "OK", id)
    }

    /**
     * 更新规则
     */
    fun update(): Response {
        val data = Server.reqData(session)
        val json = Gson().fromJson(data, RuleModel::class.java)
        val id = Db.get().ruleDao().update(json)
        return Server.json(200, "OK", id)
    }

    /**
     * 删除规则
     */
    fun delete(): Response {
        val params = session.parameters
        val id = params["id"]?.firstOrNull()?.toInt() ?: 0
        Db.get().ruleDao().delete(id)
        return Server.json(200, "OK")
    }

    /**
     * 获取app列表
     */
    fun apps(): Response {
        val apps = Db.get().ruleDao().queryApps()
        val map = hashMapOf<String,Int>()
        apps.forEach {
            if (it !in map) {
                map[it] = 1
            } else {
                map[it] = map[it]!! + 1
            }
        }
        return Server.json(200, "OK", map)
    }

    /**
     * 获取所有的系统规则
     */
    fun system(): Response {
        val rules = Db.get().ruleDao().loadAllSystem()
        return Server.json(200, "OK", rules)
    }
}
