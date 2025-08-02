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

package org.ezbook.server.tools

import org.ezbook.server.Server
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.AssetsMapModel
import org.ezbook.server.db.model.BillInfoModel

/**
 * 资产映射工具类
 *
 * 负责将账单中的原始账户名称映射为标准化的资产名称，支持多种映射方式：
 * - 直接资产查找、自定义映射、正则表达式匹配等
 */
object AssetsMap {

    /**
     * 设置资产映射
     *
     * 对账单的来源账户和目标账户进行映射处理
     *
     * @param billInfoModel 账单信息模型
     *
     * 注意：必须运行在IO线程
     */
    suspend fun setAssetsMap(billInfoModel: BillInfoModel) {
        // 检查资产管理器是否启用
        if (Db.get().settingDao().query(Setting.SETTING_ASSET_MANAGER)?.value != "true") {
            return
        }
        // 处理来源账户（跳过收入借贷和收入还款类型）
        val mappedAccountFrom = mapAccount(
            billInfoModel.accountNameFrom,
            billInfoModel.type,
            listOf(BillType.IncomeLending, BillType.IncomeRepayment),
            billInfoModel
        )
        if (mappedAccountFrom != null) {
            billInfoModel.accountNameFrom = mappedAccountFrom
        }

        // 处理目标账户（跳过支出借贷和支出还款类型）
        val mappedAccountTo = mapAccount(
            billInfoModel.accountNameTo,
            billInfoModel.type,
            listOf(BillType.ExpendLending, BillType.ExpendRepayment),
            billInfoModel
        )
        if (mappedAccountTo != null) {
            billInfoModel.accountNameTo = mappedAccountTo
        }

        Server.logD("setAssetsMap: $billInfoModel")
        return
    }

    /**
     * 映射单个账户
     *
     * 按优先级顺序处理：资产查找 -> 映射查找 -> 正则匹配 -> 创建空映射
     *
     * @param accountName 账户名称
     * @param billType 账单类型
     * @param skipTypes 跳过处理的类型列表
     * @param billInfoModel 账单模型
     * @return String? 映射后的账户名称，null表示无法映射
     */
    private suspend fun mapAccount(
        accountName: String,
        billType: BillType,
        skipTypes: List<BillType>,
        billInfoModel: BillInfoModel
    ): String? {
        // 空账户名或跳过类型直接返回
        if (accountName.isEmpty() || skipTypes.contains(billType)) return null

        // 1. 直接资产查找
        Db.get().assetsDao().query(accountName)?.name?.let { return it }

        // 2. 自定义映射查找
        val existingMap = Db.get().assetsMapDao().query(accountName)
        if (existingMap?.mapName?.isNotEmpty() == true) return existingMap.mapName

        // 3. 正则表达式匹配
        Db.get().assetsMapDao().list()
            .filter { it.regex }
            .firstOrNull { accountName.contains(it.name) }
            ?.mapName?.let { return it }

        // 4. 创建空映射（仅当非AI生成且不存在映射时）
        if (!billInfoModel.generateByAi() && existingMap == null) {
            Db.get().assetsMapDao().insert(AssetsMapModel().apply {
                name = accountName
                mapName = ""
            })
        }

        return null
    }
}