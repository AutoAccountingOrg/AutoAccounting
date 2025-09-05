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

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.AppDataModel
import org.ezbook.server.models.ResultModel

/**
 * 应用数据管理路由配置
 * 提供应用原始数据的存储、查询和统计功能，用于调试和规则优化
 */
fun Route.dataRoutes() {
    route("/data") {
        /**
         * GET /data/list - 获取应用数据列表
         * 支持分页查询和多条件筛选，自动清理过期数据
         *
         * @param page 页码，默认为1
         * @param limit 每页条数，默认为10
         * @param app 应用筛选条件
         * @param type 数据类型筛选条件
         * @param match 是否只显示已匹配的数据
         * @param search 搜索关键词
         * @return ResultModel 包含应用数据列表
         */
        get("/list") {
            val page = call.request.queryParameters["page"]?.toInt() ?: 1
            val limit = call.request.queryParameters["limit"]?.toInt() ?: 10
            val offset = (page - 1) * limit

            val app = call.request.queryParameters["app"] ?: ""
            val type = call.request.queryParameters["type"]?.takeIf { it.isNotEmpty() }
            val match: Boolean? = when (val matchParam = call.request.queryParameters["match"]) {
                null, "" -> null
                else -> matchParam.toBoolean()
            }
            val search = call.request.queryParameters["search"]?.takeIf { it.isNotEmpty() }

            val data = Db.get().dataDao().load(limit, offset, app, match, type, search)
            call.respond(ResultModel.ok(data))
        }

        /**
         * GET /data/clear - 清空所有应用数据
         *
         * @return ResultModel 操作结果
         */
        get("/clear") {
            Db.get().dataDao().clear()
            call.respond(ResultModel.ok("OK"))
        }

        /**
         * GET /data/clearOld - 清理旧数据（只保留最新2000条）
         * 独立的数据清理接口，不会在查询时自动触发
         *
         * @return ResultModel 操作结果
         */
        get("/clearOld") {
            Db.get().dataDao().clearOld()
            call.respond(ResultModel.ok("OK"))
        }

        /**
         * GET /data/apps - 获取应用列表及统计
         * 返回每个应用的数据条数统计
         *
         * @return ResultModel 包含应用名称和对应的数据条数映射
         */
        get("/apps") {
            val apps = Db.get().dataDao().queryApps()
            val appCounts = apps.groupingBy { it }.eachCount()
            call.respond(ResultModel.ok(appCounts))
        }

        /**
         * POST /data/put - 保存或更新应用数据
         * 根据数据ID自动判断是插入新数据还是更新现有数据
         *
         * @param body AppDataModel 应用数据
         * @return ResultModel 操作结果
         */
        post("/put") {
            val data = call.receive(AppDataModel::class)
            if (data.id == 0L) {
                Db.get().dataDao().insert(data)
            } else {
                Db.get().dataDao().update(data)
            }
            call.respond(ResultModel.ok("OK"))
        }

        /**
         * POST /data/delete - 删除指定应用数据
         *
         * @param body 包含id的JSON对象
         * @return ResultModel 操作结果
         */
        post("/delete") {
            val requestBody = call.receiveText()
            val json = Gson().fromJson(requestBody, JsonObject::class.java)
            val id = json?.get("id")?.asLong ?: 0
            Db.get().dataDao().delete(id)
            call.respond(ResultModel.ok("OK"))
        }
    }
} 