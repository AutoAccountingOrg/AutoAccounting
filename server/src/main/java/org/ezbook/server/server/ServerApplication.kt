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
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import org.ezbook.server.Server
import org.ezbook.server.models.ResultModel
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

fun Application.module(context: Context) {
    install(StatusPages) {
        exception<Throwable> { cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ResultModel(500, cause.message ?: "")
            )
            Server.log(cause.message ?: "")
            Server.log(cause)
        }
        status(HttpStatusCode.NotFound) {
            call.respond(
                HttpStatusCode.NotFound,
                ResultModel(404, "Not Found")
            )
        }
    }
    install(ContentNegotiation) {
        gson()
    }


    routing {

        get("/") {
            call.respond(
                ResultModel(200, "欢迎使用自动记账", Server.versionName)
            )
        }
        post("/") {
            call.respond(
                ResultModel(200, "欢迎使用自动记账", Server.versionName)
            )
        }
        route("/log") {
            post("/list") {
                call.respond(LogRoute(call).list())
            }
            post("/add") {
                call.respond(LogRoute(call).add())
            }
            post("/clear") {
                call.respond(LogRoute(call).clear())
            }
        }

        route("/rule") {
            post("/list") {
                call.respond(RuleRoute(call).list())
            }
            post("/add") {
                call.respond(RuleRoute(call).add())
            }
            post("/delete") {
                call.respond(RuleRoute(call).delete())
            }
            post("/update") {
                call.respond(RuleRoute(call).update())
            }
            post("/apps") {
                call.respond(RuleRoute(call).apps())
            }
            post("/system") {
                call.respond(RuleRoute(call).system())
            }
            post("/deleteSystemRule") {
                call.respond(RuleRoute(call).deleteTimeoutSystem())
            }
            post("/put") {
                call.respond(RuleRoute(call).put())
            }
        }

        route("/setting") {
            post("/get") {
                call.respond(SettingRoute(call).get())
            }
            post("/set") {
                call.respond(SettingRoute(call).set())
            }
            post("/list") {
                call.respond(SettingRoute(call).list())
            }
        }

        route("/js") {
            post("/analysis") {
                call.respond(JsRoute(call, context).analysis())
            }
            post("/run") {
                call.respond(JsRoute(call, context).run())
            }
        }

        route("/data") {
            post("/list") {
                call.respond(AppDataRoute(call).list())
            }
            post("/clear") {
                call.respond(AppDataRoute(call).clear())
            }
            post("/apps") {
                call.respond(AppDataRoute(call).apps())
            }
            post("/put") {
                call.respond(AppDataRoute(call).put())
            }
            post("/delete") {
                call.respond(AppDataRoute(call).delete())
            }
        }

        route("/assets") {
            post("/list") {
                call.respond(AssetsRoute(call).list())
            }
            post("/put") {
                call.respond(AssetsRoute(call).put())
            }
            post("/get") {
                call.respond(AssetsRoute(call).get())
            }
        }

        route("/assets/map") {
            post("/list") {
                call.respond(AssetsMapRoute(call).list())
            }
            post("/put") {
                call.respond(AssetsMapRoute(call).put())
            }
            post("/delete") {
                call.respond(AssetsMapRoute(call).delete())
            }
            post("/get") {
                call.respond(AssetsMapRoute(call).get())
            }
        }

        route("/book") {
            post("/list") {
                call.respond(BookNameRoute(call).list())
            }
            post("/put") {
                call.respond(BookNameRoute(call).put())
            }
        }

        route("/category") {
            post("/list") {
                call.respond(CategoryRoute(call).list())
            }
            post("/put") {
                call.respond(CategoryRoute(call).put())
            }
            post("/get") {
                call.respond(CategoryRoute(call).get())
            }
        }

        route("/category/map") {
            post("/delete") {
                call.respond(CategoryMapRoute(call).delete())
            }
            post("/list") {
                call.respond(CategoryMapRoute(call).list())
            }
            post("/put") {
                call.respond(CategoryMapRoute(call).put())
            }
        }

        route("/category/rule") {
            post("/list") {
                call.respond(CategoryRuleRoute(call).list())
            }
            post("/put") {
                call.respond(CategoryRuleRoute(call).put())
            }
            post("/delete") {
                call.respond(CategoryRuleRoute(call).delete())
            }
        }

        route("/bill") {
            post("/list") {
                call.respond(BillRoute(call).list())
            }
            post("/group") {
                call.respond(BillRoute(call).group())
            }
            post("/unGroup") {
                call.respond(BillRoute(call).unGroup())
            }
            post("/put") {
                call.respond(BillRoute(call).put())
            }
            post("/remove") {
                call.respond(BillRoute(call).remove())
            }
            post("/clear") {
                call.respond(BillRoute(call).clear())
            }
            post("/sync/list") {
                call.respond(BillRoute(call).sync())
            }

            post("/status") {
                call.respond(BillRoute(call).status())
            }

            post("/get") {
                call.respond(BillRoute(call).get())
            }

            post("/edit") {
                call.respond(BillRoute(call).edit())
            }

            post("/book/list") {
                call.respond(BookBillRoute(call).list())
            }
            post("/book/put") {
                call.respond(BookBillRoute(call).put())
            }
        }

        route("/db") {
            get("/export") {
                DatabaseRoute(call, context).exportDb()
            }
            post("/import") {
                DatabaseRoute(call, context).importDb()
            }
            post("/clear") {
                DatabaseRoute(call, context).clearDb()
            }
        }
    }
}