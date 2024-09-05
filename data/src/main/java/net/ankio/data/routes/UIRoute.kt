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

package net.ankio.data.routes

import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.protocols.http.response.Status

class UIRoute(session: IHTTPSession) {
    fun index(path: String):Response {
        var file = path
        if (path == "/" || path == "" || path.isEmpty()) {
            file = "/index.html"
        }

        //读取resources文件夹
        val inputStream = javaClass.getResourceAsStream(file)
            ?: return Response.newFixedLengthResponse(Status.NOT_FOUND, "text/html", "file not found: $file")

        val mimeType = when (path.substringAfterLast('.')) {
            "html" -> "text/html"
            "css" -> "text/css"
            "js" -> "text/javascript"
            "png" -> "image/png"
            "jpg" -> "image/jpeg"
            "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "ico" -> "image/x-icon"
            else -> "text/html"
        }

        return Response.newChunkedResponse(Status.OK, mimeType, inputStream)
    }
}