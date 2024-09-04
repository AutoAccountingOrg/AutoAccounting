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
import org.ezbook.server.db.model.CategoryRuleModel
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.response.Response

class CategoryRuleRoute(private val session: IHTTPSession) {
    fun list(): Response {
        val params = session.parameters
        val page = params["page"]?.firstOrNull()?.toInt() ?: 1
        val limit = params["limit"]?.firstOrNull()?.toInt() ?: 10
        val offset = (page - 1) * limit
        val logs =  Db.get().categoryRuleDao().load(limit, offset)
        return Server.json(200, "OK", logs)
    }

    fun put(): Response {
        val data = Server.reqData(session)
        val model = Gson().fromJson(data, CategoryRuleModel::class.java)
        
        if (model.id == 0L){
            model.id  = Db.get().categoryRuleDao().insert(model)
        }else{
            Db.get().categoryRuleDao().update(model)
        }
        return Server.json(200, "OK", model.id)
    }

    fun delete(): Response {
        val params = session.parameters
        val id = ( params["id"]?.firstOrNull()?:"0").toLong()
        Db.get().categoryRuleDao().delete(id)
        return Server.json(200, "OK", id)
    }
}