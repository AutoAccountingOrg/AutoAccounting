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

package net.ankio.data

import androidx.room.RoomDatabase
import net.ankio.data.routes.UIRoute
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.response.Response

class App(db: RoomDatabase) {
    fun route(session: IHTTPSession): Response {
        val uri = session.uri.replace("//", "/")
        if (uri.startsWith("/ui")){
            return  UIRoute(session).index(uri.replace("/ui", ""))
        }
        return Response.newFixedLengthResponse("404")
    }
}