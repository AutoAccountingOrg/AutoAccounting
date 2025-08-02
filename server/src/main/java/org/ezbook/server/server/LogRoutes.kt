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

import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.LogModel
import org.ezbook.server.models.ResultModel

/**
 * 日志管理路由配置
 * 提供日志的增删查改功能，支持分页查询和自动清理过期数据
 */
fun Route.logRoutes() {
    route("/log") {
        /**
         * GET /log/list - 获取日志列表
         * 支持分页查询，自动清理过期日志数据
         *
         * @param page 页码，默认为1
         * @param limit 每页条数，默认为10
         * @return ResultModel 包含日志列表数据
         */
        get("/list") {
            // 清理过期数据
            Db.get().logDao().clearOld()

            val page = call.request.queryParameters["page"]?.toInt() ?: 1
            val limit = call.request.queryParameters["limit"]?.toInt() ?: 10
            val offset = (page - 1) * limit

            val logs = Db.get().logDao().loadPage(limit, offset)
            call.respond(ResultModel(200, "OK", logs))
        }

        /**
         * POST /log/add - 添加新日志
         *
         * @param body LogModel 日志数据
         * @return ResultModel 包含新创建的日志ID
         */
        post("/add") {
            val log = call.receive(LogModel::class)
            val id = Db.get().logDao().insert(log)
            call.respond(ResultModel(200, "OK", id))
        }

        /**
         * POST /log/clear - 清空所有日志
         *
         * @return ResultModel 操作结果
         */
        post("/clear") {
            Db.get().logDao().clear()
            call.respond(ResultModel(200, "OK", null))
        }
    }
} 