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
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.CategoryModel
import org.ezbook.server.models.ResultModel

class CategoryRoute(private val session: ApplicationCall) {
    private val params: Parameters = session.request.queryParameters
    /**
     * 获取分类
     */
    fun list(): ResultModel {
        
        val book = params["book"]?: ""
        if (book.isEmpty()) {
            return ResultModel(400, "book is empty")
        }
        val type = params["type"]?: ""
        if (type.isEmpty()) {
            return ResultModel(400, "type is empty")
        }

        val parent = params["parent"]?: "-1"


        return ResultModel(200, "OK", Db.get().categoryDao().load(book, type, parent))
    }

    /**
     * 设置分类
     */
    suspend fun put(): ResultModel {
        val md5 = params["md5"]?: ""
        val json = session.receive( Array<CategoryModel>::class)
        val id = Db.get().categoryDao().put(json)
        SettingRoute.setByInner(Setting.HASH_CATEGORY, md5)
        return ResultModel(200, "OK", id)
    }


    fun get(): ResultModel {
        
        var book: String? = params["book"]?: ""
        if (book == "") {
            book = null
        }
        var type: String? = params["type"]?: ""
        if (type == "") {
            type = null
        }
        val name = params["name"]?: ""
        if (name.isEmpty()) {
            return ResultModel(400, "name is empty")
        }


        return ResultModel(200, "OK", Db.get().categoryDao().getByName(book, type, name))
    }
}