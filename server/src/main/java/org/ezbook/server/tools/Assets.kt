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
import org.ezbook.server.db.model.BillInfoModel

object Assets {

    /**
     * 获取资产映射
     * 必须运行在IO线程，不允许在主线程运行
     */
    suspend fun setAssetsMap(billInfoModel: BillInfoModel): Boolean {
        val assetManager =
            Db.get().settingDao().query(Setting.SETTING_ASSET_MANAGER)?.value == "true"
        if (!assetManager) {
            return false
        }

        if (billInfoModel.accountNameFrom.isNotEmpty()) {
            val mapName = Db.get().assetsMapDao().query(billInfoModel.accountNameFrom)
            val assetName = Db.get().assetsDao().query(billInfoModel.accountNameFrom)
            if (assetName == null) {
                if (mapName == null) {
                    if (!listOf(BillType.IncomeLending, BillType.IncomeRepayment).contains(
                            billInfoModel.type
                        )
                    ) {
                        return true
                    }
                } else {
                    billInfoModel.accountNameFrom = mapName.mapName
                }
            }

        }

        if (billInfoModel.accountNameTo.isNotEmpty()) {
            val mapName = Db.get().assetsMapDao().query(billInfoModel.accountNameTo)
            val assetName = Db.get().assetsDao().query(billInfoModel.accountNameTo)
            if (assetName == null) {
                if (mapName == null) {
                    if (!listOf(BillType.ExpendLending, BillType.ExpendRepayment).contains(
                            billInfoModel.type
                        )
                    ) {
                        return true
                    }
                } else {
                    billInfoModel.accountNameTo = mapName.mapName
                }
            }
        }

        return false
    }

}