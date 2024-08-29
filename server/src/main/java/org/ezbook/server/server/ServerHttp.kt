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

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import org.ezbook.server.Server
import org.ezbook.server.Server.Companion.json
import org.ezbook.server.routes.AppDataRoute
import org.ezbook.server.routes.JsRoute
import org.ezbook.server.routes.LogRoute
import org.ezbook.server.routes.RuleRoute
import org.ezbook.server.routes.SettingRoute
import java.util.Locale

class ServerHttp(port:Int,threadCount:Int,private val context: Context) : NanoHTTPD(port) {
    init {
        asyncRunner = BoundRunner(java.util.concurrent.Executors.newFixedThreadPool(threadCount))
    }
    override fun serve(session: IHTTPSession): Response {

       // val path = "$method ${session.uri}"
        return runCatching {
             when (session.uri) {
                "/" -> json(200,"hello,欢迎使用自动记账", Server.versionCode,0)
                 // 日志列表
                "/log/list" -> LogRoute(session).list()
                    // 添加日志
                "/log/add" -> LogRoute(session).add()
                    // 清空日志
                "/log/clear" -> LogRoute(session).clear()
                 //--------------------------------------------
                 //规则列表
                "/rule/list" -> RuleRoute(session).list()
                    // 添加规则
                "/rule/add" -> RuleRoute(session).add()
                    // 删除规则
                "/rule/delete" -> RuleRoute(session).delete()
                    // 修改规则
                "/rule/update" -> RuleRoute(session).update()
                 // 获取app列表
                 "/rule/apps" -> RuleRoute(session).apps()
                 // system
                 "/rule/system" -> RuleRoute(session).system()
                 //-------------------------------
                 // 设置列表
                "/setting/get" -> SettingRoute(session).get()
                    // 添加设置
                "/setting/set" -> SettingRoute(session).set()
                 //--------------------------------
                 // js 分析
                "/js/analysis" -> JsRoute(session,context).analysis()
                 // App Data
                 "/data/list" -> AppDataRoute(session).list()
                 "/data/clear" -> AppDataRoute(session).clear()
                else -> json(404,"Not Found",null,0)
            }
        }.getOrElse {
            it.printStackTrace()
            json(500, "Internal Server Error")
        }
    }
}