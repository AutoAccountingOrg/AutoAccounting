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
import kotlinx.coroutines.launch
import org.ezbook.server.Server
import org.ezbook.server.ai.AiManager
import org.ezbook.server.models.ResultModel
import org.ezbook.server.tools.SettingUtils

fun Route.aiApiRoutes() {
    route("/ai") {
        get("/providers") {
            call.respond(
                HttpStatusCode.OK,
                ResultModel(200, "success", AiManager.getInstance().getProviderNames())
            )
        }
        // 获取模型列表（可选 provider 参数）
        post("/models") {
            val body = call.receive<Map<String, String>>()
            val providerName = body["provider"].orEmpty()
            val providerKey = body["apiKey"].orEmpty()
            SettingUtils.setApiProvider(providerName)
            SettingUtils.setApiKey(providerKey)
            val list = AiManager.getInstance().getAvailableModels(providerName)
            call.respond(HttpStatusCode.OK, ResultModel(200, "success", list))
        }

        // 获取当前 Provider 信息（apiUri、apiModel）；可选 provider 参数
        get("/info") {
            val providerName = call.request.queryParameters["provider"].orEmpty()
            SettingUtils.setApiProvider(providerName)
            val info = AiManager.getInstance().getProviderInfo(providerName)
            call.respond(HttpStatusCode.OK, ResultModel(200, "success", info))
        }

        // 发送请求（支持前端传入 provider/apiUri/apiKey/model 参数构造临时 Provider）
        post("/request") {
            val body = call.receive<Map<String, String>>()
            val systemPrompt = body["system"] ?: ""
            val userPrompt = body["user"] ?: ""

            val providerName = body["provider"].orEmpty()
            val apiKey = body["apiKey"].orEmpty()
            val apiUri = body["apiUri"].orEmpty()
            val model = body["model"].orEmpty()


            // 如果前端传入了参数，则写入到 SettingUtils，供 Provider 读取（简单可靠）
            if (apiKey.isNotEmpty()) SettingUtils.setApiKey(apiKey)
            if (apiUri.isNotEmpty()) SettingUtils.setApiUri(apiUri)
            if (model.isNotEmpty()) SettingUtils.setApiModel(model)

            val provider = if (providerName.isNotEmpty())
                AiManager.getInstance().getProvider(providerName) else null

            val resp = AiManager.getInstance().request(systemPrompt, userPrompt, provider)
            call.respond(HttpStatusCode.OK, ResultModel(200, "success", resp))
        }

        // 流式请求（SSE）
        post("/request/stream") {
            val body = call.receive<Map<String, String>>()
            val systemPrompt = body["system"] ?: ""
            val userPrompt = body["user"] ?: ""

            val providerName = body["provider"].orEmpty()
            val apiKey = body["apiKey"].orEmpty()
            val apiUri = body["apiUri"].orEmpty()
            val model = body["model"].orEmpty()

            if (apiKey.isNotEmpty()) SettingUtils.setApiKey(apiKey)
            if (apiUri.isNotEmpty()) SettingUtils.setApiUri(apiUri)
            if (model.isNotEmpty()) SettingUtils.setApiModel(model)

            val provider = if (providerName.isNotEmpty())
                AiManager.getInstance().getProvider(providerName) else null

            call.respondTextWriter(ContentType.Text.EventStream) {
                try {
                    write("data: [START]\n\n")
                    flush()

                    AiManager.getInstance()
                        .requestStream(systemPrompt, userPrompt, provider) { chunk ->
                        write("data: $chunk\n\n")
                        flush()
                    }

                    write("data: [DONE]\n\n")
                    flush()

                } catch (e: Exception) {
                    Server.logD("catch error: ${e.message}")
                    write("event: error\n")
                    write("data: ${e.message}\n\n")
                    flush()
                }
            }
        }
    }
}
