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

import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.AssetsMapModel
import org.ezbook.server.db.model.BillInfoModel

object Assets {

    /**
     * 获取资产映射
     * 必须运行在IO线程，不允许在主线程运行
     */
    suspend fun setAssetsMap(billInfoModel: BillInfoModel): Boolean {
        // 检查资产管理器是否启用
        val assetManager =
            Db.get().settingDao().query(Setting.SETTING_ASSET_MANAGER)?.value == "true"
        if (!assetManager) return false

        var needsUserAction = false

        // 处理转出账户
        processAccount(
            accountName = billInfoModel.accountNameFrom,
            billType = billInfoModel.type,
            validTypes = listOf(BillType.IncomeLending, BillType.IncomeRepayment)
        )?.let { newName ->
            billInfoModel.accountNameFrom = newName
            needsUserAction = true
        }

        // 处理转入账户
        processAccount(
            accountName = billInfoModel.accountNameTo,
            billType = billInfoModel.type,
            validTypes = listOf(BillType.ExpendLending, BillType.ExpendRepayment)
        )?.let { newName ->
            billInfoModel.accountNameTo = newName
            needsUserAction = true
        }

        return needsUserAction
    }

    /**
     * 处理单个账户的资产映射
     * @param accountName 账户名称
     * @param billType 账单类型
     * @param validTypes 有效的账单类型列表
     * @return 新的账户名称，如果不需要更改则返回null
     */
    private suspend fun processAccount(
        accountName: String,
        billType: BillType,
        validTypes: List<BillType>
    ): String? {
        if (accountName.isEmpty()) return null
        if (validTypes.contains(billType)) return null

        val mapName = Db.get().assetsMapDao().query(accountName)
        val assetName = Db.get().assetsDao().query(accountName)
        if (assetName != null) return null

        if (mapName == null) {


            // 添加空白资产映射
            val map = AssetsMapModel().apply {
                name = accountName
                this.mapName = ""
            }
            Db.get().assetsMapDao().insert(map)
            return null
        }

        return mapName.mapName.ifEmpty { null }
    }
}