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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ezbook.server.Server
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.AssetsMapModel
import org.ezbook.server.models.ResultModel
import org.ezbook.server.tools.AssetsMap

/**
 * 资产映射管理路由配置
 * 提供资产名称映射功能，用于将不同来源的资产名称统一到标准资产账户
 */
fun Route.assetsMapRoutes() {
    route("/assets/map") {
        /**
         * GET /assets/map/list - 获取资产映射列表
         * 支持分页查询和搜索功能
         *
         * @param page 页码，默认为1（当limit=0时忽略分页）
         * @param limit 每页条数，默认为10，设为0时返回所有数据
         * @param search 搜索关键词，可选
         * @return ResultModel 包含资产映射列表数据
         */
        get("/list") {
            val limit = call.request.queryParameters["limit"]?.toInt() ?: 10

            if (limit == 0) {
                // 返回所有数据，不分页
                val mappings = Db.get().assetsMapDao().list()
                call.respond(ResultModel(200, "OK", mappings))
                return@get
            }

            val page = call.request.queryParameters["page"]?.toInt() ?: 1
            val offset = (page - 1) * limit
            //  val search = call.request.queryParameters["search"]?.takeIf { it.isNotEmpty() }

            val mappings = Db.get().assetsMapDao().load(limit, offset)
            call.respond(ResultModel(200, "OK", mappings))
        }

        /**
         * POST /assets/map/put - 保存或更新资产映射
         * 根据资产名称自动判断是插入新映射还是更新现有映射
         *
         * @param body AssetsMapModel 资产映射数据
         * @return ResultModel 包含映射ID
         */
        post("/put") {
            val model = call.receive(AssetsMapModel::class)
            // 基于 name 的唯一索引 + REPLACE 策略，幂等写入
            val id = Db.get().assetsMapDao().insert(model)

            // 清除缓存，确保下次使用最新的映射规则
            AssetsMap.clearCache()

            call.respond(ResultModel(200, "OK", id))
        }

        /**
         * POST /assets/map/delete - 删除指定资产映射
         *
         * @param body 包含id的JSON对象
         * @return ResultModel 包含删除的映射ID
         */
        post("/delete") {
            val requestBody = call.receiveText()
            val json =
                com.google.gson.Gson().fromJson(requestBody, com.google.gson.JsonObject::class.java)
            val id = json?.get("id")?.asLong ?: 0
            Db.get().assetsMapDao().delete(id)

            // 清除缓存，确保删除后的映射规则生效
            AssetsMap.clearCache()
            
            call.respond(ResultModel(200, "OK", id))
        }

        /**
         * GET /assets/map/get - 获取指定名称的资产映射
         *
         * @param name 资产名称
         * @return ResultModel 包含映射信息
         */
        get("/get") {
            val name = call.request.queryParameters["name"] ?: ""
            val mapping = Db.get().assetsMapDao().query(name)
            call.respond(ResultModel(200, "OK", mapping))
        }

        /**
         * GET /assets/map/empty - 获取未映射的资产列表
         * 返回尚未建立映射关系的资产名称
         *
         * @return ResultModel 包含未映射的资产列表
         */
        get("/empty") {
            val emptyMappings = Db.get().assetsMapDao().empty()
            call.respond(ResultModel(200, "OK", emptyMappings))
        }

        /**
         * POST /assets/map/reapply - 重新应用资产映射到历史数据
         * 对最近3个月的历史账单重新应用当前的资产映射规则
         * 使用分批处理避免内存问题，异步执行避免阻塞请求
         *
         * @return ResultModel 包含操作启动结果
         */
        post("/reapply") {
            try {
                reapplyAssetMappingToHistoryData()

                call.respond(
                    ResultModel(
                        200,
                        "Asset mapping reapplication started successfully",
                        true
                    )
                )
            } catch (e: Exception) {
                Server.log("Error starting asset mapping reapplication: ${e.message}")
                call.respond(ResultModel(500, "Failed to start reapplication: ${e.message}", false))
            }
        }
    }
}

/**
 * 重新应用资产映射到历史数据
 * 只处理最近3个月的账单，分批处理避免内存溢出
 */
private suspend fun reapplyAssetMappingToHistoryData() {
    try {
        Server.log("开始重新应用资产映射到最近3个月的历史数据")

        val db = Db.get()

        // 计算3个月前的时间戳
        val threeMonthsAgo = calculateThreeMonthsAgo()
        Server.log("处理时间范围: ${java.util.Date(threeMonthsAgo)} 至今")

        val totalBills = db.billInfoDao().getRecentBillsCount(threeMonthsAgo)
        val batchSize = 100 // 每批处理100条记录
        var processedCount = 0

        Server.log("最近3个月账单数量: $totalBills")

        if (totalBills == 0) {
            Server.log("没有找到最近3个月的账单，操作结束")
            return
        }

        // 分批处理账单
        for (offset in 0 until totalBills step batchSize) {
            val bills = db.billInfoDao().getRecentBillsBatch(batchSize, offset, threeMonthsAgo)

            for (bill in bills) {
                try {
                    // 创建账单副本以避免修改原始数据
                    val billCopy = bill.copy()

                    // 重新应用资产映射
                    AssetsMap.setAssetsMap(billCopy)

                    // 只更新资产相关字段
                    bill.accountNameFrom = billCopy.accountNameFrom
                    bill.accountNameTo = billCopy.accountNameTo

                    // 更新到数据库
                    db.billInfoDao().update(bill)

                    processedCount++
                } catch (e: Exception) {
                    Server.log("处理账单 ${bill.id} 时出错: ${e.message}")
                }
            }

            // 每处理一批后记录进度
            Server.log("已处理账单: $processedCount / $totalBills")
        }

        Server.log("资产映射重新应用完成，共处理最近3个月的 $processedCount 条账单")

    } catch (e: Exception) {
        Server.log("重新应用资产映射时发生错误: ${e.message}")
        Server.log(e)
    }
}

/**
 * 计算3个月前的时间戳
 * @return 3个月前的时间戳（毫秒）
 */
private fun calculateThreeMonthsAgo(): Long {
    val calendar = java.util.Calendar.getInstance()
    calendar.add(java.util.Calendar.MONTH, -3)
    return calendar.timeInMillis
} 