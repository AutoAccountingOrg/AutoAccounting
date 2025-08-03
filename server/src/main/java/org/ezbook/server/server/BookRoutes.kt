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
import org.ezbook.server.Server
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.BookNameModel
import org.ezbook.server.models.ResultModel

/**
 * 账本管理路由配置
 * 提供记账账本的管理功能，包括账本列表和数据同步
 */
fun Route.bookRoutes() {
    route("/book") {
        /**
         * POST /book/list - 获取账本列表
         *
         * @return ResultModel 包含所有账本数据
         */
        post("/list") {
            val books = Db.get().bookNameDao().load()
            call.respond(ResultModel(200, "OK", books))
        }

        /**
         * POST /book/put - 批量更新账本数据
         * 用于同步外部系统的账本数据，并更新数据hash值
         *
         * @param md5 数据的MD5校验值，用于数据完整性验证
         * @param body Array<BookNameModel> 账本数据数组
         * @return ResultModel 包含操作结果
         */
        post("/put") {
            val md5 = call.request.queryParameters["md5"] ?: ""
            val books = call.receive<Array<BookNameModel>>()
            val result = Db.get().bookNameDao().put(books)

            // 更新账本数据的hash值
            setByInner(Setting.HASH_BOOK, md5)
            call.respond(ResultModel(200, "OK", result))
        }

        /**
         * POST /book/delete - 删除指定账本
         * 根据账本ID删除对应的账本记录
         *
         * @param body 包含id的JSON对象
         * @return ResultModel 操作结果
         */
        post("/delete") {
            val requestBody = call.receiveText()
            val json =
                com.google.gson.Gson().fromJson(requestBody, com.google.gson.JsonObject::class.java)
            val id = json?.get("id")?.asLong ?: 0
            Server.log("删除账本:$id")
            Db.get().bookNameDao().delete(id)
            call.respond(ResultModel(200, "OK", 0))
        }
    }
} 