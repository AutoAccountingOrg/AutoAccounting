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
 *  limitations under the License.
 */

package org.ezbook.server.server

import io.ktor.application.call
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import org.ezbook.server.Server
import org.ezbook.server.ai.AiManager
import org.ezbook.server.constant.AnalysisTaskStatus
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.AnalysisTaskModel
import org.ezbook.server.models.ResultModel
import org.ezbook.server.log.ServerLog
import org.ezbook.server.tools.SettingUtils
import org.ezbook.server.tools.SummaryService
import org.ezbook.server.tools.runCatchingExceptCancel

/**
 * AI分析任务路由配置
 * 提供AI分析任务的管理功能，包括任务列表、创建、详情查看和删除
 */
fun Route.analysisTaskRoutes() {
    route("/analysis") {
        /**
         * POST /analysis/all - 获取分析任务列表（支持分页）
         * 请求参数：
         * - page: 页码（从1开始，默认为1）
         * - size: 每页大小（默认为20，最大100）
         */
        post("/all") {
            val body = call.receiveText()
            val json = if (body.isNotBlank()) {
                com.google.gson.Gson().fromJson(body, com.google.gson.JsonObject::class.java)
            } else {
                null
            }

            val page = json?.get("page")?.asInt ?: 1
            val size = json?.get("size")?.asInt ?: 20

            // 参数校验
            val validPage = maxOf(1, page)
            val validSize = minOf(100, maxOf(1, size))
            val offset = (validPage - 1) * validSize

            val dao = Db.get().analysisTaskDao()

            // 标记超时的未完成任务为失败状态
            val thirtyMinutesAgo = System.currentTimeMillis() - (30 * 60 * 1000L)
            dao.markTimeoutTasks(thirtyMinutesAgo)

            val tasks = dao.getPage(validSize, offset)
            val total = dao.getCount()

            val result = mapOf(
                "tasks" to tasks,
                "total" to total,
                "page" to validPage,
                "size" to validSize,
                "totalPages" to ((total + validSize - 1) / validSize) // 向上取整
            )

            call.respond(ResultModel.ok(result))
        }

        /**
         * POST /analysis/create - 创建新的分析任务
         */
        post("/create") {
            val body = call.receiveText()
            val json = com.google.gson.Gson().fromJson(body, com.google.gson.JsonObject::class.java)

            val title = json?.get("title")?.asString ?: ""
            val startTime = json?.get("startTime")?.asLong ?: 0
            val endTime = json?.get("endTime")?.asLong ?: 0

            // 检查重复任务
            val existingCount = Db.get().analysisTaskDao().countByTimeRange(startTime, endTime)
            if (existingCount > 0) {
                call.respond(ResultModel.error(400, "任务已存在"))
                return@post
            }

            val task = AnalysisTaskModel().apply {
                this.title = title
                this.startTime = startTime
                this.endTime = endTime
                this.status = AnalysisTaskStatus.PENDING
                this.createTime = System.currentTimeMillis()
                this.updateTime = System.currentTimeMillis()
                this.progress = 0
            }

            val taskId = Db.get().analysisTaskDao().insert(task)

            // 异步执行分析任务
            Server.withIO {
                executeTask(taskId)
            }

            call.respond(ResultModel.ok(taskId))
        }

        /**
         * POST /analysis/detail - 获取分析任务详情
         */
        post("/detail") {
            val body = call.receiveText()
            val json = com.google.gson.Gson().fromJson(body, com.google.gson.JsonObject::class.java)
            val id = json?.get("id")?.asLong ?: 0

            val task = Db.get().analysisTaskDao().getById(id)
            call.respond(
                if (task != null) ResultModel.ok(task) else ResultModel.error(
                    404,
                    "任务不存在"
                )
            )
        }

        /**
         * POST /analysis/delete - 删除分析任务
         */
        post("/delete") {
            val body = call.receiveText()
            val json = com.google.gson.Gson().fromJson(body, com.google.gson.JsonObject::class.java)
            val id = json?.get("id")?.asLong ?: 0

            Db.get().analysisTaskDao().deleteById(id)
            call.respond(ResultModel.ok("删除成功"))
        }

        /**
         * POST /analysis/clear - 删除所有分析任务
         */
        post("/clear") {
            Db.get().analysisTaskDao().deleteAll()
            call.respond(ResultModel.ok("已清空所有任务"))
        }
    }
}

/**
 * 执行分析任务
 */
private suspend fun executeTask(taskId: Long) {
    val dao = Db.get().analysisTaskDao()

    runCatchingExceptCancel {
        val task = dao.getById(taskId) ?: return

        // 更新状态为处理中
        dao.updateStatus(taskId, AnalysisTaskStatus.PROCESSING)
        dao.updateProgress(taskId, 10)

        // 获取账单摘要数据
        val dataSummary = SummaryService.generateSummary(task.startTime, task.endTime, task.title)

        dao.updateProgress(taskId, 30)

        // 构建AI分析请求
        val userInput = """
请根据以下账单数据生成财务总结报告，包括收支分析、分类统计和理财建议：

时间范围：${task.title}

$dataSummary

请生成详细的财务分析报告。
            """.trimIndent()

        dao.updateProgress(taskId, 50)

        // 调用AI生成分析报告
        val aiResult = AiManager.getInstance().request(SettingUtils.aiSummaryPrompt(), userInput)

        if (aiResult.isSuccess) {
            val analysisContent = aiResult.getOrNull()
            if (analysisContent != null) {
                // 转换为HTML格式
                val htmlContent = analysisContent

                dao.updateProgress(taskId, 90)

                // 保存结果
                dao.update(task.apply {
                    resultHtml = htmlContent
                    status = AnalysisTaskStatus.COMPLETED
                    progress = 100
                    updateTime = System.currentTimeMillis()
                })

                ServerLog.d("AI分析任务完成，ID: $taskId")
            } else {
                error("AI返回内容为空")
            }
        } else {
            error("AI分析失败: ${aiResult.exceptionOrNull()?.message}")
        }

    }.onFailure { e ->
        ServerLog.e("执行AI分析任务失败，ID: $taskId", e)
        dao.updateStatus(taskId, AnalysisTaskStatus.FAILED)
        val task = dao.getById(taskId)
        if (task != null) {
            dao.update(task.apply {
                errorMessage = e.message ?: "未知错误"
                updateTime = System.currentTimeMillis()
            })
        }
    }
}
