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
import io.ktor.request.receiveText
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.SettingModel
import org.ezbook.server.models.ResultModel

class SettingRoute(private val session: ApplicationCall) {
    private val params: Parameters = session.request.queryParameters

    /**
     * 获取设置
     */
    suspend fun get(): ResultModel {
        val key = params["key"] ?: ""
        if (key === "") {
            return ResultModel(400, "key is required")
        }
        val data = Db.get().settingDao().query(key)

        return ResultModel(200, "OK", data?.value ?: "")
    }

    /**
     * 设置
     */
    suspend fun set(): ResultModel {
        val key = params["key"] ?: ""
        if (key === "") {
            return ResultModel(400, "key is required")
        }
        val value = session.receiveText()
        setByInner(key, value)
        return ResultModel(200, "OK")
    }

    suspend fun list(): ResultModel {
        return ResultModel(200, "OK", Db.get().settingDao().load())
    }

    companion object {
        suspend fun setByInner(key: String, value: String) {
            val model = SettingModel()
            model.key = key
            model.value = value

            val data = Db.get().settingDao().query(key)

            if (data != null) {
                model.id = data.id
                Db.get().settingDao().update(model)
            } else {
                Db.get().settingDao().insert(model)
            }
        }
    }

}