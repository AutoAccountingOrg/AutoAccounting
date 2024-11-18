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
import org.ezbook.server.db.model.CategoryRuleModel
import org.ezbook.server.models.ResultModel

class CategoryRuleRoute(private val session: ApplicationCall) {
    private val params: Parameters = session.request.queryParameters
    fun list(): ResultModel {
        
        val page = params["page"]?.toInt() ?: 1
        val limit = params["limit"]?.toInt() ?: 10
        val offset = (page - 1) * limit
        val logs = Db.get().categoryRuleDao().load(limit, offset)
        return ResultModel(200, "OK", logs)
    }

    suspend fun put(): ResultModel {
        val model = session.receive(CategoryRuleModel::class)

        if (model.id == 0L) {
            model.id = Db.get().categoryRuleDao().insert(model)
        } else {
            Db.get().categoryRuleDao().update(model)
        }
        return ResultModel(200, "OK", model.id)
    }

    fun delete(): ResultModel {
        val id = (params["id"]?: "0").toLong()
        Db.get().categoryRuleDao().delete(id)
        return ResultModel(200, "OK", id)
    }
}