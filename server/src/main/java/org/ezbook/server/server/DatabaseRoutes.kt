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
import org.ezbook.server.log.ServerLog
import org.ezbook.server.tools.runCatchingExceptCancel

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
            Db.cleanupResidualBackups(context)
            runCatchingExceptCancel {
                val file = Db.copy(context)
                // 发送导出文件
                call.respondFile(file.parentFile!!, file.name)
                // 发送完成后清理临时文件，避免残留在缓存目录
                runCatching { file.delete() }

            }.onFailure {
                ServerLog.e("数据库导出失败：${it.message}", it)
                call.respond(ResultModel.error(500, "Database export failed: ${it.message}"))
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
            runCatchingExceptCancel {
                val multipart = call.receiveMultipart()

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            val targetFile = context.getDatabasePath("db_backup.db")
                            part.streamProvider().use { input ->
                                targetFile.outputStream().buffered().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            Db.import(context, targetFile)
                            call.respond(ResultModel.ok("File uploaded successfully"))
                            return@forEachPart
                        }

                        else -> {}
                    }
                    part.dispose()
                }
            }.onFailure {
                ServerLog.e("数据库导入失败：${it.message}", it)
                call.respond(ResultModel.error(500, "Database import failed: ${it.message}"))
            }
        }

        /**
         * POST /db/clear - 清空数据库
         * 清除所有数据，恢复到初始状态
         *
         * @return ResultModel 清理操作结果
         */
        post("/clear") {
            runCatchingExceptCancel {
                Db.clear(context)
                call.respond(ResultModel.ok("Database cleared"))
            }.onFailure {
                ServerLog.e("清空数据库失败：${it.message}", it)
                call.respond(ResultModel.error(500, "Database clear failed: ${it.message}"))
            }
        }
    }
} 