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
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.AssetsModel
import org.ezbook.server.models.ResultModel

/**
 * 资产管理路由配置
 * 提供资产账户的管理功能，包括账户列表、数据同步和单个账户查询
 */
fun Route.assetsRoutes() {
    route("/assets") {
        /**
         * GET /assets/list - 获取资产账户列表
         *
         * @return ResultModel 包含所有资产账户数据
         */
        get("/list") {
            val assets = Db.get().assetsDao().load()
            call.respond(ResultModel(200, "OK", assets))
        }

        /**
         * POST /assets/put - 批量更新资产数据
         * 用于同步外部系统的资产数据，并更新数据hash值
         *
         * @param md5 数据的MD5校验值，用于数据完整性验证
         * @param body Array<AssetsModel> 资产数据数组
         * @return ResultModel 包含操作结果
         */
        post("/put") {
            val md5 = call.request.queryParameters["md5"] ?: ""
            val assets = call.receive<Array<AssetsModel>>()
            val result = Db.get().assetsDao().put(assets)

            // 更新资产数据的hash值
            setByInner(Setting.HASH_ASSET, md5)
            call.respond(ResultModel(200, "OK", result))
        }

        /**
         * GET /assets/get - 获取指定名称的资产信息
         *
         * @param name 资产账户名称
         * @return ResultModel 包含指定资产的详细信息
         */
        get("/get") {
            val name = call.request.queryParameters["name"] ?: ""
            val asset = Db.get().assetsDao().query(name)
            call.respond(ResultModel(200, "OK", asset))
        }
    }
} 