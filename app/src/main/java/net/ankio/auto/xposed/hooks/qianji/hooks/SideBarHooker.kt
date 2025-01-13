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

package net.ankio.auto.xposed.hooks.qianji.hooks

import android.app.Activity
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.databinding.MenuItemBinding
import net.ankio.auto.storage.Constants
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.ui.ViewUtils
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.AppRuntime.classLoader
import net.ankio.auto.xposed.core.utils.DataUtils
import net.ankio.auto.xposed.core.utils.MessageUtils
import net.ankio.auto.xposed.core.utils.ThreadUtils
import net.ankio.auto.xposed.hooks.qianji.impl.AssetPreviewPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.impl.BookManagerImpl
import net.ankio.auto.xposed.hooks.qianji.impl.BxPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.impl.CateInitPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.impl.SearchPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.sync.SyncBillUtils
import net.ankio.auto.xposed.hooks.qianji.tools.QianJiUi
import org.ezbook.server.Server
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import org.ezbook.server.constant.SyncType
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.db.model.SettingModel


class SideBarHooker : PartHooker() {


    override fun hook() {
        val clazz = Hooker.loader("com.mutangtech.qianji.ui.main.MainActivity")

        Hooker.after(
            clazz,
            "onCreate",
            android.os.Bundle::class.java
        ){ param ->
            val activity = param.thisObject as Activity
            hookMenu(activity, classLoader)
        }

        Hooker.after(
            clazz,
            "onResume"
        ){ param ->
            val activity = param.thisObject as Activity
            runCatching {
                AppRuntime.manifest.attachResource(activity)
                ThreadUtils.launch {
                    checkServerStatus(activity)
                }
                syncData2Auto(activity)
            }.onFailure {
                AppRuntime.manifest.logE(it)
            }
        }

    }

    private fun hookMenu(
        activity: Activity,
        classLoader: ClassLoader?
    ) {
        val clazz = classLoader!!.loadClass("com.mutangtech.qianji.ui.maindrawer.MainDrawerLayout")
        Hooker.onceAfter(clazz, "refreshAccount") {
            // 只hook一次
            val obj = it.thisObject as FrameLayout
            // 调用 findViewById 并转换为 TextView
            val linearLayout =
                ViewUtils.getViewById(
                    "com.mutangtech.qianji.R\$id",
                    obj,
                    classLoader,
                    "main_drawer_content_layout"
                ) as LinearLayout
            runCatching {
                AppRuntime.manifest.attachResource(activity)
                // 找到了obj里面的name字段
                addSettingMenu(linearLayout, activity)
            }.onFailure {
                AppRuntime.manifest.logE(it)
            }
            true
        }
    }



    lateinit var itemMenuBinding: MenuItemBinding

    /**
     * 检查服务状态
     */
    private suspend fun checkServerStatus(activity: Activity) = withContext(Dispatchers.IO) {
        if ( !::itemMenuBinding.isInitialized) {
            return@withContext
        }
        val background =
            if (Server.request("/")!==null) R.drawable.status_running else R.drawable.status_stopped
        withContext(Dispatchers.Main) {
            itemMenuBinding.serviceStatus.background =
                AppCompatResources.getDrawable(activity, background)
        }
    }

    private val colorUtils = QianJiUi()

    private fun addSettingMenu(
        linearLayout: LinearLayout,
        context: Activity,
    ) {
        val mainColor = colorUtils.getMainColor(context)
        val subColor = colorUtils.getSubColor(context)
        val backgroundColor = colorUtils.getBackgroundColor(context)

        itemMenuBinding = MenuItemBinding.inflate(LayoutInflater.from(context))
        itemMenuBinding.root.setBackgroundColor(backgroundColor)

        itemMenuBinding.title.text = context.getString(R.string.app_name)
        itemMenuBinding.title.setTextColor(mainColor)

        itemMenuBinding.version.text = BuildConfig.VERSION_NAME.replace(" - Xposed", "")
        itemMenuBinding.version.setTextColor(subColor)
        itemMenuBinding.root.setOnClickListener {
            MessageUtils.toast("强制同步数据中...")
            //强制同步
            syncData2Auto(context, true)
        }

        linearLayout.addView(itemMenuBinding.root)

        ThreadUtils.launch {
            checkServerStatus(context)
        }
    }

    private var last = 0L

    /**
     * 同步数据到自动记账
     */
    private fun syncData2Auto(context: Activity, force: Boolean = false) {

        //主动调用就忽略自动打开

        if (force) last = 0
        // 最快30秒同步一次
        if (System.currentTimeMillis() - last < Constants.SYNC_INTERVAL) {
            AppRuntime.manifest.log("Sync too fast, ignore")
            return
        }
        last = System.currentTimeMillis()
        ThreadUtils.launch {
            //同步资产分类等信息
            AssetPreviewPresenterImpl.syncAssets()
            val books = BookManagerImpl.syncBooks()
            CateInitPresenterImpl.syncCategory(books)

            if (!DataUtils.configBoolean(
                    Setting.PROACTIVELY_MODEL,
                    DefaultData.PROACTIVELY_MODEL
                )
            ) {
                //同步报销账单
                BxPresenterImpl.syncBaoXiao()
                //同步支出账单
                SearchPresenterImpl.syncBills()
                //同步账单
                SyncBillUtils().sync(context)
            } else {
                if (DataUtils.configString(
                        Setting.SYNC_TYPE,
                        DefaultData.SYNC_TYPE
                    ) == SyncType.WhenOpenApp.name
                ) {
                    if (BillInfoModel.sync().isNotEmpty()) {
                        //同步账单
                        SyncBillUtils().sync(context)
                    }

                }
            }

        }
    }

}