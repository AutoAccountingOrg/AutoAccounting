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

import android.util.Log
import com.google.gson.Gson
import org.ezbook.server.Server
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.CategoryModel
import org.ezbook.server.db.model.SettingModel
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.response.Response

class CategoryRoute(private val session: IHTTPSession) {
    /**
     * 获取分类
     */
    fun list(): Response {
        val params = session.parameters
        val book = params["book"]?.firstOrNull()?:""
        if (book.isEmpty()) {
            return Server.json(400, "book is empty")
        }
        val type = params["type"]?.firstOrNull()?:""
        if (type.isEmpty()) {
            return Server.json(400, "type is empty")
        }

        val parent = params["parent"]?.firstOrNull()?:"-1"


        return Server.json(200, "OK", Db.get().categoryDao().load(book, type, parent), 0)
    }

    /**
     * 设置分类
     */
    fun put(): Response {
        val params = session.parameters
        val md5 = params["md5"]?.firstOrNull()?:""
        val data = Server.reqData(session)
        val json = Gson().fromJson(data, Array<CategoryModel>::class.java)
        val id = Db.get().categoryDao().put(json)
        SettingRoute.setByInner("sync_category_md5",md5)
        return Server.json(200, "OK", id)
    }


    fun get(): Response {
        val params = session.parameters
        var book:String? = params["book"]?.firstOrNull()?:""
        if(book == ""){
            book = null
        }
        var type:String? = params["type"]?.firstOrNull()?:""
        if(type == ""){
            type = null
        }
        val name = params["name"]?.firstOrNull()?:""
        if (name.isEmpty()) {
            return Server.json(400, "name is empty")
        }

        Log.d("CategoryRoute", "get: $book $type $name")

        return Server.json(200, "OK", Db.get().categoryDao().getByName(book, type, name))
    }
}