/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondTextWriter
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import org.ezbook.server.ai.AiManager
import org.ezbook.server.models.ResultModel
import org.ezbook.server.log.ServerLog
import org.ezbook.server.tools.SettingUtils
import org.ezbook.server.tools.runCatchingExceptCancel

fun Route.aiApiRoutes() {
    route("/ai") {
        // helpers: 统一解析与应用前端传入的 Provider/Key/URL/Model
        suspend fun applyIncomingSettings(params: Map<String, String>) {
            val providerName = params["provider"].orEmpty()
            val apiKey = params["apiKey"].orEmpty()
            val apiUri = params["apiUri"].orEmpty()
            val model = params["model"].orEmpty()

            if (providerName.isNotEmpty()) SettingUtils.setApiProvider(providerName)
            if (apiKey.isNotEmpty()) SettingUtils.setApiKey(apiKey)
            if (apiUri.isNotEmpty()) SettingUtils.setApiUri(apiUri)
            if (model.isNotEmpty()) SettingUtils.setApiModel(model)
        }

        fun resolveProvider(params: Map<String, String>) =
            params["provider"].orEmpty().takeIf { it.isNotEmpty() }
                ?.let { AiManager.getInstance().getProvider(it) }

        fun systemOf(params: Map<String, String>) = params["system"] ?: ""
        fun userOf(params: Map<String, String>) = params["user"] ?: ""

        get("/providers") {
            call.respond(
                HttpStatusCode.OK,
                ResultModel.ok(AiManager.getInstance().getProviderNames())
            )
        }
        // 获取模型列表（可选 provider 参数）
        post("/models") {
            val body = call.receive<Map<String, String>>()
            applyIncomingSettings(body)
            val list = AiManager.getInstance().getAvailableModels(body["provider"].orEmpty())
            call.respond(HttpStatusCode.OK, ResultModel.ok(list))
        }

        // 获取当前 Provider 信息（apiUri、apiModel）；可选 provider 参数
        get("/info") {
            val providerName = call.request.queryParameters["provider"].orEmpty()
            SettingUtils.setApiProvider(providerName)
            val info = AiManager.getInstance().getProviderInfo(providerName)
            call.respond(HttpStatusCode.OK, ResultModel.ok(info))
        }

        // 发送请求（支持前端传入 provider/apiUri/apiKey/model 参数构造临时 Provider）
        post("/request") {
            val body = call.receive<Map<String, String>>()
            applyIncomingSettings(body)
            val provider = resolveProvider(body)
            val result = AiManager.getInstance().request(systemOf(body), userOf(body), provider)
            result.fold(
                onSuccess = { resp -> call.respond(HttpStatusCode.OK, ResultModel.ok(resp)) },
                onFailure = { e ->
                    call.respond(
                        HttpStatusCode.OK,
                        ResultModel.error(500, e.message ?: "")
                    )
                }
            )

        }

        // 流式请求（SSE）
        post("/request/stream") {
            val body = call.receive<Map<String, String>>()
            applyIncomingSettings(body)
            val provider = resolveProvider(body)

            call.respondTextWriter(ContentType.Text.EventStream) {
                runCatchingExceptCancel {
                    write("data: [START]\n\n")
                    flush()

                    AiManager.getInstance()
                        .requestStream(systemOf(body), userOf(body), provider) { chunk ->
                            write("data: $chunk\n\n")
                            flush()
                        }

                    write("data: [DONE]\n\n")
                    flush()

                }.onFailure {
                    ServerLog.e("catch error: ${it.message}", it)
                    write("event: error\n")
                    write("data: ${it.message}\n\n")
                    flush()
                }
            }
        }
    }
}
