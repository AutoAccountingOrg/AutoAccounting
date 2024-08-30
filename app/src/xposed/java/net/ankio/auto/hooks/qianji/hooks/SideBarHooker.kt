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

package net.ankio.auto.hooks.qianji.hooks

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.common.ServerInfo
import net.ankio.auto.core.App
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.api.PartHooker
import net.ankio.auto.core.ui.ColorUtils
import net.ankio.auto.core.ui.ViewUtils
import net.ankio.auto.databinding.MenuItemBinding
import net.ankio.auto.hooks.qianji.tools.SyncUtils


class SideBarHooker : PartHooker{
    override fun hook(
        hookerManifest: HookerManifest,
        application: Application?,
        classLoader: ClassLoader
    ) {
        this.hookerManifest = hookerManifest
        val clazz = classLoader.loadClass("com.mutangtech.qianji.ui.main.MainActivity")
       XposedHelpers.findAndHookMethod(
           clazz,
           "onCreate",
           android.os.Bundle::class.java,
           object : XC_MethodHook() {
               override fun afterHookedMethod(param: MethodHookParam) {
                   val activity = param.thisObject as Activity
                   hookMenu(activity, classLoader)
               }
           }
       )

        XposedHelpers.findAndHookMethod(
            clazz,
            "onResume",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as Activity
                    runCatching {
                        App.attachResource(activity)
                        App.launch {
                            checkServerStatus(activity)
                        }
                        syncData2Auto(activity)
                    }.onFailure {
                        hookerManifest.logE(it)
                    }
                }
            }
        )
    }

    private fun hookMenu(
        activity: Activity,
        classLoader: ClassLoader?
    ) {
        if (!::hookerManifest.isInitialized) {
           return
        }
        var hooked = false
        val clazz = classLoader!!.loadClass("com.mutangtech.qianji.ui.maindrawer.MainDrawerLayout")
        XposedHelpers.findAndHookMethod(
            clazz,
            "refreshAccount",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    // 只hook一次
                    val obj = param!!.thisObject as FrameLayout
                    if (hooked) return
                    hooked = true
                    // 调用 findViewById 并转换为 TextView
                    val linearLayout =
                        ViewUtils.getViewById("com.mutangtech.qianji.R\$id",obj, classLoader, "main_drawer_content_layout") as LinearLayout
                    runCatching {
                        App.attachResource(activity)
                        // 找到了obj里面的name字段
                        addSettingMenu(linearLayout, activity)
                    }.onFailure {
                        hookerManifest.logE(it)
                    }
                }
            },
        )
    }


    lateinit var hookerManifest: HookerManifest
    lateinit var itemMenuBinding: MenuItemBinding

    /**
     * 检查服务状态
     */
    private suspend fun checkServerStatus(activity: Activity) = withContext(Dispatchers.IO) {
        if(!::hookerManifest.isInitialized || !::itemMenuBinding.isInitialized){
            return@withContext
        }
        val background =  if (ServerInfo.isServerStart()) R.drawable.status_running else R.drawable.status_stopped

        withContext(Dispatchers.Main){
             itemMenuBinding.serviceStatus.background = AppCompatResources.getDrawable(activity,background)
        }
    }

    private fun addSettingMenu(
        linearLayout: LinearLayout,
        context: Activity,
    ) {
        if (!::hookerManifest.isInitialized) {
            return
        }
        val mainColor = ColorUtils.getMainColor(context)
        val subColor = ColorUtils.getSubColor(context)
        val backgroundColor = ColorUtils.getBackgroundColor(context)

        itemMenuBinding = MenuItemBinding.inflate(LayoutInflater.from(context))
        itemMenuBinding.root.setBackgroundColor(backgroundColor)

        itemMenuBinding.title.text = context.getString(R.string.app_name)
        itemMenuBinding.title.setTextColor(mainColor)

        itemMenuBinding.version.text = BuildConfig.VERSION_NAME.replace(" - Xposed","")
        itemMenuBinding.version.setTextColor(subColor)

        itemMenuBinding.root.setOnClickListener {
            //打开自动记账
            val intent =
                context.packageManager.getLaunchIntentForPackage(BuildConfig.APPLICATION_ID)
            if (intent != null) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }

        linearLayout.addView(itemMenuBinding.root)

        App.launch {
            checkServerStatus(context)
        }
    }

    /**
     * 同步数据到自动记账
     */
    fun syncData2Auto(context: Activity){

        App.toast("正在同步数据到自动记账...")
        App.launch {
           val utils = SyncUtils(hookerManifest, context.classLoader)
            //同步资产
            App.toast("正在同步资产数据...")
            utils.syncAssets()
        }
    }

}