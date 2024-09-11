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
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.RoomDatabase
import org.ezbook.server.Server
import org.ezbook.server.Server.Companion.json
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.BookBillModel
import org.ezbook.server.routes.AppDataRoute
import org.ezbook.server.routes.AssetsMapRoute
import org.ezbook.server.routes.AssetsRoute
import org.ezbook.server.routes.BillRoute
import org.ezbook.server.routes.BookBillRoute
import org.ezbook.server.routes.BookNameRoute
import org.ezbook.server.routes.CategoryMapRoute
import org.ezbook.server.routes.CategoryRoute
import org.ezbook.server.routes.CategoryRuleRoute
import org.ezbook.server.routes.DatabaseRoute
import org.ezbook.server.routes.JsRoute
import org.ezbook.server.routes.LogRoute
import org.ezbook.server.routes.RuleRoute
import org.ezbook.server.routes.SettingRoute
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.NanoHTTPD
import org.nanohttpd.protocols.http.response.Response


class ServerHttp(port: Int, private val context: Context) : NanoHTTPD(port) {
    init {
        asyncRunner = BoundRunner()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun handle(session: IHTTPSession?): Response {
        return  runCatching {
            val uri = session!!.uri.replace("//", "/")
            when (uri) {
                "/" -> json(200, "hello,欢迎使用自动记账", Server.versionCode)
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
                // 设置列表
                "/setting/list" -> SettingRoute(session).list()
                //--------------------------------
                // js 分析
                "/js/analysis" -> JsRoute(session, context).analysis()
                // js 代码
                "/js/run" -> JsRoute(session, context).run()
                // App Data
                "/data/list" -> AppDataRoute(session).list()
                "/data/clear" -> AppDataRoute(session).clear()
                "/data/apps" -> AppDataRoute(session).apps()
                "/data/put" -> AppDataRoute(session).put()
                "/data/delete" -> AppDataRoute(session).delete()
                // 资产
                "/assets/list" -> AssetsRoute(session).list()
                "/assets/put" -> AssetsRoute(session).put()
                "/assets/get" -> AssetsRoute(session).get()
                // 资产映射
                "/assets/map/list" -> AssetsMapRoute(session).list()
                "/assets/map/put" -> AssetsMapRoute(session).put()
                "/assets/map/delete" -> AssetsMapRoute(session).delete()
                // 账本
                "/book/list" -> BookNameRoute(session).list()
                "/book/put" -> BookNameRoute(session).put()
                //分类
                "/category/list" -> CategoryRoute(session).list()
                "/category/put" -> CategoryRoute(session).put()
                "/category/get" -> CategoryRoute(session).get()
                // 分类映射
                "/category/map/delete" -> CategoryMapRoute(session).delete()
                "/category/map/list" -> CategoryMapRoute(session).list()
                "/category/map/put" -> CategoryMapRoute(session).put()

                // 自定义分类规则
                "/category/rule/list" -> CategoryRuleRoute(session).list()
                "/category/rule/put" -> CategoryRuleRoute(session).put()
                "/category/rule/delete" -> CategoryRuleRoute(session).delete()

                // 账单列表
                "/bill/list" -> BillRoute(session).list()
                "/bill/put" -> BillRoute(session).put()
                "/bill/remove" -> BillRoute(session).remove()
                // 同步未同步的账单
                "/bill/sync/list" -> BillRoute(session).sync()
                // 来自记账软件的账单
                "/bill/book/list" -> BookBillRoute(session).list()
                "/bill/book/put" -> BookBillRoute(session).put()

                // 备份
                "/db/export" -> DatabaseRoute(session, context).exportDb()
                "/db/import" -> DatabaseRoute(session, context).importDb()

                else -> {
                    runCatching {

                        val clazz = javaClass.classLoader!!.loadClass("net.ankio.data.App")


                        val constructor = clazz.declaredConstructors.first()


                        val app = constructor.newInstance(Db.get())

                        val method = clazz.declaredMethods.first()
                        method.invoke(app, session) as Response

                    }.getOrElse {
                        it.printStackTrace()
                        json(404, it.message?: "Not Found", null)
                    }

                }
            }
        }.getOrElse {
            it.printStackTrace()
            json(500, "Internal Server Error: ${it.message}")
        }
    }
}