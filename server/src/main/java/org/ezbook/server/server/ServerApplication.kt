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
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.routing
import org.ezbook.server.models.ResultModel
import org.ezbook.server.tools.BillService
import org.ezbook.server.log.ServerLog
import org.ezbook.server.tools.SettingUtils
import kotlin.coroutines.cancellation.CancellationException

fun Application.module(context: Context) {
    install(StatusPages) {
        exception<Throwable> { cause ->
            // CancellationException 是协程正常的取消信号（如客户端断开连接），
            // 重新抛出让 Ktor 正常处理，不记录为错误
            if (cause is CancellationException) throw cause
            call.respond(
                HttpStatusCode.OK,
                ResultModel.error(500, buildErrorMessage(cause))
            )
            ServerLog.e(cause.message ?: "", cause)
        }
    }
    install(ContentNegotiation) {
        gson()
    }

    intercept(ApplicationCallPipeline.Setup) {
        if (SettingUtils.debugMode()) return@intercept
        val remoteIp = call.request.local.remoteHost  // 客户端 IP
        val allowedIps = listOf(
            // IPv4 本地地址
            "127.0.0.1",
            "localhost",
            // IPv6 本地地址
            "::1",           // IPv6 localhost
            "0:0:0:0:0:0:0:1" // IPv6 localhost 完整格式
        )

        if (remoteIp !in allowedIps) {
            call.respond(
                HttpStatusCode.Forbidden,
                ResultModel.error(403, "Access denied from $remoteIp")
            )
            finish() // 阻止继续处理
        }
    }

    routing {
        // 基础路由
        baseRoutes()

        // AI API 路由
        aiApiRoutes()

        // 日志路由
        logRoutes()

        // 规则路由
        ruleRoutes()

        // 设置路由
        settingRoutes()

        // JavaScript 路由
        jsRoutes(context, BillService())

        // 数据路由
        dataRoutes()

        // 资产路由
        assetsRoutes()

        // 资产映射路由
        assetsMapRoutes()

        // 账本路由
        bookRoutes()

        // 分类路由
        categoryRoutes()

        // 分类映射路由
        categoryMapRoutes()

        // 分类规则路由
        categoryRuleRoutes()

        // 账单路由
        billRoutes()

        // 标签路由
        tagRoutes()

        // 数据库路由
        databaseRoutes(context)

        // AI分析任务路由
        analysisTaskRoutes()
    }
}

/**
 * 组装全局异常的可读错误信息
 * - 默认：包含异常类型 + 当前异常信息 + 根因信息
 * - 调试模式：附加完整堆栈，便于定位
 */
private fun buildErrorMessage(cause: Throwable): String {
    val root = generateSequence(cause) { it.cause }.last()
    val currentMessage = cause.message ?: "unknown error"
    val rootMessage = root.message ?: "unknown error"
    val baseMessage = buildString {
        append(cause::class.java.simpleName)
        append(": ")
        append(currentMessage)
        if (root !== cause) {
            append(" | root=")
            append(root::class.java.simpleName)
            append(": ")
            append(rootMessage)
        }
    }
    return baseMessage + "\n" + cause.stackTraceToString()
}
