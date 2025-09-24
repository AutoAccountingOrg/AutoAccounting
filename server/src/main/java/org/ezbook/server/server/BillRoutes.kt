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

import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import org.ezbook.server.constant.BillState
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.models.ResultModel
import org.ezbook.server.tools.ServerLog
import org.ezbook.server.tools.SummaryService
import org.ezbook.server.tools.StatisticsService

/**
 * 账单管理路由配置
 * 提供账单的完整生命周期管理，包括增删改查、状态管理、统计分析等功能
 */
fun Route.billRoutes() {
    route("/bill") {
        /**
         * GET /bill/list - 获取账单列表
         * 支持分页查询和状态筛选，自动清理过期数据
         *
         * @param page 页码，默认为1
         * @param limit 每页条数，默认为10
         * @param type 状态筛选，默认包含已编辑、已同步、待编辑状态
         * @return ResultModel 包含账单列表数据
         */
        get("/list") {
            // 清理无去重的账单和过期数据
            // Db.get().billInfoDao().deleteNoGroup()
            //   Db.get().billInfoDao().clearOld(System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000)

            val page = call.request.queryParameters["page"]?.toInt() ?: 1
            val limit = call.request.queryParameters["limit"]?.toInt() ?: 10
            val defaultStates = listOf(
                BillState.Edited.name,
                BillState.Synced.name,
                BillState.Wait2Edit.name
            )
            val type = call.request.queryParameters["type"]?.split(", ") ?: defaultStates

            val offset = (page - 1) * limit

            // 月份为必填：缺少 year 或 month 返回 400
            val year = call.request.queryParameters["year"]?.toInt()
                ?: return@get call.respond(ResultModel.error(400, "Year parameter is required"))
            val month = call.request.queryParameters["month"]?.toInt()
                ?: return@get call.respond(ResultModel.error(400, "Month parameter is required"))

            val calendar = java.util.Calendar.getInstance().apply {
                set(year, month - 1, 1, 0, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val startTime = calendar.timeInMillis
            calendar.add(java.util.Calendar.MONTH, 1)
            val endTime = calendar.timeInMillis

            ServerLog.d("获取账单列表（按月份）：year=$year, month=$month, page=$page, limit=$limit, type=$type")
            val bills =
                Db.get().billInfoDao().loadPageByTimeRange(limit, offset, type, startTime, endTime)
            call.respond(ResultModel.ok(bills))
        }

        /**
         * GET /bill/group - 获取账单去重信息
         *
         * @param id 账单ID
         * @return ResultModel 包含账单去重数据
         */
        get("/group") {
            val id = call.request.queryParameters["id"]?.toLong() ?: 0
            val result = Db.get().billInfoDao().queryGroup(id)
            call.respond(ResultModel.ok(result))
        }

        /**
         * POST /bill/unGroup - 取消账单去重
         *
         * @param body 包含id的JSON对象
         * @return ResultModel 操作结果
         */
        post("/unGroup") {
            val requestBody = call.receiveText()
            val json =
                com.google.gson.Gson().fromJson(requestBody, com.google.gson.JsonObject::class.java)
            val id = json?.get("id")?.asLong ?: 0
            Db.get().billInfoDao().unGroup(id)
            call.respond(ResultModel.ok(0))
        }

        /**
         * POST /bill/put - 保存或更新账单
         * 根据账单ID自动判断是插入新账单还是更新现有账单
         *
         * @param body BillInfoModel 账单数据
         * @return ResultModel 包含账单ID
         */
        post("/put") {
            val bill = call.receive(BillInfoModel::class)
            val existingBill = Db.get().billInfoDao().queryId(bill.id)

            val id = if (existingBill != null) {
                Db.get().billInfoDao().update(bill)
                bill.id
            } else {
                Db.get().billInfoDao().insert(bill)
            }
            call.respond(ResultModel.ok(id))
        }

        /**
         * POST /bill/remove - 删除账单
         * 同时删除账单本身和相关的去重信息
         *
         * @param body 包含id的JSON对象
         * @return ResultModel 操作结果
         */
        post("/remove") {
            val requestBody = call.receiveText()
            val json =
                com.google.gson.Gson().fromJson(requestBody, com.google.gson.JsonObject::class.java)
            val id = json?.get("id")?.asLong ?: 0
            ServerLog.d("删除账单：$id")
            Db.get().billInfoDao().deleteId(id)
            Db.get().billInfoDao().deleteGroup(id)
            call.respond(ResultModel.ok(0))
        }

        /**
         * POST /bill/clear - 清空所有账单
         *
         * @return ResultModel 操作结果
         */
        post("/clear") {
            Db.get().billInfoDao().clear()
            call.respond(ResultModel.ok("OK"))
        }

        /**
         * GET /bill/sync/list - 获取未同步账单列表
         *
         * @return ResultModel 包含未同步的账单数据
         */
        get("/sync/list") {
            val result = Db.get().billInfoDao().queryNoSync()
            call.respond(ResultModel.ok(result))
        }

        /**
         * POST /bill/status - 更新账单同步状态
         *
         * @param body 包含id和sync的JSON对象
         * @return ResultModel 操作结果
         */
        post("/status") {
            val requestBody = call.receiveText()
            val json =
                com.google.gson.Gson().fromJson(requestBody, com.google.gson.JsonObject::class.java)
            val id = json?.get("id")?.asLong ?: 0
            val status = json?.get("sync")?.asBoolean ?: false
            val newState = if (status) BillState.Synced else BillState.Edited
            Db.get().billInfoDao().updateStatus(id, newState)
            call.respond(ResultModel.ok(0))
        }

        /**
         * GET /bill/get - 获取指定账单详情
         *
         * @param id 账单ID
         * @return ResultModel 包含账单详细信息
         */
        get("/get") {
            val id = call.request.queryParameters["id"]?.toLong() ?: 0
            val result = Db.get().billInfoDao().queryId(id)
            call.respond(ResultModel.ok(result))
        }

        /**
         * GET /bill/edit - 获取待编辑账单列表
         *
         * @return ResultModel 包含待编辑的账单列表
         */
        get("/edit") {
            val bills = Db.get().billInfoDao().loadWaitEdit()
            call.respond(ResultModel.ok(bills))
        }

        /**
         * GET /bill/range - 获取指定时间范围内的账单列表
         *
         * @param start 开始时间戳（毫秒）
         * @param end 结束时间戳（毫秒）
         * @return ResultModel 包含时间范围内的账单列表
         */
        get("/range") {
            val startTime = call.request.queryParameters["start"]?.toLong() ?: 0
            val endTime =
                call.request.queryParameters["end"]?.toLong() ?: System.currentTimeMillis()
            val bills = Db.get().billInfoDao().getBillsByTimeRange(startTime, endTime)
            call.respond(ResultModel.ok(bills))
        }

        /**
         * POST /bill/monthly/stats - 获取月度统计数据
         * 计算指定月份的收入和支出统计
         *
         * @param year 年份，必填
         * @param month 月份，必填
         * @return ResultModel 包含收入和支出统计数据
         */
        post("/monthly/stats") {
            val year = call.request.queryParameters["year"]?.toInt()
                ?: return@post call.respond(ResultModel.error(400, "Year parameter is required"))
            val month = call.request.queryParameters["month"]?.toInt()
                ?: return@post call.respond(ResultModel.error(400, "Month parameter is required"))

            // 计算时间范围
            val calendar = java.util.Calendar.getInstance().apply {
                set(year, month - 1, 1, 0, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val startTime = calendar.timeInMillis

            calendar.add(java.util.Calendar.MONTH, 1)
            val endTime = calendar.timeInMillis

            val income = Db.get().billInfoDao().getMonthlyIncome(startTime, endTime) ?: 0.0
            val expense = Db.get().billInfoDao().getMonthlyExpense(startTime, endTime) ?: 0.0

            call.respond(ResultModel.ok(mapOf("income" to income, "expense" to expense)))
        }

        /**
         * GET /bill/stats - 获取指定时间范围的统计数据
         * 返回汇总、趋势、分类占比三部分，客户端无需再遍历账单构建。
         *
         * @param start 开始时间戳（毫秒）必填
         * @param end 结束时间戳（毫秒）必填
         */
        get("/stats") {
            val startTime = call.request.queryParameters["start"]?.toLong()
                ?: return@get call.respond(
                    ResultModel.error(
                        400,
                        "Start time parameter is required"
                    )
                )
            val endTime = call.request.queryParameters["end"]?.toLong()
                ?: return@get call.respond(ResultModel.error(400, "End time parameter is required"))

            val stats = StatisticsService.buildStats(startTime, endTime)
            call.respond(ResultModel.ok(stats))
        }

        /**
         * GET /bill/book/list - 获取账本账单列表
         *
         * @param type 账单类型
         * @return ResultModel 包含账本账单数据
         */
        get("/book/list") {
            val type = call.request.queryParameters["type"] ?: ""
            val data = Db.get().bookBillDao().list(type)
            call.respond(ResultModel.ok(data))
        }

        /**
         * POST /bill/book/put - 批量更新账本账单数据
         *
         * @param md5 数据的MD5校验值
         * @param type 账单类型
         * @param body Array<BookBillModel> 账本账单数据数组
         * @return ResultModel 操作结果
         */
        post("/book/put") {
            val md5 = call.request.queryParameters["md5"] ?: ""
            val type = call.request.queryParameters["type"] ?: ""
            val bills = call.receive<Array<org.ezbook.server.db.model.BookBillModel>>()
            Db.get().bookBillDao().put(bills.toList(), type)
            setByInner(type, md5)
            call.respond(ResultModel.ok("OK"))
        }
    }
} 