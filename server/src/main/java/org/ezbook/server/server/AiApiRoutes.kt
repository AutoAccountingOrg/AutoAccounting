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

fun Route.aiApiRoutes() {
    route("/ai") {
        // 列出所有提供者
        get("/providers") {
            val names = AiManager.getInstance().getProviderNames()
            call.respond(HttpStatusCode.OK, ResultModel(200, "success", names))
        }

        // 获取/设置当前提供者名称
        get("/provider") {
            val current = AiManager.getInstance().getProviderNames()
                .find { it == AiManager.getInstance().currentProviderName }
            call.respond(HttpStatusCode.OK, ResultModel(200, "success", current))
        }
        post("/provider") {
            val body = call.receive<Map<String, String>>()
            val name = body["name"] ?: ""
            val ok = AiManager.getInstance().setCurrentProvider(name)
            call.respond(
                if (ok) HttpStatusCode.OK else HttpStatusCode.BadRequest,
                ResultModel(if (ok) 200 else 400, if (ok) "success" else "invalid provider")
            )
        }

        // API Key
        get("/apikey") {
            val key = AiManager.getInstance().getCurrentApiKey()
            call.respond(HttpStatusCode.OK, ResultModel(200, "success", key))
        }
        post("/apikey") {
            val body = call.receive<Map<String, String>>()
            val key = body["key"] ?: ""
            AiManager.getInstance().setCurrentApiKey(key)
            call.respond(HttpStatusCode.OK, ResultModel(200, "success"))
        }

        // API URL
        get("/apiurl") {
            val url = AiManager.getInstance().getCurrentApiUrl()
            call.respond(HttpStatusCode.OK, ResultModel(200, "success", url))
        }
        post("/apiurl") {
            val body = call.receive<Map<String, String>>()
            val url = body["url"] ?: ""
            AiManager.getInstance().setCurrentApiUrl(url)
            call.respond(HttpStatusCode.OK, ResultModel(200, "success"))
        }

        // 模型列表
        get("/models") {
            val list = AiManager.getInstance().getAvailableModels()
            call.respond(HttpStatusCode.OK, ResultModel(200, "success", list))
        }
        // 获取/设置当前模型
        get("/model") {
            val model = AiManager.getInstance().getCurrentModel()
            call.respond(HttpStatusCode.OK, ResultModel(200, "success", model))
        }
        post("/model") {
            val body = call.receive<Map<String, String>>()
            val model = body["model"] ?: ""
            AiManager.getInstance().setCurrentModel(model)
            call.respond(HttpStatusCode.OK, ResultModel(200, "success"))
        }

        // 创建 Key URI
        get("/createKeyUri") {
            val uri = AiManager.getInstance().getCreateKeyUri()
            call.respond(HttpStatusCode.OK, ResultModel(200, "success", uri))
        }

        // 发送请求
        post("/request") {
            val body = call.receive<Map<String, String>>()
            val systemPrompt = body["system"] ?: ""
            val userPrompt = body["user"] ?: ""
            val resp = AiManager.getInstance().request(systemPrompt, userPrompt)
            call.respond(HttpStatusCode.OK, ResultModel(200, "success", resp))
        }

        // 发送流式请求 - 真正的SSE响应
        post("/request/stream") {
            val body = call.receive<Map<String, String>>()
            val systemPrompt = body["system"] ?: ""
            val userPrompt = body["user"] ?: ""
            // 使用respondTextWriter实现真正的流式响应
            call.respondTextWriter(ContentType.Text.EventStream) {
                try {
                    write("data: [START]\n\n")
                    flush()

                    AiManager.getInstance().requestStream(systemPrompt, userPrompt) { chunk ->
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
