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

package net.ankio.auto.xposed.hooks.qianji.debt/*
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


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.hooks.qianji.impl.AssetPreviewPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.impl.BookManagerImpl
import net.ankio.auto.xposed.hooks.qianji.models.QjAssetAccountModel
import net.ankio.auto.xposed.hooks.qianji.models.QjBillModel
import net.ankio.auto.xposed.hooks.qianji.models.QjBookModel
import org.ezbook.server.db.model.BillInfoModel


/**
 * 还款
 */

class ExpendRepaymentUtils :
    BaseDebt() {
    override suspend fun sync(billModel: BillInfoModel) = withContext(Dispatchers.IO) {
        // 谁付钱
        val accountFrom = getAccountFrom(billModel)
        // 资产
        val accountTo = getAccountTo(billModel)

        val book = BookManagerImpl.getBookByName(billModel.bookName)

        AppRuntime.manifest.d("收款: ${billModel.money} ${billModel.accountNameFrom} -> ${billModel.accountNameTo}")

        //拆分账单

        val (bill1, bill2) = splitBill(billModel, accountFrom)

        if (bill2 != null) {
            val bill = updateBill(bill2, 10, book, accountFrom, accountTo)
            saveBill(bill)
        }

        // 更新loan
        updateLoan(bill1!!, accountFrom)
        // 更新资产
        updateAsset(accountFrom, accountTo, bill1)

        if (bill1.money > 0) {
            // 构建账单
            val bill = updateBill(bill1, 9, book, accountFrom, accountTo)
            saveBill(bill)
        }



        pushBill()
    }

    private suspend fun splitBill(
        billModel: BillInfoModel,
        accountFrom: QjAssetAccountModel
    ): List<BillInfoModel?> = withContext(Dispatchers.IO) {
        val assetMoney = accountFrom.getMoney()
        if (assetMoney < billModel.money) {
            val interest = billModel.money - assetMoney
            val bill1 = billModel.copy().apply {
                money = assetMoney
            }
            val bill2 = billModel.copy().apply {
                money = interest
                remark = "债务利息"
            }
            return@withContext listOf(bill1, bill2)
        }
        return@withContext listOf(billModel, null)
    }


    /**
     * 获取借入账户
     */

    private suspend fun getAccountTo(billModel: BillInfoModel): QjAssetAccountModel =
        withContext(Dispatchers.IO) {
            return@withContext AssetPreviewPresenterImpl.getAssetByName(billModel.accountNameTo)
                ?: throw RuntimeException("债主不存在 key=accountname;value=${billModel.accountNameTo}")
        }


    /**
     * 获取借款账户
     */

    private suspend fun getAccountFrom(billModel: BillInfoModel): QjAssetAccountModel =
        withContext(Dispatchers.IO) {
            return@withContext AssetPreviewPresenterImpl.getAssetByName(billModel.accountNameFrom)
                ?: throw RuntimeException("收款账户不存在 key=accountname;value=${billModel.accountNameFrom}")
        }


    /**
     * 更新债务
     */

    private suspend fun updateLoan(billModel: BillInfoModel, accountTo: QjAssetAccountModel) =
        withContext(Dispatchers.IO) {
            // 债务
            val loan = accountTo.getLoanInfo()

            // {"a":0,"b":"2024-07-17","c":"","e":-12.0,"f":0.0}
            // f=TotalPay 已还金额
            // e=money 待还金额
            //
            loan.setTotalpay(-billModel.money)

            accountTo.setLoanInfo(loan)
            accountTo.addMoney(-billModel.money)
        }

    /**
     * 保存账单
     */

    private suspend fun updateAsset(
        accountFrom: QjAssetAccountModel,
        accountTo: QjAssetAccountModel,
        billModel: BillInfoModel,
    ) = withContext(Dispatchers.IO) {

        accountTo.addMoney(billModel.money)

        updateAssets(accountTo)
        updateAssets(accountFrom)
    }


    /**
     * 更新账单
     */

    private suspend fun updateBill(
        billModel: BillInfoModel,
        type: Int,
        book: QjBookModel,
        accountFrom: QjAssetAccountModel,
        accountTo: QjAssetAccountModel
    ): QjBillModel = withContext(Dispatchers.IO) {
        val money = billModel.money

        val remark = billModel.remark

        val time = billModel.time / 1000

        val imageList = ArrayList<String>()

        //    bill2 = Bill.newInstance(i12, trim, d12, timeInMillis, imageUrls);
        val bill = QjBillModel.newInstance(
            type,
            remark,
            money,
            time,
            imageList
        )

        //  Arguments com.mutangtech.qianji.data.db.dbhelper.k.saveOrUpdateBill(_id=null;billid=1726485242560127574;userid=200104405e109647c18e9;bookid=-1;timeInSec=1726485230;type=10;remark=债务利息;money=10.0;status=2egoryId=0;platform=0;assetId=1716722720363;fromId=1726474557467;targetId=-1;extra=null)

        //(agent) [195824] Arguments com.mutangtech.qianji.data.db.dbhelper.k.saveOrUpdateBill(_id=null;billid=1726485242551107424;userid=200104405e109647c18e9;bookid=-1;timeInSec=1726485230;type=9;remark=;money=974.0;status=2;categoryId=0;platform=0;assetId=1726474557467;fromId=1716722720363;targetId=-1;extra=null)


        if (billModel.remark.contains("利息")) {
            QjBillModel.setZhaiwuCurrentAsset(bill, accountTo)
            QjBillModel.setZhaiwuAboutAsset(bill, accountFrom)
        } else {
            QjBillModel.setZhaiwuCurrentAsset(bill, accountFrom)
            QjBillModel.setZhaiwuAboutAsset(bill, accountTo)
        }


        bill.setBook(book)
        bill.setDescinfo("${accountTo.getName()}->${accountFrom.getName()}")

        bill

    }
}
