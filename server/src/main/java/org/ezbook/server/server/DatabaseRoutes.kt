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

import android.content.Context
import io.ktor.application.call
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.response.respondFile
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import org.ezbook.server.db.Db
import org.ezbook.server.models.ResultModel
import java.io.IOException

/**
 * 数据库管理路由配置
 * 提供数据库的备份、恢复和清理功能，支持完整的数据迁移和管理
 */
fun Route.databaseRoutes(context: Context) {
    route("/db") {
        /**
         * GET /db/export - 导出数据库
         * 将当前数据库导出为文件，供用户下载备份
         *
         * @return 数据库备份文件
         */
        get("/export") {
            try {
                val file = Db.copy(context)
                call.respondFile(file.parentFile!!, file.name)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(ResultModel(500, "Database export failed: ${e.message}"))
            }
        }

        /**
         * POST /db/import - 导入数据库
         * 接收用户上传的数据库文件并进行数据恢复
         * 支持multipart/form-data格式的文件上传
         *
         * @param file 数据库备份文件（multipart上传）
         * @return ResultModel 导入操作结果
         */
        post("/import") {
            try {
                val multipart = call.receiveMultipart()

                // 遍历上传的文件部分
                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            val targetFile = context.getDatabasePath("db_backup.db")
                            // 保存上传的文件
                            part.streamProvider().use { input ->
                                targetFile.outputStream().buffered().use { output ->
                                    input.copyTo(output)
                                }
                            }

                            // 导入数据库
                            Db.import(context, targetFile)
                            call.respond(ResultModel(200, "File uploaded successfully"))
                            return@forEachPart
                        }

                        else -> {
                            // 忽略非文件部分
                        }
                    }
                    part.dispose()
                }
            } catch (e: Exception) {
                call.respond(ResultModel(500, "Database import failed: ${e.message}"))
            }
        }

        /**
         * POST /db/clear - 清空数据库
         * 清除所有数据，恢复到初始状态
         *
         * @return ResultModel 清理操作结果
         */
        post("/clear") {
            try {
                Db.clear(context)
                call.respond(ResultModel(200, "Database cleared"))
            } catch (e: Exception) {
                call.respond(ResultModel(500, "Database clear failed: ${e.message}"))
            }
        }
    }
} 