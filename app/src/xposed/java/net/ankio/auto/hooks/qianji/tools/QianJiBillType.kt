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

package net.ankio.auto.hooks.qianji.tools

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.core.App
import org.ezbook.server.constant.AssetsType
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.AssetsModel
import org.ezbook.server.db.model.BillInfoModel

enum class QianJiBillType(val value: Int) {

    Expend(0), // 支出
    Income(1), // 收入
    Transfer(2), // 转账
    TransferCredit(3), // 信用卡还款
    ExpendReimbursement(5), // 支出（记作报销）

    // 以下是钱迹未实现的功能

    ExpendLending(15), // 支出（借出）
    ExpendRepayment(16), // 支出（还款销账）
    IncomeLending(17), // 收入（借入）
    IncomeRepayment(18), // 收入（还款销账）
    IncomeReimbursement(19);// 收入（报销)




    companion object {
        suspend fun toQianJi(billInfoModel: BillInfoModel): Int = withContext(Dispatchers.IO) {
            when (billInfoModel.type) {
                BillType.Expend -> Expend.value
                BillType.Income -> Income.value
                BillType.Transfer -> {
                    val sync = App.get("sync_assets")
                    val assets = runCatching {
                        Gson().fromJson(
                            sync,
                            Array<AssetsModel>::class.java
                        )
                    }.getOrElse { emptyArray() }
                    val asset = assets.find { it.name == billInfoModel.accountNameTo }
                    if (asset == null) {
                        return@withContext Transfer.value
                    }
                    if (asset.type == AssetsType.CREDIT) {
                        return@withContext TransferCredit.value
                    }
                    Transfer.value
                }

                BillType.ExpendReimbursement -> ExpendReimbursement.value
                BillType.ExpendLending -> ExpendLending.value
                BillType.ExpendRepayment -> ExpendRepayment.value
                BillType.IncomeLending -> IncomeLending.value
                BillType.IncomeReimbursement -> IncomeReimbursement.value
                BillType.IncomeRepayment -> IncomeRepayment.value
                else -> Expend.value
            }
        }


        fun toAuto(type: Int): BillType {
            return when (type) {
                Expend.value -> BillType.Expend
                Income.value -> BillType.Income
                Transfer.value -> BillType.Transfer
                TransferCredit.value -> BillType.Transfer
                ExpendReimbursement.value -> BillType.ExpendReimbursement
                ExpendLending.value -> BillType.ExpendLending
                ExpendRepayment.value -> BillType.ExpendRepayment
                IncomeLending.value -> BillType.IncomeLending
                IncomeRepayment.value -> BillType.IncomeRepayment
                IncomeReimbursement.value -> BillType.IncomeReimbursement
                else -> BillType.Expend
            }
        }
    }


}