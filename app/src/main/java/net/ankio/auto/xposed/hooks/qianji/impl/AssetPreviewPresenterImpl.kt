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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.http.api.AssetsAPI
import net.ankio.auto.http.api.SettingAPI
import net.ankio.auto.xposed.core.api.HookerClazz
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.DataUtils
import net.ankio.auto.xposed.core.utils.MessageUtils
import net.ankio.auto.xposed.hooks.qianji.models.QjAssetAccountModel
import net.ankio.auto.xposed.hooks.qianji.tools.QianJiAssetType
import net.ankio.dex.model.Clazz
import org.ezbook.server.constant.AssetsType
import org.ezbook.server.constant.Currency
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.AssetsModel
import org.ezbook.server.tools.MD5HashTable
import org.ezbook.server.tools.runCatchingExceptCancel
import java.lang.reflect.Proxy

/**
 * 钱迹资产预览界面的Presenter实现类
 * 该类负责处理钱迹App中资产相关的业务逻辑，包括资产列表的获取、同步和更新等功能
 */
class AssetPreviewPresenterImpl private constructor() {

    // 缓存获取到的资产列表（实例级）
    private var assets: List<*>? = null

    companion object : HookerClazz() {
        // 钱迹App中资产预览Presenter的完整类名
        private const val CLAZZ =
            "com.mutangtech.qianji.asset.account.mvp.AssetPreviewPresenterImpl"

        // 懒加载方式获取钱迹资产预览Presenter类
        internal val assetPreviewPresenterImplClazz by lazy { clazz() }

        override var rule = Clazz(name = this::class.java.name, nameRule = CLAZZ)

        // 单例实例
        private val instance: AssetPreviewPresenterImpl by lazy { AssetPreviewPresenterImpl() }

        // 静态转发，保持现有调用方式不变
        suspend fun syncAssets() = instance.syncAssets()
        suspend fun getAssetByName(name: String, sType: Int = -1) =
            instance.getAssetByName(name, sType)

        suspend fun getOrCreateAssetByName(name: String, type: Int, sType: Int) =
            instance.getOrCreateAssetByName(name, type, sType)
    }

    /**
     * 从钱迹获取资产列表
     * 该方法通过Hook钱迹App的相关方法，获取完整的资产列表
     * @return 返回资产列表对象
     */
    private suspend fun getAssetsList(): List<*> = withContext(Dispatchers.Main) {
        // 获取构造函数
        val constructor = assetPreviewPresenterImplClazz.constructors.firstOrNull()
            ?: throw NoSuchMethodException("构造函数未找到")

        // 获取构造函数参数类型
        val parameterTypes: Array<Class<*>> = constructor.parameterTypes
        // 创建构造函数所需的参数实例
        val param2Object = XposedHelpers.newInstance(parameterTypes[1]) // f8.c
        val param1Clazz = parameterTypes[0] // u7.b

        // 创建代理对象作为第一个参数
        val param1Object = Proxy.newProxyInstance(
            AppRuntime.classLoader,
            arrayOf(param1Clazz)
        ) { _, _, _ -> null }

        // 创建AssetPreviewPresenterImpl实例
        val assetPreviewPresenterImplObj =
            XposedHelpers.newInstance(assetPreviewPresenterImplClazz, param1Object, param2Object)

        // 使用 CompletableDeferred 优雅等待回调
        val deferred = CompletableDeferred<List<*>>()
        Hooker.onceAfterNoParams(
            parameterTypes[1],
            "setAccountList"
        ) {
            val accountList = it.args[0] as List<*>
            if (!deferred.isCompleted) deferred.complete(accountList)
        }
        // 触发加载资产列表
        XposedHelpers.callMethod(assetPreviewPresenterImplObj, "loadAssets", true, false)
        deferred.await()
    }

