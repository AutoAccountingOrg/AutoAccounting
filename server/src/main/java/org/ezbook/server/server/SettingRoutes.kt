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
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.SettingModel
import org.ezbook.server.models.ResultModel

/**
 * 系统设置路由配置
 * 提供应用设置的读取、保存和列表功能
 */
fun Route.settingRoutes() {
    route("/setting") {
        /**
         * POST /setting/get - 获取指定设置项的值
         *
         * @param key 设置项的键名
         * @return ResultModel 包含设置项的值，不存在时返回空字符串
         */
        post("/get") {
            val key = call.request.queryParameters["key"] ?: ""
            if (key.isEmpty()) {
                call.respond(ResultModel.error(400, "key is required"))
                return@post
            }

            val data = Db.get().settingDao().query(key)
            call.respond(ResultModel.ok(data?.value ?: ""))
        }

        /**
         * POST /setting/set - 设置指定键的值
         *
         * @param key 设置项的键名
         * @param body 设置项的值（通过请求体传递）
         * @return ResultModel 操作结果
         */
        post("/set") {
            val key = call.request.queryParameters["key"] ?: ""
            if (key.isEmpty()) {
                call.respond(ResultModel.error(400, "key is required"))
                return@post
            }

            val value = call.receiveText()
            setByInner(key, value)
            call.respond(ResultModel.ok("OK"))
        }

        /**
         * POST /setting/list - 获取所有设置项列表
         *
         * @return ResultModel 包含所有设置项的列表
         */
        post("/list") {
            call.respond(ResultModel.ok(Db.get().settingDao().load()))
        }
    }
}

/**
 * 内部设置保存函数
 * 根据键名是否存在决定是插入新记录还是更新现有记录
 *
 * @param key 设置项的键名
 * @param value 设置项的值
 */
/**
 * 以唯一键 + REPLACE 的方式写入设置
 * 依赖 SettingModel.key 的唯一索引，统一走 insert(onConflict = REPLACE)
 */
suspend fun setByInner(key: String, value: String) {
    Db.get().settingDao().insert(
        SettingModel().apply {
            this.key = key
            this.value = value
        }
    )
}