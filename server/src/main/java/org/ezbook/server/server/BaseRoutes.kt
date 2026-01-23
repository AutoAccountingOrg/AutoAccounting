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
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import org.ezbook.server.Server
import org.ezbook.server.models.ResultModel

/**
 * 基础路由配置
 * 提供应用的基本信息和健康检查接口
 */
fun Route.baseRoutes() {
    /**
     * GET / - 获取应用基本信息
     * 返回应用的欢迎信息和版本号
     *
     * @return ResultModel 包含欢迎信息和版本号
     */
    get("/") {
        call.respond(ResultModel.ok(Server.versionName))
    }

    /**
     * POST / - 获取应用基本信息（POST方式）
     * 与GET方式功能相同，用于兼容不同的客户端需求
     *
     * @return ResultModel 包含欢迎信息和版本号
     */
    post("/") { call.respond(ResultModel.ok(Server.versionName)) }
} 