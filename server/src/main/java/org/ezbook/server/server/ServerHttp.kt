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

package org.ezbook.server.server

import fi.iki.elonen.NanoHTTPD
import org.ezbook.server.Server.Companion.json
import org.ezbook.server.routes.AppDataRoute

class ServerHttp : NanoHTTPD(52045) {

    override fun serve(session: IHTTPSession): Response {
        return when (session.uri) {
            "/" -> json(200,"hello,自动记账",null,0)
            "/app/list" -> AppDataRoute(session).list()
            else -> json(404,"Not Found",null,0)
        }
    }
}