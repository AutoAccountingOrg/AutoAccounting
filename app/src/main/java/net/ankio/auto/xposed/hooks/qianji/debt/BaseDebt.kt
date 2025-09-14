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

package net.ankio.auto.xposed.hooks.qianji.debt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.ankio.auto.utils.DateUtils
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.hooks.qianji.helper.AssetDbHelper
import net.ankio.auto.xposed.hooks.qianji.helper.BillDbHelper
import net.ankio.auto.xposed.hooks.qianji.impl.BaseSubmitAssetPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.impl.Callbacks
import net.ankio.auto.xposed.hooks.qianji.models.LoanInfoModel
import net.ankio.auto.xposed.hooks.qianji.models.QjAssetAccountModel
import net.ankio.auto.xposed.hooks.qianji.models.QjBillModel
import net.ankio.auto.xposed.hooks.qianji.models.QjBookModel
import org.ezbook.server.db.model.BillInfoModel
import org.json.JSONObject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

abstract class BaseDebt {

    abstract suspend fun sync(billModel: BillInfoModel)

    fun createLoan(time: Long): LoanInfoModel {
        val loan = LoanInfoModel.newInstance()
        loan.setStartdate(DateUtils.stampToDate(time, "yyyy-MM-dd"))
        return loan
    }

    suspend fun submitAsset(
        assetAccount: QjAssetAccountModel,
        book: QjBookModel,
        billModel: Map<String, Any>
    ): QjAssetAccountModel = suspendCancellableCoroutine { cont ->
        // 构建账本数据
        val json = JSONObject(billModel)
        AppRuntime.manifest.log("提交资产=>${assetAccount},${book},${json}")

        // 创建Presenter并设置视图代理，基于 onSubmitFinished 回调做结果决策
        val presenter = BaseSubmitAssetPresenterImpl.newInstance()
        presenter.setView(
            Callbacks(
                onSubmitFinished = { success, asset ->
                    if (!success) {
                        // 失败：抛出异常，避免记录错乱
                        if (!cont.isCompleted) cont.resumeWithException(IllegalStateException("Submit asset failed"))
                        return@Callbacks
                    }
                    val origin = asset
                        ?: run {
                            if (!cont.isCompleted) cont.resumeWithException(IllegalStateException("Submit success but asset is null"))
                            return@Callbacks
                        }
                    try {
                        val result = QjAssetAccountModel.fromObject(origin)
                        if (!cont.isCompleted) cont.resume(result)
                    } catch (e: Throwable) {
                        if (!cont.isCompleted) cont.resumeWithException(e)
                    }
                }
            )
        )

        // 发起提交（使用同一个 presenter 实例）
        presenter.submitAsset(
            book,
            assetAccount,
            json,
            null
        )
    }


    /**
     * 是否为新账单
     */
    suspend fun isNewAssets(account: QjAssetAccountModel): Boolean = withContext(Dispatchers.IO) {
        return@withContext account.getId() == -1L
    }

    fun saveBill(bill: QjBillModel) {
        BillDbHelper.newInstance().saveOrUpdateBill(bill)
    }


    fun updateAssets(assetAccount: QjAssetAccountModel) {
        AssetDbHelper.newInstance().insertOrReplace(assetAccount)
    }

    fun pushBill() {
        /*  val companion = XposedHelpers.getStaticObjectField(billToolsClazz, "Companion")
          val billTools = XposedHelpers.callMethod(companion, "getInstance")
          XposedHelpers.callMethod(billTools, "startPush", AppRuntime.application!!)*/
    }
}