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
import fi.iki.elonen.NanoHTTPD
import org.ezbook.server.Server
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.RuleModel

class RuleRoute(private val session: NanoHTTPD.IHTTPSession) {
    /**
     * 获取规则列表
     */
    fun list(): NanoHTTPD.Response {
        val params = session.parameters
        val page = params["page"]?.firstOrNull()?.toInt() ?: 1
        val limit = params["limit"]?.firstOrNull()?.toInt() ?: 10

        val app = params["app"]?.firstOrNull() ?: ""
        val type = params["type"]?.firstOrNull() ?: ""

        val offset = (page - 1) * limit

        val logs = if (type === "") {
            Db.get().ruleDao().loadByAppAndType(limit, offset,app)
        } else {
            Db.get().ruleDao().loadByAppAndType(limit, offset,app,type)
        }

        val total = if (type === "") {
            Db.get().ruleDao().count(app)
        } else {
            Db.get().ruleDao().count(app,type)
        }
        return Server.json(200, "OK", logs, total)
    }

    /**
     * 添加规则，
     */
    fun add(): NanoHTTPD.Response {
        val json = Gson().fromJson(Server.reqData(session), RuleModel::class.java)
        val id = Db.get().ruleDao().insert(json)
        return Server.json(200, "OK", id)
    }

    /**
     * 更新规则
     */
    fun update(): NanoHTTPD.Response {
        val json = Gson().fromJson(Server.reqData(session), RuleModel::class.java)
        val id = Db.get().ruleDao().update(json)
        return Server.json(200, "OK", id)
    }
    /**
     * 删除规则
     */
    fun delete(): NanoHTTPD.Response {
        val params = session.parameters
        val id = params["id"]?.firstOrNull()?.toInt() ?: 0
        Db.get().ruleDao().delete(id)
        return Server.json(200, "OK")
    }

    /**
     * 获取app列表
     */
    fun apps(): NanoHTTPD.Response {
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
    fun system(): NanoHTTPD.Response {
        val rules = Db.get().ruleDao().loadAllSystem()
        return Server.json(200, "OK", rules)
    }
}
