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

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.AssetsMapModel
import org.ezbook.server.models.ResultModel
import org.ezbook.server.tools.AssetsMap
import org.ezbook.server.log.ServerLog
import java.util.Date

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
                call.respond(ResultModel.ok(mappings))
                return@get
            }

            val page = call.request.queryParameters["page"]?.toInt() ?: 1
            val offset = (page - 1) * limit
            //  val search = call.request.queryParameters["search"]?.takeIf { it.isNotEmpty() }

            val mappings = Db.get().assetsMapDao().load(limit, offset)
            call.respond(ResultModel.ok(mappings))
        }

        /**
         * POST /assets/map/put - 保存或更新资产映射
         * 根据资产名称自动判断是插入新映射还是更新现有映射
         *
         * @param body AssetsMapModel 资产映射数据
         * @return ResultModel 包含映射ID
         */
        post("/put") {
            val model = call.receive<AssetsMapModel>()
            // 基于 name 的唯一索引 + REPLACE 策略，幂等写入
            val id = Db.get().assetsMapDao().insert(model)


            call.respond(ResultModel.ok(id))
        }

        /**
         * POST /assets/map/delete - 删除指定资产映射
         *
         * @param body 包含id的JSON对象
         * @return ResultModel 包含删除的映射ID
         */
        post("/delete") {
            val req = call.receive<DeleteRequest>()
            Db.get().assetsMapDao().delete(req.id)
            call.respond(ResultModel.ok(req.id))
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
            call.respond(ResultModel.ok(mapping))
        }

        /**
         * GET /assets/map/empty - 获取未映射的资产列表
         * 返回尚未建立映射关系的资产名称
         *
         * @return ResultModel 包含未映射的资产列表
         */
        get("/empty") {
            val emptyMappings = Db.get().assetsMapDao().empty()
            call.respond(ResultModel.ok(emptyMappings))
        }

        /**
         * POST /assets/map/reapply - 重新应用资产映射到历史数据
         * 对最近3个月的历史账单重新应用当前的资产映射规则
         * 使用分批处理避免内存问题，异步执行避免阻塞请求
         *
         * @return ResultModel 包含操作启动结果
         */
        post("/reapply") {
            reapplyAssetMappingToHistoryData()
            call.respond(ResultModel.ok(true))
        }

        /**
         * POST /assets/map/sort - 批量更新资产映射排序
         * 接收包含name和sort的列表，批量更新排序值
         *
         * @param body 包含排序信息的JSON数组
         * @return ResultModel 操作结果
         */
        post("/sort") {
            // 手动解析JSON避免泛型擦除导致的类型转换问题
            val json = call.receiveText()
            val type = object : TypeToken<List<SortItem>>() {}.type
            val sortList: List<SortItem> = Gson().fromJson(json, type)
            val dao = Db.get().assetsMapDao()

            // 批量更新每个项的排序值
            sortList.forEach { item ->
                dao.query(item.name)?.let { model ->
                    model.sort = item.sort
                    dao.update(model)
                }
            }

            call.respond(ResultModel.ok(true))
        }
    }
}

/**
 * 排序项数据类
 * @param name 资产映射名称（唯一标识）
 * @param sort 新的排序值
 */
private data class SortItem(
    val name: String = "",
    val sort: Int = 0
)

/**
 * 删除请求体
 * 仅包含要删除记录的唯一标识。
 */
private data class DeleteRequest(
    /** 要删除的记录ID */
    val id: Long
)

/**
 * 重新应用资产映射到历史数据
 * 只处理最近3个月的账单，分批处理避免内存溢出
 */
private suspend fun reapplyAssetMappingToHistoryData() {

    ServerLog.d("开始重新应用资产映射到最近3个月的历史数据")

    val db = Db.get()

    // 计算3个月前的时间戳
    val threeMonthsAgo = calculateThreeMonthsAgo()
    ServerLog.d("处理时间范围: ${Date(threeMonthsAgo)} 至今")

    val totalBills = db.billInfoDao().getRecentBillsCount(threeMonthsAgo)
    val batchSize = 100 // 每批处理100条记录
    var processedCount = 0

    ServerLog.d("最近3个月账单数量: $totalBills")

    if (totalBills == 0) {
        ServerLog.d("没有找到最近3个月的账单，操作结束")
        return
    }
    val assetsMap = AssetsMap()

    // 分批处理账单
    for (offset in 0 until totalBills step batchSize) {
        val bills = db.billInfoDao().getRecentBillsBatch(batchSize, offset, threeMonthsAgo)

        for (bill in bills) {
            try {
                // 创建账单副本以避免修改原始数据
                val billCopy = bill.copy()

                // 重新应用资产映射
                assetsMap.setAssetsMap(billCopy, false)

                // 只更新资产相关字段
                bill.accountNameFrom = billCopy.accountNameFrom
                bill.accountNameTo = billCopy.accountNameTo

                // 更新到数据库
                db.billInfoDao().update(bill)

                processedCount++
            } catch (e: Exception) {
                ServerLog.e("处理账单 ${bill.id} 时出错: ${e.message}")
            }
        }

        // 每处理一批后记录进度
        ServerLog.d("已处理账单: $processedCount / $totalBills")
    }

    ServerLog.d("资产映射重新应用完成，共处理最近3个月的 $processedCount 条账单")


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