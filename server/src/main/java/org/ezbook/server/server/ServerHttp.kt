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

import fi.iki.elonen.NanoHTTPD
import org.ezbook.server.Server
import org.ezbook.server.Server.Companion.json
import org.ezbook.server.routes.LogRoute
import org.ezbook.server.routes.RuleRoute
import org.ezbook.server.routes.SettingRoute
import java.util.Locale

class ServerHttp(port:Int,threadCount:Int) : NanoHTTPD(port) {
    init {
        asyncRunner = BoundRunner(java.util.concurrent.Executors.newFixedThreadPool(threadCount))
    }
    override fun serve(session: IHTTPSession): Response {
        val method = session.method.name.uppercase(Locale.ROOT)
        val path = "$method ${session.uri}"
        return runCatching {
             when (path) {
                "GET /" -> json(200,"hello,欢迎使用自动记账", Server.versionCode,0)
                 // 日志列表
                "GET /log/list" -> LogRoute(session).list()
                    // 添加日志
                "POST /log/add" -> LogRoute(session).add()
                    // 清空日志
                "GET /log/clear" -> LogRoute(session).clear()
                 //--------------------------------------------
                 //规则列表
                "GET /rule/list" -> RuleRoute(session).list()
                    // 添加规则
                "POST /rule/add" -> RuleRoute(session).add()
                    // 删除规则
                "GET /rule/delete" -> RuleRoute(session).delete()
                    // 修改规则
                "POST /rule/update" -> RuleRoute(session).update()
                 // 获取app列表
                 "GET /rule/apps" -> RuleRoute(session).apps()

                 //-------------------------------
                 // 设置列表
                "GET /setting/get" -> SettingRoute(session).get()
                    // 添加设置
                "POST /setting/set" -> SettingRoute(session).set()


                else -> json(404,"Not Found",null,0)
            }
        }.getOrElse {
            it.printStackTrace()
            json(500, "Internal Server Error")
        }
    }
}