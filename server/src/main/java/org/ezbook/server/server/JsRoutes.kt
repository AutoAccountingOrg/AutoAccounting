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
import org.ezbook.server.tools.isBase64Image

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

/**
 * 将 HTTP Query + Body 转为强类型参数。
 * Body 仅 data，通过 base64 前缀（data:image...）识别为图片。
 */
private suspend fun ApplicationCall.toAnalysisParams(): AnalysisParams {
    val body = receiveText()
    val isImage = body.isBase64Image()
    val (data, image) = if (isImage) "" to body else body to ""
    return AnalysisParams(
        app = request.queryParameters["app"].orEmpty(),
        type = request.queryParameters["type"].orEmpty(),
        fromAppData = request.queryParameters["fromAppData"].toBoolean(),
        data = data,
        image = image
    )
}

/**
 * 分析请求参数。
 */
data class AnalysisParams(
    val app: String,
    val type: String,
    val fromAppData: Boolean,
    val data: String,
    val image: String = ""
)
