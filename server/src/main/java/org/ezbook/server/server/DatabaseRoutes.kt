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
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.request.contentType
import io.ktor.request.receiveChannel
import io.ktor.response.respond
import io.ktor.response.respondFile
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.routing.route
import io.ktor.utils.io.jvm.javaio.copyTo
import org.ezbook.server.db.Db
import org.ezbook.server.log.ServerLog
import org.ezbook.server.models.ResultModel
import org.ezbook.server.tools.runCatchingExceptCancel
import java.io.IOException

/**
 * 数据库管理路由配置
 * 提供数据库的备份、恢复和清理功能，支持完整的数据迁移和管理
 */
fun Route.databaseRoutes(context: Context) {
    route("/db") {
        suspend fun handleImport(call: ApplicationCall) {
            val contentType = call.request.contentType().withoutParameters()
            if (contentType.match(ContentType.MultiPart.FormData)) {
                throw IOException("Multipart database import is no longer supported. Please update the client and retry.")
            }

            val contentLength = call.request.headers[HttpHeaders.ContentLength]?.toLongOrNull()
            if (contentLength != null && contentLength <= 0L) {
                throw IOException("Import file does not exist or is empty")
            }

            Db.cleanupResidualBackups(context)
            val targetFile = context.getDatabasePath("db_backup.db")
            targetFile.parentFile?.apply {
                if (!exists()) mkdirs()
            }

            try {
                targetFile.outputStream().buffered().use { output ->
                    call.receiveChannel().copyTo(output)
                }

                if (!targetFile.exists() || targetFile.length() == 0L) {
                    throw IOException("Import file does not exist or is empty")
                }

                Db.import(context, targetFile)
            } finally {
                runCatching { targetFile.delete() }
            }

            call.respond(ResultModel.ok("File uploaded successfully"))
        }

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
         * POST/PUT /db/import - 导入数据库
         * 接收用户上传的数据库文件并进行数据恢复
         * 使用 application/octet-stream 原始流上传，避免 multipart 在低内存设备上触发 OOM
         *
         * @return ResultModel 导入操作结果
         */
        post("/import") {
            runCatchingExceptCancel {
                handleImport(call)
            }.onFailure {
                ServerLog.e("数据库导入失败：${it.message}", it)
                call.respond(ResultModel.error(500, "Database import failed: ${it.message}"))
            }
        }

        put("/import") {
            runCatchingExceptCancel {
                handleImport(call)
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