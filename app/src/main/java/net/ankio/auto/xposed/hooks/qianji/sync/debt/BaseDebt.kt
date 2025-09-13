///*
// * Copyright (C) 2024 ankio(ankio@ankio.net)
// * Licensed under the Apache License, Version 3.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *         http://www.apache.org/licenses/LICENSE-3.0
// *
// *  Unless required by applicable law or agreed to in writing, software
// *  distributed under the License is distributed on an "AS IS" BASIS,
// *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *  See the License for the specific language governing permissions and
// *   limitations under the License.
// */
//
//package net.ankio.auto.xposed.hooks.qianji.sync.debt
//
//import de.robv.android.xposed.XposedHelpers
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import net.ankio.auto.utils.DateUtils
//import net.ankio.auto.xposed.core.hook.Hooker
//import net.ankio.auto.xposed.core.utils.AppRuntime
//import net.ankio.auto.xposed.hooks.qianji.impl.AssetPreviewPresenterImpl
//import net.ankio.auto.xposed.hooks.qianji.models.QjAssetAccountModel
//import net.ankio.auto.xposed.hooks.qianji.models.QjBillModel
//import net.ankio.auto.xposed.hooks.qianji.models.QjBookModel
//import net.ankio.auto.xposed.hooks.qianji.models.LoanInfoModel
//import org.ezbook.server.db.model.BillInfoModel
//import org.json.JSONObject
//import kotlin.coroutines.resume
//import kotlin.coroutines.suspendCoroutine
//
//abstract class BaseDebt {
//    /**
//     * 账单类
//     */
//
//
//    val billHelpersClazz by lazy {
//        // BillDbHelper
//        AppRuntime.clazz("BillDbHelper")
//    }
//
//    val billToolsClazz by lazy {
//        // BillTools
//        AppRuntime.clazz("BillTools")
//    }
//
//
//    val baseSubmitAssetPresenterImpl by lazy {
//        Hooker.loader(
//            "com.mutangtech.qianji.asset.submit.mvp.BaseSubmitAssetPresenterImpl"
//        )
//    }
//
//    val requestInterface by lazy {
//        AppRuntime.clazz("RequestInterface")
//    }
//
//    val assetsInterface by lazy {
//        AppRuntime.clazz("AssetsInterface")
//    }
//
//    val billClazz by lazy {
//        Hooker.loader(
//            "com.mutangtech.qianji.data.model.Bill"
//        )
//    }
//
//    abstract suspend fun sync(billModel: BillInfoModel)
//
//    fun createLoan(time: Long): LoanInfoModel {
//        val loan = LoanInfoModel.newInstance()
//        loan.setStartdate(DateUtils.stampToDate(time, "yyyy-MM-dd"))
//        return loan
//    }
//
//    suspend fun submitAsset(
//        assetAccount: QjAssetAccountModel,
//        book: QjBookModel,
//        billModel: Map<String, Any>
//    ): QjAssetAccountModel = suspendCoroutine { continuation ->
//        val presenter = XposedHelpers.newInstance(baseSubmitAssetPresenterImpl)
//        // 构建账本数据
//        val json = JSONObject(billModel)
//
//
//        AppRuntime.log("提交资产=>${assetAccount},${book},${json}")
//        //提交数据给钱迹
//        XposedHelpers.callMethod(
//            presenter,
//            "submitAsset",
//            book.toObject(),
//            assetAccount.toObject(),
//            json,
//            null
//        )
//        Hooker.onceAfter(
//            requestInterface,
//            "onError",
//            Int::class.javaPrimitiveType!!,
//            String::class.java
//        ) {
//            val code = it.args[0] as Int
//            val msg = it.args[1] as String
//            AppRuntime.log("Push Asset => ${code}:${msg}")
//            true
//        }
//        Hooker.onceAfter(
//            requestInterface,
//            "onFinish",
//            Object::class.java
//        ) {  // assetsInterface
//            val assetsInstance = it.args[0]
//            if (assetsInstance.javaClass == assetsInterface) {
//                // 提交成功
//                val assetsItem = XposedHelpers.callMethod(assetsInstance, "getData")
//                AppRuntime.log("Push Asset Success => ${assetsItem}")
//                continuation.resume(QjAssetAccountModel.fromObject(assetsItem))
//
//                return@onceAfter true
//            }
//            false
//        }
//    }
//
//
//    /**
//     * 是否为新账单
//     */
//    suspend fun isNewAssets(account: QjAssetAccountModel): Boolean = withContext(Dispatchers.IO) {
//        return@withContext account.getId() == -1L
//    }
//
//    fun saveBill(bill: QjBillModel) {
//        val billHelpers = XposedHelpers.newInstance(billHelpersClazz)
//        XposedHelpers.callMethod(billHelpers, "saveOrUpdateBill", bill.toObject())
//    }
//
//
//
//    fun updateAssets(assetAccount: QjAssetAccountModel) {
//        AssetPreviewPresenterImpl.updateAsset(assetAccount)
//    }
//
//    fun pushBill() {
//        val companion = XposedHelpers.getStaticObjectField(billToolsClazz, "Companion")
//        val billTools = XposedHelpers.callMethod(companion, "getInstance")
//        XposedHelpers.callMethod(billTools, "startPush", AppRuntime.application!!)
//    }
//}