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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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

            // 服务端去重策略：
            // 1) 仅对 creator == "system" 的规则执行
            // 2) 从 element 中提取 shopItem/shopName 的 content 值作为唯一键
            // 3) 两者都为空则不保存
            // 4) 若存在相同键的自动规则，则覆盖更新（使用其 id）
            if (model.creator == "system") {
                val (shopItemKey, shopNameKey) = extractShopKeys(model.element)
                if (shopItemKey.isEmpty() && shopNameKey.isEmpty()) {
                    call.respond(ResultModel.ok(0L))
                    return@post
                }
                val autoRules = Db.get().categoryRuleDao().loadByCreator(model.creator)
                val existed =
                    autoRules.firstOrNull { sameShopKeys(it.element, shopItemKey, shopNameKey) }
                if (existed != null) {
                    model.id = existed.id
                }
            }

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

/**
 * 从 element JSON 提取 shopItem/shopName 的 content 值
 */
private fun extractShopKeys(elementJson: String): Pair<String, String> {
    return try {
        val listType = object : TypeToken<MutableList<HashMap<String, Any>>>() {}.type
        val list: MutableList<HashMap<String, Any>> =
            Gson().fromJson(elementJson, listType) ?: mutableListOf()
        if (list.isNotEmpty()) list.removeAt(list.lastIndex)
        var item = ""
        var name = ""
        for (m in list) {
            val type = m["type"]?.toString() ?: continue
            val content = m["content"]?.toString() ?: ""
            if (type == "shopItem") item = content
            if (type == "shopName") name = content
        }
        item to name
    } catch (e: Exception) {
        "" to ""
    }
}

/**
 * 判断 element 是否与指定 shopItem/shopName 键相同
 */
private fun sameShopKeys(elementJson: String, shopItem: String, shopName: String): Boolean {
    val (item, name) = extractShopKeys(elementJson)
    val itemOk = if (shopItem.isEmpty()) true else (item == shopItem)
    val nameOk = if (shopName.isEmpty()) true else (name == shopName)
    return itemOk && nameOk
} 