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

import fi.iki.elonen.NanoHTTPD
import org.ezbook.server.Server
import org.ezbook.server.db.Db

class AppDataRoute(private val session: NanoHTTPD.IHTTPSession) {
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

        val logs = if (type.isEmpty()) {
            Db.get().dataDao().loadByAppAndType(limit, offset,app)
        } else {
            Db.get().dataDao().loadByAppAndType(limit, offset,app,type)
        }

        val total = if (type.isEmpty()) {
            Db.get().dataDao().count(app)
        } else {
            Db.get().dataDao().count(app,type)
        }
        return Server.json(200, "OK", logs, total)
    }

}