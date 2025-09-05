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
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.CategoryMapModel
import org.ezbook.server.models.ResultModel

/**
 * 分类映射管理路由配置
 * 提供分类名称映射功能，用于将不同来源的分类名称统一到标准分类
 */
fun Route.categoryMapRoutes() {
    route("/category/map") {
        /**
         * POST /category/map/delete - 删除指定分类映射
         *
         * @param body 包含id的JSON对象
         * @return ResultModel 包含删除的映射ID
         */
        post("/delete") {
            val requestBody = call.receiveText()
            val json =
                com.google.gson.Gson().fromJson(requestBody, com.google.gson.JsonObject::class.java)
            val id = json?.get("id")?.asLong ?: 0
            Db.get().categoryMapDao().delete(id)
            call.respond(ResultModel.ok(id))
        }

        /**
         * GET /category/map/list - 获取分类映射列表
         * 支持分页查询和搜索功能
         *
         * @param page 页码，默认为1（当limit=0时忽略分页）
         * @param limit 每页条数，默认为10，设为0时返回所有数据
         * @param search 搜索关键词，可选
         * @return ResultModel 包含分类映射列表数据
         */
        get("/list") {
            val limit = call.request.queryParameters["limit"]?.toInt() ?: 10

            if (limit == 0) {
                // 返回所有数据，不分页
                val mappings = Db.get().categoryMapDao().loadWithoutLimit()
                call.respond(ResultModel.ok(mappings))
                return@get
            }

            val page = call.request.queryParameters["page"]?.toInt() ?: 1
            val offset = (page - 1) * limit
            val search = call.request.queryParameters["search"]?.takeIf { it.isNotEmpty() }

            val mappings = Db.get().categoryMapDao().loadWithLimit(limit, offset, search)
            call.respond(ResultModel.ok(mappings))
        }

        /**
         * POST /category/map/put - 保存或更新分类映射
         * 根据分类名称自动判断是插入新映射还是更新现有映射
         *
         * @param body CategoryMapModel 分类映射数据
         * @return ResultModel 包含映射ID
         */
        post("/put") {
            val model = call.receive(CategoryMapModel::class)
            val existingMapping = Db.get().categoryMapDao().query(model.name)

            if (existingMapping == null) {
                model.id = Db.get().categoryMapDao().insert(model)
            } else {
                model.id = existingMapping.id
                Db.get().categoryMapDao().update(model)
            }

            call.respond(ResultModel.ok(model.id))
        }
    }
} 