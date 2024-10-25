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

package net.ankio.auto.xposed.hooks.qianji.sync.debt

import android.content.Context
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.hooks.qianji.sync.AssetsUtils
import net.ankio.auto.xposed.hooks.qianji.tools.AssetAccount
import net.ankio.auto.xposed.hooks.qianji.tools.LoanInfo
import net.ankio.auto.utils.DateUtils
import org.ezbook.server.db.model.BillInfoModel
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class BaseDebt(
    private val manifest: HookerManifest,
    private val classLoader: ClassLoader,
    private val context: Context
) {
    /**
     * 账单类
     */




    val billHelpersClazz  by lazy {
        // BillDbHelper
        manifest.clazz("BillDbHelper",classLoader)
    }

    val billToolsClazz  by lazy {
        // BillTools
        manifest.clazz("BillTools",classLoader)
    }

    var assetsUtils: AssetsUtils = AssetsUtils(manifest,classLoader)

    val baseSubmitAssetPresenterImpl by lazy {
        XposedHelpers.findClass("com.mutangtech.qianji.asset.submit.mvp.BaseSubmitAssetPresenterImpl",classLoader)
    }

    val requestInterface by lazy {
        manifest.clazz("RequestInterface",classLoader)
    }

    val assetsInterface by lazy {
        manifest.clazz("AssetsInterface",classLoader)
    }

    val billClazz  by lazy {
        XposedHelpers.findClass(
            "com.mutangtech.qianji.data.model.Bill",
            classLoader
        )
    }

    abstract suspend fun sync(billModel: BillInfoModel)

    fun createLoan(time:Long): LoanInfo {
        val loan = LoanInfo(classLoader)
        loan.setStartdate(DateUtils.stampToDate(time, "yyyy-MM-dd"))
        return loan
    }

   suspend fun submitAsset(assetAccount: AssetAccount, book:Any, billModel: Map<String,Any>): AssetAccount = suspendCoroutine { continuation ->
        val presenter = XposedHelpers.newInstance(baseSubmitAssetPresenterImpl)
        // 构建账本数据
        val json = JSONObject(billModel)



       val asset = assetAccount.get

       manifest.log("提交资产=>${asset},${book},${json}")
        //提交数据给钱迹
        XposedHelpers.callMethod(presenter,"submitAsset",book,asset,json,null)
       Hooker.onceAfter(
           requestInterface,
           "onError",
           Int::class.javaPrimitiveType!!,
           String::class.java
       ){
              val code = it.args[0] as Int
                val msg = it.args[1] as String
                manifest.log("Push Asset => ${code}:${msg}")
           true
       }
       Hooker.onceAfter(
            requestInterface,
            "onFinish",
            Object::class.java
        ){  // assetsInterface
            val assetsInstance = it.args[0]
            if (assetsInstance.javaClass == assetsInterface){
                // 提交成功

                val assetsItem = XposedHelpers.callMethod(assetsInstance,"getData")
                manifest.log("Push Asset Success => ${assetsItem}")
                continuation.resume(AssetAccount(classLoader,assetsItem))

                return@onceAfter true
            }
            false
        }
    }
    /**
     * 是否为新账单
     */
    suspend fun isNewAssets(account: AssetAccount): Boolean = withContext(Dispatchers.IO) {
        return@withContext account.getId() == -1L
    }
    fun saveBill(bill:Any){
        val billHelpers = XposedHelpers.newInstance(billHelpersClazz)
        XposedHelpers.callMethod(billHelpers,"saveOrUpdateBill",bill)
    }

    fun pushBill(){
        val companion = XposedHelpers.getStaticObjectField(billToolsClazz,"Companion")
        val billTools = XposedHelpers.callMethod(companion,"getInstance")
        XposedHelpers.callMethod(billTools,"startPush",context)
    }
}