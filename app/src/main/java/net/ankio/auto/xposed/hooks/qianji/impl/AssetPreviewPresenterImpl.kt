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

package net.ankio.auto.xposed.hooks.qianji.impl

import com.google.gson.Gson
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.DataUtils
import net.ankio.auto.xposed.core.utils.MD5HashTable
import net.ankio.auto.xposed.core.utils.MessageUtils
import net.ankio.auto.xposed.hooks.qianji.models.AssetAccount
import net.ankio.auto.xposed.hooks.qianji.tools.QianJiAssetType
import org.ezbook.server.constant.AssetsType
import org.ezbook.server.constant.Currency
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.AssetsModel
import org.ezbook.server.db.model.SettingModel
import java.lang.reflect.Proxy
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object AssetPreviewPresenterImpl {

    const val CLAZZ = "com.mutangtech.qianji.asset.account.mvp.AssetPreviewPresenterImpl"
    val assetPreviewPresenterImplClazz by lazy {
        Hooker.loader(CLAZZ)
    }
    private var assets: List<*>? = null
    /**
     * 从钱迹获取资产列表
     */
    suspend fun getAssetsList(): List<*> = suspendCoroutine { continuation ->
        var resumed = false
        // 获取所有构造函数
        val constructor = assetPreviewPresenterImplClazz.constructors.firstOrNull()

        /**
         * 410_951 public AssetPreviewPresenterImpl(u7.b bVar, f8.c cVar)
         */

        if (constructor == null) {
            continuation.resumeWith(Result.failure(NoSuchMethodException("构造函数未找到")))
            return@suspendCoroutine
        }
        val parameterTypes: Array<Class<*>> = constructor.parameterTypes
        // 第一个是interface, 第二个是class
        val param2Object = XposedHelpers.newInstance(parameterTypes[1]) // f8.c
        val param1Clazz = parameterTypes[0] // u7.b


        val param1Object = Proxy.newProxyInstance(
            AppRuntime.classLoader,
            arrayOf(param1Clazz)
        ) { _, _, _ ->
            null
        }

        val assetPreviewPresenterImplObj =
            XposedHelpers.newInstance(assetPreviewPresenterImplClazz, param1Object, param2Object)

        //  f8.c.setAccountList(java.util.List, boolean, boolean, boolean, java.util.HashMap, int)
        Hooker.onceAfter(
            parameterTypes[1],
            "setAccountList",
            List::class.java,
            Boolean::class.java,
            Boolean::class.java,
            Boolean::class.java,
            HashMap::class.java,
            Int::class.java
        ) {
            val accountList = it.args[0] as List<*> // 资产列表
            if (!resumed) {
                resumed = true
                continuation.resume(accountList)
            }
            true
        }

        //触发加载资产列表
        XposedHelpers.callMethod(assetPreviewPresenterImplObj, "loadAssets", true, false)

    }
    /**
     * 同步资产列表
     */
    suspend fun syncAssets() = withContext(Dispatchers.IO) {
        val accounts =
            withContext(Dispatchers.Main) {
                getAssetsList()
            }
        val assets = arrayListOf<AssetsModel>()

        accounts.forEach {
            if (it == null) return@forEach
            val assetAccount = AssetAccount.fromObject(it)
            if (!assetAccount.isVisible()) {
                AppRuntime.logD("隐藏的资产不同步:${assetAccount.getName()}")
                return@forEach
            }

            val model = AssetsModel()
            val stype = assetAccount.getStype()
            val type = assetAccount.getType()
            model.type = when (type) {
                QianJiAssetType.Type_Money -> AssetsType.NORMAL
                QianJiAssetType.Type_Credit -> AssetsType.CREDIT
                QianJiAssetType.Type_Recharge -> AssetsType.VIRTUAL
                QianJiAssetType.Type_Invest -> AssetsType.FINANCIAL
                QianJiAssetType.Type_DebtLoan -> when (stype) {
                    QianJiAssetType.SType_Loan -> AssetsType.BORROWER
                    else -> AssetsType.CREDITOR
                }

                else -> AssetsType.NORMAL
            }
            model.name = assetAccount.getName()
            model.icon = assetAccount.getIcon()
            model.sort = assetAccount.getSort()
            model.currency =
                runCatching { Currency.valueOf(assetAccount.getCurrency()) }.getOrDefault(Currency.CNY)

            assets.add(model)
        }
        val sync = Gson().toJson(assets)
        val md5 = MD5HashTable.md5(sync)
        val server = SettingModel.get(Setting.HASH_ASSET, "")
        DataUtils.set("sync_assets", Gson().toJson(assets))
        if (server == md5  && !AppRuntime.debug || assets.isEmpty() ) { //资产为空也不同步
            AppRuntime.log("No need to sync Assets, server md5:${server} local md5:${md5}")
            return@withContext
        }
        AppRuntime.logD("Sync Assets:${Gson().toJson(assets)}")
        AssetsModel.put(assets, md5)
        withContext(Dispatchers.Main) {
            MessageUtils.toast("已同步资产信息到自动记账")
        }
    }

    /**
     * 通过资产名称获取资产
     */
    suspend fun getAssetByName(name: String, sType: Int = -1): AssetAccount? =
        withContext(Dispatchers.IO) {
            if (assets == null) {
                assets = withContext(Dispatchers.Main) {
                    getAssetsList()
                }
            }
            val account = assets!!.firstOrNull {
                val assetAccount = AssetAccount.fromObject(it!!)
                assetAccount.getName() == name && (sType == -1 || assetAccount.getStype() == sType)
            }
            if (account == null) {
                AppRuntime.logD("未找到资产:$name")
                return@withContext null
            } else {
                return@withContext AssetAccount.fromObject(account)
            }
        }

    suspend fun getOrCreateAssetByName(name: String, type: Int, sType: Int): AssetAccount =
        withContext(Dispatchers.IO) {
            val account = getAssetByName(name, sType)
            if (account != null) {
                return@withContext account
            }
            val asset = AssetAccount()
            asset.setType(type)
            asset.setStype(sType)
            asset.setName(name)
            asset.setIncount(1)
            asset.setIcon("null")
            return@withContext asset
        }


    // 修改资产余额： public final void R0(AssetAccount p0,double p1,boolean p2){
    //       double[] uodoubleArra;
    //       if (p0 == null) {
    //          return;
    //       }
    //       double money = p0.getMoney();
    //       if (p2) {
    //          uodoubleArra = new double[]{p1};
    //          p1 = m.plus(money, uodoubleArra);
    //       }else {
    //          uodoubleArra = new double[]{p1};
    //          p1 = m.subtract(money, uodoubleArra);
    //       }
    //       p0.changeMoney(p1);
    //       new a().insertOrReplace(p0, 0);
    //       Intent intent = new Intent("com.free2017.broadcast.asset.changed_single");
    //       intent.putExtra("data", p0);
    //       b.b(intent);
    //       return;
    //    }
}