    /**
     * 同步资产列表到自动记账系统
     * 该方法会将钱迹中的资产信息转换并同步到自动记账系统中
     */
    suspend fun syncAssets() = withContext(Dispatchers.IO) {
        val accounts =
            withContext(Dispatchers.Main) {
                getAssetsList()
            }
        val assets = arrayListOf<AssetsModel>()

        accounts.forEach {
            if (it == null) return@forEach
            val assetAccount = QjAssetAccountModel.fromObject(it)
            if (
                !assetAccount.isVisible()  //不可见
                || assetAccount.isZhaiWuFinished() //债务结束
            ) {
                AppRuntime.manifest.log("隐藏的资产不同步:${assetAccount.getName()}")
                return@forEach
            }

            val model = AssetsModel()
            val stype = assetAccount.getStype()
            val type = assetAccount.getType()
            model.type = when (type) {
                QianJiAssetType.Type_Money -> AssetsType.NORMAL
                QianJiAssetType.Type_Credit -> AssetsType.CREDIT
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
                runCatchingExceptCancel { Currency.valueOf(assetAccount.getCurrency()) }.getOrDefault(
                    Currency.CNY
                )

            assets.add(model)
        }
        val sync = Gson().toJson(assets)
        val md5 = MD5HashTable.md5(sync)
        val server = SettingAPI.get(Setting.HASH_ASSET, "")
        DataUtils.set("sync_assets", Gson().toJson(assets))
        if (server == md5  && !AppRuntime.debug || assets.isEmpty() ) { //资产为空也不同步
            AppRuntime.manifest.log("No need to sync Assets, server md5:${server} local md5:${md5}")
            return@withContext
        }
        AppRuntime.manifest.log("Sync Assets:${Gson().toJson(assets)}")
        AssetsAPI.put(assets, md5)
        withContext(Dispatchers.Main) {
            MessageUtils.toast("已同步资产信息到自动记账")
        }
    }

    /**
     * 通过资产名称获取特定资产信息
     * @param name 资产名称
     * @param sType 资产子类型，默认为-1表示不限制子类型
     * @return 返回匹配的资产账户，如果未找到则返回null
     */
    suspend fun getAssetByName(name: String, sType: Int = -1): QjAssetAccountModel? =
        withContext(Dispatchers.IO) {
            // 确保assets已加载
            if (assets == null) {
                assets = withContext(Dispatchers.Main) {
                    getAssetsList()
                }
            }

            // 尝试查找资产
            var account = findAssetInList(name, sType)

            // 如果未找到，重新加载资产列表后再次尝试
            if (account == null) {
                assets = withContext(Dispatchers.Main) {
                    getAssetsList()
                }
                account = findAssetInList(name, sType)
                if (account == null) {
                    AppRuntime.manifest.log("未找到资产:$name")
                }
            }

            account
        }

    /**
     * 在资产列表中查找指定资产
     * @param name 资产名称
     * @param sType 资产子类型
     * @return 返回匹配的资产账户，如果未找到则返回null
     */
    private fun findAssetInList(name: String, sType: Int): QjAssetAccountModel? {
        return assets?.firstOrNull {
            val assetAccount = QjAssetAccountModel.fromObject(it!!)
            assetAccount.getName() == name && (sType == -1 || assetAccount.getStype() == sType)
        }?.let { QjAssetAccountModel.fromObject(it) }
    }

    /**
     * 获取或创建指定资产
     * 如果资产不存在，则创建一个新的资产账户
     * @param name 资产名称
     * @param type 资产类型
     * @param sType 资产子类型
     * @return 返回已存在或新创建的资产账户
     */
    suspend fun getOrCreateAssetByName(name: String, type: Int, sType: Int): QjAssetAccountModel =
        withContext(Dispatchers.IO) {
            val account = getAssetByName(name, sType)
            if (account != null) {
                return@withContext account
            }
            val asset = QjAssetAccountModel.newInstance(type, sType, name)
            asset.setIncount(1)
            asset.setIcon("null")
            return@withContext asset
        }
}