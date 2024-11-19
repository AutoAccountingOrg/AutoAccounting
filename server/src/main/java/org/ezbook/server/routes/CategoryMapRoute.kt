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
import org.ezbook.server.db.model.CategoryMapModel
import org.ezbook.server.models.ResultModel

class CategoryMapRoute(private val session: ApplicationCall) {
    private val params: Parameters = session.request.queryParameters
    suspend fun list(): ResultModel {
      
        val limit = params["limit"]?.toInt() ?: 10



        if (limit == 0) {
            return ResultModel(200, "OK", Db.get().categoryMapDao().loadWithoutLimit())
        }
        val page = params["page"]?.toInt() ?: 1
        val offset = (page - 1) * limit
        var search: String? = params["search"] ?: ""
        if (search == "") search = null
        val logs = Db.get().categoryMapDao().loadWithLimit(limit, offset, search)


        return ResultModel(200, "OK", logs)
    }

    suspend fun put(): ResultModel {
        val model = session.receive( CategoryMapModel::class)
        val name = model.name
        val modelItem = Db.get().categoryMapDao().query(name)
        if (modelItem == null) {
            model.id = Db.get().categoryMapDao().insert(model)
        } else {
            model.id = modelItem.id
            Db.get().categoryMapDao().update(model)
        }
        return ResultModel(200, "OK", model.id)
    }

    suspend fun delete(): ResultModel {
        val id = (params["id"] ?: "0").toLong()
        Db.get().categoryMapDao().delete(id)
        return ResultModel(200, "OK", id)
    }
}