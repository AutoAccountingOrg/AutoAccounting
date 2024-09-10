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
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.AssetsModel
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.response.Response

class AssetsRoute(private val session: IHTTPSession) {
    fun list(): Response {
        val logs =  Db.get().assetsDao().load(90000, 0)

        return Server.json(200, "OK", logs)
    }

    fun put(): Response {
        val params = session.parameters
        val md5 = params["md5"]?.firstOrNull()?:""
        val data = Server.reqData(session)
        val json = Gson().fromJson(data, Array<AssetsModel>::class.java)
        val id = Db.get().assetsDao().put(json)
        SettingRoute.setByInner(Setting.HASH_ASSET,md5)
        return Server.json(200, "OK", id)
    }

    fun get(): Response {
        val params = session.parameters
        val name = params["name"]?.firstOrNull()?:""
        val data = Db.get().assetsDao().query(name)
        return Server.json(200, "OK", data)
    }
}