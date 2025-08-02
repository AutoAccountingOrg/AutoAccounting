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
import org.ezbook.server.db.model.AssetsMapModel
import org.ezbook.server.models.ResultModel

/**
 * 资产映射管理路由配置
 * 提供资产名称映射功能，用于将不同来源的资产名称统一到标准资产账户
 */
fun Route.assetsMapRoutes() {
    route("/assets/map") {
        /**
         * GET /assets/map/list - 获取资产映射列表
         * 支持分页查询和搜索功能
         *
         * @param page 页码，默认为1（当limit=0时忽略分页）
         * @param limit 每页条数，默认为10，设为0时返回所有数据
         * @param search 搜索关键词，可选
         * @return ResultModel 包含资产映射列表数据
         */
        get("/list") {
            val limit = call.request.queryParameters["limit"]?.toInt() ?: 10

            if (limit == 0) {
                // 返回所有数据，不分页
                val mappings = Db.get().assetsMapDao().loadWithoutLimit()
                call.respond(ResultModel(200, "OK", mappings))
                return@get
            }

            val page = call.request.queryParameters["page"]?.toInt() ?: 1
            val offset = (page - 1) * limit
            val search = call.request.queryParameters["search"]?.takeIf { it.isNotEmpty() }

            val mappings = Db.get().assetsMapDao().loadWithLimit(limit, offset, search)
            call.respond(ResultModel(200, "OK", mappings))
        }

        /**
         * POST /assets/map/put - 保存或更新资产映射
         * 根据资产名称自动判断是插入新映射还是更新现有映射
         *
         * @param body AssetsMapModel 资产映射数据
         * @return ResultModel 包含映射ID
         */
        post("/put") {
            val model = call.receive(AssetsMapModel::class)
            val existingMapping = Db.get().assetsMapDao().query(model.name)

            if (existingMapping == null) {
                model.id = Db.get().assetsMapDao().insert(model)
            } else {
                model.id = existingMapping.id
                Db.get().assetsMapDao().update(model)
            }

            call.respond(ResultModel(200, "OK", model.id))
        }

        /**
         * POST /assets/map/delete - 删除指定资产映射
         *
         * @param body 包含id的JSON对象
         * @return ResultModel 包含删除的映射ID
         */
        post("/delete") {
            val requestBody = call.receiveText()
            val json =
                com.google.gson.Gson().fromJson(requestBody, com.google.gson.JsonObject::class.java)
            val id = json?.get("id")?.asLong ?: 0
            Db.get().assetsMapDao().delete(id)
            call.respond(ResultModel(200, "OK", id))
        }

        /**
         * GET /assets/map/get - 获取指定名称的资产映射
         *
         * @param name 资产名称
         * @return ResultModel 包含映射信息
         */
        get("/get") {
            val name = call.request.queryParameters["name"] ?: ""
            val mapping = Db.get().assetsMapDao().query(name)
            call.respond(ResultModel(200, "OK", mapping))
        }

        /**
         * GET /assets/map/empty - 获取未映射的资产列表
         * 返回尚未建立映射关系的资产名称
         *
         * @return ResultModel 包含未映射的资产列表
         */
        get("/empty") {
            val emptyMappings = Db.get().assetsMapDao().empty()
            call.respond(ResultModel(200, "OK", emptyMappings))
        }
    }
} 