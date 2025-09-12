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

import io.github.oshai.kotlinlogging.KotlinLogging
import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.databinding.MenuItemBinding
import net.ankio.auto.http.LocalNetwork
import net.ankio.auto.storage.Constants
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.ui.ViewUtils
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.MessageUtils
import net.ankio.auto.xposed.core.utils.ThreadUtils
import net.ankio.auto.xposed.hooks.qianji.impl.AssetPreviewPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.impl.BookManagerImpl
import net.ankio.auto.xposed.hooks.qianji.impl.CateInitPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.tools.QianJiUi
import org.ezbook.server.Server

/**
 * SideBarHooker类用于修改钱迹App的侧边栏菜单
 * 主要功能包括：
 * 1. 在侧边栏添加自动记账的入口
 * 2. 同步钱迹的数据到自动记账
 * 3. 监控服务状态并显示
 */
class SideBarHooker : PartHooker() {
    private val logger = KotlinLogging.logger(this::class.java.name)


    override fun hook() {
        // 获取钱迹MainActivity的类
        val clazz = Hooker.loader("com.mutangtech.qianji.ui.main.MainActivity")

        // Hook MainActivity的onCreate方法，用于初始化菜单
        Hooker.after(
            clazz,
            "onCreate",
            android.os.Bundle::class.java
        ){ param ->
            val activity = param.thisObject as Activity
            AppRuntime.manifest.attachResource(activity)
            hookMenu(activity)
        }
    }

    /**
     * 修改侧边栏菜单
     * @param activity 当前Activity
     */
    private fun hookMenu(
        activity: Activity
    ) {
        val clazz = Hooker.loader("com.mutangtech.qianji.ui.maindrawer.MainDrawerLayout")
        // 只hook一次refreshAccount方法，避免重复添加菜单项
        Hooker.onceAfter(clazz, "refreshAccount") {
            val obj = it.thisObject as FrameLayout
            val linearLayout =
                ViewUtils.getViewById(
                    "com.mutangtech.qianji.R\$id",
                    obj,
                    AppRuntime.classLoader,
                    "main_drawer_content_layout"
                ) as LinearLayout
            runCatching {
                AppRuntime.manifest.attachResource(activity)
                addSettingMenu(linearLayout, activity)
            }.onFailure { item ->
                logger.error(item) { "异常" }
            }
            true
        }
    }

    // UI工具类实例
    private val colorUtils = QianJiUi()

    /**
     * 添加设置菜单到侧边栏
     * @param linearLayout 侧边栏的布局容器
     * @param context Activity上下文
     */
    private fun addSettingMenu(
        linearLayout: LinearLayout,
        context: Activity,
    ) {
        // 获取主题相关颜色
        val mainColor = colorUtils.getMainColor(context)
        val subColor = colorUtils.getSubColor(context)
        val backgroundColor = colorUtils.getBackgroundColor(context)

        // 初始化菜单项布局
        val itemMenuBinding = MenuItemBinding.inflate(LayoutInflater.from(context))
        itemMenuBinding.root.setBackgroundColor(backgroundColor)

        // 设置菜单项标题和版本号
        itemMenuBinding.title.text = context.getString(R.string.app_name)
        itemMenuBinding.title.setTextColor(mainColor)
        itemMenuBinding.version.text = BuildConfig.VERSION_NAME
        itemMenuBinding.version.setTextColor(subColor)


        // 设置图标点击事件，打开自动记账
        itemMenuBinding.appIcon.setOnClickListener {
            context.startActivity(Intent().setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setClassName(
                    BuildConfig.APPLICATION_ID,
                    BuildConfig.APPLICATION_ID + ".ui.activity.MainActivity"
                )
            )
        }



        // 添加菜单项到侧边栏
        linearLayout.addView(itemMenuBinding.root)

        // 启动持续状态检查
        ThreadUtils.launch {
            while (true) {
                if (!itemMenuBinding.root.isAttachedToWindow) break
                val result = LocalNetwork.get<String>("/")
                val background =
                    if (result.isSuccess) R.drawable.status_running else R.drawable.status_stopped
                withContext(Dispatchers.Main) {
                    itemMenuBinding.serviceStatus.setBackgroundResource(background)
                }
                kotlinx.coroutines.delay(1000) // 延迟1秒
            }
        }
    }

}