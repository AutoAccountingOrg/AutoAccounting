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

package org.ezbook.server.routes

import io.ktor.application.ApplicationCall
import io.ktor.http.Parameters
import io.ktor.request.receive
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.RuleModel
import org.ezbook.server.models.ResultModel

class RuleRoute(private val session: ApplicationCall) {
    private val params: Parameters = session.request.queryParameters
    /**
     * 获取规则列表
     */
    fun list(): ResultModel {
        val page = params["page"]?.toInt() ?: 1
        val limit = params["limit"]?.toInt() ?: 10

        val app = params["app"] ?: ""
        var type: String? = params["type"]?: ""
        var search: String? = params["search"]?: ""

        val offset = (page - 1) * limit

        if (type == "") type = null

        if (search == "") search = null

        val logs = Db.get().ruleDao().loadByAppAndFilters(limit, offset, app, type, search)
        
        return ResultModel(200, "OK", logs)
    }

    /**
     * 添加规则，
     */
    suspend fun add(): ResultModel {
        val data = session.receive(RuleModel::class)
        val id = Db.get().ruleDao().insert(data)
        return ResultModel(200, "OK", id)
    }

    /**
     * 更新规则
     */
    suspend fun update(): ResultModel {
        val data = session.receive(RuleModel::class)
        val id = Db.get().ruleDao().update(data)
        return ResultModel(200, "OK", id)
    }

    /**
     * 删除规则
     */
    fun delete(): ResultModel {
        val id = params["id"]?.toInt() ?: 0
        Db.get().ruleDao().delete(id)
        return ResultModel(200, "OK")
    }

    /**
     * 获取app列表
     */
    fun apps(): ResultModel {
        val apps = Db.get().ruleDao().queryApps()
        val map = hashMapOf<String, Int>()
        apps.forEach {
            if (it !in map) {
                map[it] = 1
            } else {
                map[it] = map[it]!! + 1
            }
        }
        return ResultModel(200, "OK", map)
    }

    /**
     * 根据名称获取其中一个规则
     */
    fun system(): ResultModel {
        val name = params["name"] ?: ""
        val rules = Db.get().ruleDao().loadSystemRule(name)
        return ResultModel(200, "OK", rules)
    }

    /**
     * 删除超时未更新的系统规则
     */
    fun deleteTimeoutSystem(): ResultModel {
        // 删除5分钟前的系统规则
        Db.get().ruleDao().deleteSystemRule(System.currentTimeMillis() - 300 * 1000)
        return ResultModel(200, "OK")
    }

    /**
     * 更新规则
     */
    suspend fun put(): ResultModel {
        val data = session.receive(RuleModel::class)
        if (data.id == 0) {
            val id = Db.get().ruleDao().insert(data)
            return ResultModel(200, "OK", id)
        } else {
            val id = Db.get().ruleDao().update(data)
            return ResultModel(200, "OK", id)
        }
    }
}
