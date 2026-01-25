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
 * 提供日志的增删查改功能，支持分页查询、筛选和自动清理过期数据
 */
fun Route.logRoutes() {
    route("/log") {
        /**
         * GET /log/list - 获取日志列表
         * 支持分页查询和筛选，自动清理过期日志数据
         *
         * @param page 页码，默认为1
         * @param limit 每页条数，默认为10
         * @param app 应用筛选，空表示所有应用
         * @param levels 日志级别筛选，逗号分隔，空表示所有级别
         * @return ResultModel 包含日志列表数据
         */
        get("/list") {
            // 清理过期数据
            Db.get().logDao().clearOld()

            val page = call.request.queryParameters["page"]?.toInt() ?: 1
            val limit = call.request.queryParameters["limit"]?.toInt() ?: 10
            val offset = (page - 1) * limit

            val app = call.request.queryParameters["app"] ?: ""
            val levelsParam = call.request.queryParameters["levels"]
            val levels = if (levelsParam.isNullOrEmpty()) null else levelsParam.split(",")

            val logs = Db.get().logDao().loadPage(limit, offset, app, levels)
            
            call.respond(ResultModel.ok(logs))
        }

        /**
         * GET /log/apps - 获取所有应用列表
         * 用于筛选器的应用选择
         *
         * @return ResultModel 包含应用包名列表
         */
        get("/apps") {
            val apps = Db.get().logDao().getApps()
            call.respond(ResultModel.ok(apps))
        }

        /**
         * POST /log/add - 添加新日志
         *
         * @param body LogModel 日志数据
         * @return ResultModel 包含新创建的日志ID
         */
        post("/add") {
            val log = call.receive<LogModel>()
            val id = Db.get().logDao().insert(log)
            call.respond(ResultModel.ok(id))
        }

        /**
         * POST /log/addBatch - 批量添加日志
         *
         * @param body List<LogModel> 日志数据列表
         * @return ResultModel 包含新创建的日志ID列表
         */
        post("/addBatch") {
            // 使用 List 类型，Ktor 序列化对其支持更稳定
            val logs = call.receive<Array<LogModel>>()
            val ids = Db.get().logDao().insert(logs.toList())
            call.respond(ResultModel.ok(ids))
        }

        /**
         * POST /log/clear - 清空所有日志
         *
         * @return ResultModel 操作结果
         */
        post("/clear") {
            Db.get().logDao().clear()
            call.respond(ResultModel.ok(null))
        }
    }
} 