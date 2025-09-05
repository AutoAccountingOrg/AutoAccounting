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
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.CategoryModel
import org.ezbook.server.models.ResultModel

/**
 * 分类管理路由配置
 * 提供收支分类的管理功能，支持多级分类和按账本、类型查询
 */
fun Route.categoryRoutes() {
    route("/category") {
        /**
         * GET /category/list - 获取分类列表
         * 根据账本和类型获取对应的分类数据
         *
         * @param book 账本名称，必填
         * @param type 分类类型（收入/支出），必填
         * @param parent 父分类ID，默认为-1（顶级分类）
         * @return ResultModel 包含分类列表数据
         */
        get("/list") {
            val book = call.request.queryParameters["book"] ?: ""


            val type = call.request.queryParameters["type"] ?: ""
            if (type.isEmpty()) {
                call.respond(ResultModel.error(400, "type is empty"))
                return@get
            }

            val parent = call.request.queryParameters["parent"] ?: "-1"
            val categories = Db.get().categoryDao().load(book, type, parent)
            call.respond(ResultModel.ok(categories))
        }

        /**
         * POST /category/put - 批量更新分类数据
         * 用于同步外部系统的分类数据，并更新数据hash值
         *
         * @param md5 数据的MD5校验值，用于数据完整性验证
         * @param body Array<CategoryModel> 分类数据数组
         * @return ResultModel 包含操作结果
         */
        post("/put") {
            val md5 = call.request.queryParameters["md5"] ?: ""
            val categories = call.receive<Array<CategoryModel>>()
            val result = Db.get().categoryDao().put(categories)

            // 更新分类数据的hash值
            setByInner(Setting.HASH_CATEGORY, md5)
            call.respond(ResultModel.ok(result))
        }

        /**
         * GET /category/get - 根据名称获取分类信息
         *
         * @param book 账本名称，可选
         * @param type 分类类型，可选
         * @param name 分类名称，必填
         * @return ResultModel 包含分类详细信息
         */
        get("/get") {
            val book = call.request.queryParameters["book"]?.takeIf { it.isNotEmpty() }
            val type = call.request.queryParameters["type"]?.takeIf { it.isNotEmpty() }
            val name = call.request.queryParameters["name"] ?: ""

            if (name.isEmpty()) {
                call.respond(ResultModel.error(400, "name is empty"))
                return@get
            }

            val category = Db.get().categoryDao().getByName(book, type, name)
            call.respond(ResultModel.ok(category))
        }

        /**
         * GET /category/getById - 根据ID获取分类信息
         *
         * @param id 分类ID，必填
         * @return ResultModel 包含分类详细信息
         */
        get("/getById") {
            val id = call.request.queryParameters["id"]?.toLongOrNull()

            if (id == null || id <= 0) {
                call.respond(ResultModel.error(400, "id is invalid"))
                return@get
            }

            val category = Db.get().categoryDao().getById(id)
            call.respond(ResultModel.ok(category))
        }

        /**
         * POST /category/save - 保存或更新分类
         * 根据分类ID自动判断是插入新分类还是更新现有分类
         *
         * @param body CategoryModel 分类数据
         * @return ResultModel 包含分类ID
         */
        post("/save") {
            val category = call.receive<CategoryModel>()

            val resultId = if (category.id == 0L) {
                // 新建分类
                category.id = Db.get().categoryDao().insert(category)
                category.remoteId = category.id.toString()
                Db.get().categoryDao().update(category)
                category.id
            } else {
                // 更新分类
                val updateCount = Db.get().categoryDao().update(category)
                if (updateCount > 0) category.id else 0L
            }

            call.respond(ResultModel.ok(resultId))
        }

        /**
         * POST /category/delete - 删除指定分类
         *
         * @param body 包含id的JSON对象
         * @return ResultModel 包含删除的分类ID
         */
        post("/delete") {
            val requestBody = call.receiveText()
            val json =
                com.google.gson.Gson().fromJson(requestBody, com.google.gson.JsonObject::class.java)
            val id = json?.get("id")?.asLong ?: 0

            if (id <= 0) {
                call.respond(ResultModel.error(400, "id is invalid"))
                return@post
            }

            val deleteCount = Db.get().categoryDao().delete(id)
            val resultId = if (deleteCount > 0) id else 0L
            call.respond(ResultModel.ok(resultId))
        }
    }
}