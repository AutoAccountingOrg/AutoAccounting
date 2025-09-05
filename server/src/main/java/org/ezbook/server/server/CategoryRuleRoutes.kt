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
import io.ktor.routing.post
import io.ktor.routing.route
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.CategoryRuleModel
import org.ezbook.server.models.ResultModel

/**
 * 分类规则管理路由配置
 * 提供自动分类规则的管理功能，用于根据商户、商品等信息自动识别分类
 */
fun Route.categoryRuleRoutes() {
    route("/category/rule") {
        /**
         * POST /category/rule/list - 获取分类规则列表
         * 支持分页查询
         *
         * @param page 页码，默认为1
         * @param limit 每页条数，默认为10
         * @return ResultModel 包含分类规则列表数据
         */
        post("/list") {
            val page = call.request.queryParameters["page"]?.toInt() ?: 1
            val limit = call.request.queryParameters["limit"]?.toInt() ?: 10
            val offset = (page - 1) * limit

            val rules = Db.get().categoryRuleDao().load(limit, offset)
            call.respond(ResultModel.ok(rules))
        }

        /**
         * POST /category/rule/put - 保存或更新分类规则
         * 根据规则ID自动判断是插入新规则还是更新现有规则
         *
         * @param body CategoryRuleModel 分类规则数据
         * @return ResultModel 包含规则ID
         */
        post("/put") {
            val model = call.receive(CategoryRuleModel::class)

            if (model.id == 0L) {
                model.id = Db.get().categoryRuleDao().insert(model)
            } else {
                Db.get().categoryRuleDao().update(model)
            }

            call.respond(ResultModel.ok(model.id))
        }

        /**
         * POST /category/rule/delete - 删除指定分类规则
         *
         * @param body 包含id的JSON对象
         * @return ResultModel 包含删除的规则ID
         */
        post("/delete") {
            val requestBody = call.receiveText()
            val json =
                com.google.gson.Gson().fromJson(requestBody, com.google.gson.JsonObject::class.java)
            val id = json?.get("id")?.asLong ?: 0
            Db.get().categoryRuleDao().delete(id)
            call.respond(ResultModel.ok(id))
        }
    }
} 