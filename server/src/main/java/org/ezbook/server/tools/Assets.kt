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

object Assets {

    /**
     * 获取资产映射
     * 必须运行在IO线程，不允许在主线程运行
     */
    suspend fun setAssetsMap(billInfoModel: BillInfoModel): Boolean {
        val assetManager = isAssetManagerEnabled()
        if (!assetManager) return false

        var needsUserAction = true

        val newAccountNameFrom = processAccount(
            billInfoModel.accountNameFrom,
            billInfoModel.type,
            listOf(BillType.IncomeLending, BillType.IncomeRepayment),
            billInfoModel
        )
        if (newAccountNameFrom != null) {
            billInfoModel.accountNameFrom = newAccountNameFrom
            needsUserAction = false
        }

        val newAccountNameTo = processAccount(
            billInfoModel.accountNameTo,
            billInfoModel.type,
            listOf(BillType.ExpendLending, BillType.ExpendRepayment),
            billInfoModel
        )
        if (newAccountNameTo != null) {
            billInfoModel.accountNameTo = newAccountNameTo
            needsUserAction = false
        }

        if (needsUserAction && billInfoModel.generateByAi()) {
            needsUserAction = false
        }

        Server.logD("setAssetsMap: $billInfoModel, needsUserAction: $needsUserAction")

        return needsUserAction
    }

    private suspend fun isAssetManagerEnabled(): Boolean =
        Db.get().settingDao().query(Setting.SETTING_ASSET_MANAGER)?.value == "true"

    private suspend fun processAccount(
        accountName: String,
        billType: BillType,
        validTypes: List<BillType>,
        billInfoModel: BillInfoModel
    ): String? {
        if (accountName.isEmpty() || validTypes.contains(billType)) return null

        val assetName = Db.get().assetsDao().query(accountName)
        if (assetName != null) return assetName.name

        val mapName = Db.get().assetsMapDao().query(accountName)
        if (mapName != null && mapName.mapName.isNotEmpty()) {
            return mapName.mapName
        }

        // 处理空映射或不存在的映射
        return handleEmptyMapping(accountName) ?: insertEmptyMapping(
            accountName,
            mapName,
            billInfoModel
        )
    }

    private suspend fun handleEmptyMapping(accountName: String): String? {
        val list = Db.get().assetsMapDao().list()
        return list.firstOrNull { it.regex && accountName.contains(it.name) }?.mapName
    }

    private suspend fun insertEmptyMapping(
        accountName: String,
        mapName: AssetsMapModel?,
        billInfoModel: BillInfoModel
    ): String? {
        if (billInfoModel.generateByAi()) return null
        if (mapName != null) return null
        Db.get().assetsMapDao().insert(AssetsMapModel().apply {
            name = accountName
            this.mapName = ""
        })
        return null
    }
}