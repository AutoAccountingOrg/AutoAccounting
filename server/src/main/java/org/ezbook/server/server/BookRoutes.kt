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
import org.ezbook.server.db.model.BookNameModel
import org.ezbook.server.models.ResultModel
import org.ezbook.server.log.ServerLog
import org.ezbook.server.tools.SettingUtils

/**
 * 账本管理路由配置
 * 提供记账账本的管理功能，包括账本列表和数据同步
 */
fun Route.bookRoutes() {
    route("/book") {
        /**
         * GET /book/get - 根据ID获取单个账本
         * @param id 账本ID（query）
         */
        get("/get") {
            val requestName = call.request.queryParameters["name"].orEmpty()
            val book = resolveBookByNameOrDefault(requestName)
            call.respond(ResultModel.ok(book))
        }

        /**
         * POST /book/list - 获取账本列表
         *
         * @return ResultModel 包含所有账本数据
         */
        post("/list") {
            // 加载账本列表
            var books = Db.get().bookNameDao().load()

            // 若无任何账本，则创建并插入一个默认账本，确保用户开箱即用
            if (books.isEmpty()) {
                val defaultName = SettingUtils.bookName()
                val defaultBook = BookNameModel().apply { this.name = defaultName }
                Db.get().bookNameDao().insertOrReplace(defaultBook)
                // 重新加载以返回包含默认账本的完整列表
                books = Db.get().bookNameDao().load()
            }

            call.respond(ResultModel.ok(books))
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
            call.respond(ResultModel.ok(result))
        }

        /**
         * POST /book/add - 新增账本
         */
        post("/add") {
            val model = call.receive(BookNameModel::class)
            model.id = 0
            val id = Db.get().bookNameDao().insertOrReplace(model)
            call.respond(ResultModel.ok(id))
        }

        /**
         * POST /book/update - 更新账本
         */
        post("/update") {
            val model = call.receive(BookNameModel::class)
            val updated = Db.get().bookNameDao().insertOrReplace(model)
            call.respond(ResultModel.ok(updated))
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
            ServerLog.d("删除账本：$id")
            Db.get().bookNameDao().delete(id)
            call.respond(ResultModel.ok(0))
        }

        /**
         * GET /book/default - 获取默认账本（来自设置）
         */
        get("/default") {
            val book = resolveBookByNameOrDefault(SettingUtils.bookName())
            call.respond(ResultModel.ok(book))
        }
    }
}

/**
 * 账本解析（含默认回退）
 *
 * 优先级：精确名称 → 默认账本名称 → 列表第一个 → 空表时返回默认名的新对象
 */
suspend fun resolveBookByNameOrDefault(name: String): BookNameModel {
    val books = Db.get().bookNameDao().load()
    val defaultName = SettingUtils.bookName()

    if (books.isEmpty()) {
        return BookNameModel().apply { this.name = defaultName }
    }

    if (name.isNotEmpty()) {
        books.firstOrNull { it.name == name }?.let { return it }
    }

    books.firstOrNull { it.name == defaultName }?.let { return it }

    return books.first()
}