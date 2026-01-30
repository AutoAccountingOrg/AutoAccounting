/*
 * Copyright (C) 2025 Ankio
 * Licensed under the Apache License, Version 3.0
 */

package org.ezbook.server.server

import android.content.Context
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import org.ezbook.server.models.ResultModel
import org.ezbook.server.tools.BillService

/**
 * 重写后的 jsRoutes，拆分业务逻辑，遵循单一职责原则
 */
fun Route.jsRoutes(context: Context, service: BillService) {
    route("/js") {
        post("/analysis") {
            val params = call.toAnalysisParams()
            call.respond(service.analyze(params, context))
        }

        post("/run") {
            val script = call.receiveText()
            call.respond(ResultModel.ok(service.executeJs(script)))
        }
    }
}

/** 将 HTTP Query + Body 转为强类型参数 */
private suspend fun ApplicationCall.toAnalysisParams(): AnalysisParams = AnalysisParams(
    app = request.queryParameters["app"].orEmpty(),
    type = request.queryParameters["type"].orEmpty(),
    fromAppData = request.queryParameters["fromAppData"].toBoolean(),
    data = receiveText()
)

/**
 * 分析请求参数。
 */
data class AnalysisParams(
    val app: String,
    val type: String,
    val fromAppData: Boolean,
    val data: String
)
