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

import org.ezbook.server.Server
import org.ezbook.server.db.Db
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.NanoHTTPD.MIME_PLAINTEXT
import org.nanohttpd.protocols.http.NanoHTTPD.ResponseException
import org.nanohttpd.protocols.http.request.Method
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.protocols.http.response.Response.newChunkedResponse
import org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse
import org.nanohttpd.protocols.http.response.Status
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException


class DatabaseRoute(
    private val session: IHTTPSession,
    private val context: android.content.Context
) {

    fun exportDb(): Response {
        val file = Db.copy(context)
        try {
            // 使用FileInputStream流式读取文件
            val fis = FileInputStream(file)
            // 返回流式传输的响应，确保文件不会完全加载到内存
            val response = newChunkedResponse(Status.OK, "application/octet-stream", fis)

            response.addHeader("Content-Disposition", "attachment; filename=\"auto.db\"")

            return response

        } catch (e: IOException) {
            e.printStackTrace()
            return newFixedLengthResponse(
                Status.INTERNAL_ERROR,
                MIME_PLAINTEXT,
                "Error Reading File"
            )
        }
    }

    fun importDb(): Response {
        if (Method.POST == session.method) {
            try {
                // 文件上传请求的表单数据会存储在 files Map 中
                val files: Map<String, String> = HashMap()
                // 获取请求的参数，并将上传的文件存储到临时文件
                session.parseBody(files)
                // 获取表单中的其他参数

                val filename = files["file"]!! // 表单中文件字段的名称应为 "file"
                val uploadedFile = File(filename)

                val targetFile = context.getDatabasePath("db_backup.db")

                FileInputStream(uploadedFile).use { `in` ->
                    FileOutputStream(targetFile).use { out ->
                        val buffer = ByteArray(1024)
                        var length: Int
                        while ((`in`.read(buffer).also { length = it }) > 0) {
                            out.write(buffer, 0, length)
                        }
                    }
                }

                Db.import(context, targetFile)

                // 返回响应
                return Server.json(200, "File uploaded successfully")
            } catch (e: IOException) {
                e.printStackTrace()
                return Server.json(500, "File upload failed")
            } catch (e: ResponseException) {
                e.printStackTrace()
                return Server.json(500, "File upload failed")
            }
        } else {
            // 返回上传文件的 HTML 表单页面
            val htmlForm = "<html><body>" +
                    "<form method='post' enctype='multipart/form-data'>" +
                    "Select a file: <input type='file' name='file'/>" +
                    "<input type='submit' value='Upload'/>" +
                    "</form>" +
                    "</body></html>"
            return newFixedLengthResponse(htmlForm)
        }
    }
}