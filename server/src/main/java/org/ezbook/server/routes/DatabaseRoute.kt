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
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.response.respondFile
import org.ezbook.server.db.Db
import org.ezbook.server.models.ResultModel
import java.io.IOException


class DatabaseRoute(
    private val session: ApplicationCall,
    private val context: android.content.Context
) {
    suspend fun exportDb() {
        val file = Db.copy(context)
        try {
            session.respondFile(file.parentFile!!, file.name)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    suspend fun importDb() {
        // 检查请求是否是multipart类型
        val multipart = session.receiveMultipart()

        // 遍历接收到的各部分
        multipart.forEachPart { part ->
            when (part) {
                is PartData.FileItem -> {
                    val targetFile = context.getDatabasePath("db_backup.db")
                    // 保存文件
                    part.streamProvider().use { input ->
                        targetFile.outputStream().buffered().use { output ->
                            input.copyTo(output)
                        }
                    }
                    Db.import(context, targetFile)
                    session.respond(ResultModel(200, "File uploaded successfully"))
                    return@forEachPart
                }
                else -> {
                    // 忽略其他部分
                }
            }
            part.dispose()
        }

    }

   suspend fun clearDb() {
        Db.clear(context)
        session.respond(ResultModel(200, "Database cleared"))
    }
}