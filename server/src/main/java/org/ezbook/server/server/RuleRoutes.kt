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
import org.ezbook.server.db.model.RuleModel
import org.ezbook.server.models.ResultModel

/**
 * 规则管理路由配置
 * 提供账单解析规则的增删改查功能，支持分页、筛选和应用统计
 */
fun Route.ruleRoutes() {
    route("/rule") {
        /**
         * GET /rule/list - 获取规则列表
         * 支持分页查询和多条件筛选
         *
         * @param page 页码，默认为1
         * @param limit 每页条数，默认为10
         * @param app 应用筛选条件
         * @param creator 创建者筛选条件
         * @param type 规则类型筛选条件
         * @param search 搜索关键词
         * @return ResultModel 包含规则列表数据
         */
        get("/list") {
            val page = call.request.queryParameters["page"]?.toInt() ?: 1
            val limit = call.request.queryParameters["limit"]?.toInt() ?: 10
            val offset = (page - 1) * limit

            val app = call.request.queryParameters["app"]?.takeIf { it.isNotEmpty() }
            val creator = call.request.queryParameters["creator"] ?: ""
            val type = call.request.queryParameters["type"]?.takeIf { it.isNotEmpty() }
            val search = call.request.queryParameters["search"]?.takeIf { it.isNotEmpty() }

            val rules =
                Db.get().ruleDao().loadByAppAndFilters(limit, offset, app, type, search, creator)
            call.respond(ResultModel.ok(rules))
        }

        /**
         * POST /rule/add - 添加新规则
         *
         * @param body RuleModel 规则数据
         * @return ResultModel 包含新创建的规则ID
         */
        post("/add") {
            val data = call.receive(RuleModel::class)
            val id = Db.get().ruleDao().insert(data)
            call.respond(ResultModel.ok(id))
        }

        /**
         * POST /rule/delete - 删除指定规则
         *
         * @param body 包含id的JSON对象
         * @return ResultModel 操作结果
         */
        post("/delete") {
            val requestBody = call.receiveText()
            val json =
                com.google.gson.Gson().fromJson(requestBody, com.google.gson.JsonObject::class.java)
            val id = json?.get("id")?.asInt ?: 0
            Db.get().ruleDao().delete(id)
            call.respond(ResultModel.ok("OK"))
        }

        /**
         * POST /rule/update - 更新现有规则
         *
         * @param body RuleModel 规则数据
         * @return ResultModel 包含更新后的规则ID
         */
        post("/update") {
            val data = call.receive(RuleModel::class)
            Db.get().ruleDao().update(data)
            // 返回明确字符串，避免客户端按字符串解析时遇到对象结构
            call.respond(ResultModel.ok("OK"))
        }

        /**
         * GET /rule/apps - 获取应用列表及统计
         * 返回每个应用的规则数量统计
         *
         * @return ResultModel 包含应用名称和对应的规则数量映射
         */
        get("/apps") {
            val apps = Db.get().ruleDao().queryApps()
            val appCounts = apps.groupingBy { it }.eachCount()
            call.respond(ResultModel.ok(appCounts))
        }

        /**
         * GET /rule/system - 获取系统规则
         * 根据规则名称获取系统预置规则
         *
         * @param name 规则名称
         * @return ResultModel 包含系统规则数据
         */
        get("/system") {
            val name = call.request.queryParameters["name"] ?: ""
            val rules = Db.get().ruleDao().loadSystemRule(name)
            call.respond(ResultModel.ok(rules))
        }

        /**
         * POST /rule/deleteSystemRule - 清理过期系统规则
         * 删除5分钟前创建的系统规则，用于清理临时规则
         *
         * @return ResultModel 操作结果
         */
        post("/deleteSystemRule") {
            val timeoutMs = System.currentTimeMillis() - 5 * 60 * 1000 // 5分钟前
            Db.get().ruleDao().deleteSystemRule(timeoutMs)
            call.respond(ResultModel.ok("OK"))
        }

        /**
         * POST /rule/put - 智能保存规则
         * 根据规则ID自动判断是插入新规则还是更新现有规则
         *
         * @param body RuleModel 规则数据
         * @return ResultModel 包含规则ID
         */
        post("/put") {
            val data = call.receive(RuleModel::class)
            val id = if (data.id == 0) {
                Db.get().ruleDao().insert(data)
            } else {
                Db.get().ruleDao().update(data)
                data.id
            }
            call.respond(ResultModel.ok(id))
        }
    }
} 