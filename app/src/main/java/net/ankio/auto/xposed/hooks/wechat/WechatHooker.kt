/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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

package net.ankio.auto.xposed.hooks.wechat

import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.utils.AppRuntime.application
import net.ankio.auto.xposed.core.utils.AppRuntime.classLoader
import net.ankio.auto.xposed.hooks.wechat.hooks.ChatUserHooker
import net.ankio.auto.xposed.hooks.wechat.hooks.DatabaseHooker
import net.ankio.auto.xposed.hooks.wechat.hooks.PayToolsHooker
import net.ankio.auto.xposed.hooks.wechat.hooks.RedPackageHooker
import net.ankio.auto.xposed.hooks.wechat.hooks.TransferHooker
import net.ankio.auto.xposed.hooks.wechat.hooks.WebViewHooker
import net.ankio.auto.xposed.hooks.wechat.models.LuckMoneyModel
import net.ankio.auto.xposed.hooks.wechat.models.RemittanceModel
import net.ankio.auto.xposed.hooks.wechat.models.WechatUserModel
import net.ankio.dex.model.Clazz
import org.ezbook.server.constant.DefaultData
import java.io.File


class WechatHooker : HookerManifest() {
    override val packageName: String = DefaultData.WECHAT_PACKAGE
    override val appName: String = "微信"

    override fun hookLoadPackage() {
        if (application == null) {
            return
        }

        // 腾讯tinker热更新框架在加载后会导致hook无效，最简单的办法是删掉
        // 判断目录/data/data/com.tencent.mm/tinker/下是否有patch-开头的文件夹，如果有就删除
        val tinkerDir = File(application!!.dataDir, "tinker")

        if (tinkerDir.exists()) {
            log("tinkerDir: ${tinkerDir.absolutePath}")
            closeTinker(classLoader)
        }


    }


    override var applicationName = "com.tencent.tinker.loader.app.TinkerApplication"
    override var partHookers: MutableList<PartHooker> = mutableListOf(
        TransferHooker(),
        DatabaseHooker(),
        RedPackageHooker(),
        ChatUserHooker(),
        PayToolsHooker(),
        WebViewHooker()
    )



    override var rules: MutableList<Clazz> = mutableListOf(
        RemittanceModel.rule,
        LuckMoneyModel.rule,
        WechatUserModel.rule,
    )


    private fun closeTinker(classLoader: ClassLoader) {
        val tinkerManager = runCatching {
            XposedHelpers.findClass(
                "com.tencent.tinker.lib.tinker.TinkerApplicationHelper",
                classLoader
            )
        }.getOrNull() ?: return
        val applicationLike =
            XposedHelpers.callStaticMethod(tinkerManager, "getTinkerApplicationLike") as Class<*>
        val shareTinkerInternals = XposedHelpers.findClass(
            "com.tencent.tinker.loader.shareutil.ShareTinkerInternals",
            classLoader
        )
        //关闭其他进程
        val application = XposedHelpers.callMethod(applicationLike, "getApplication")
        XposedHelpers.callStaticMethod(shareTinkerInternals, "killAllOtherProcess", application)
        //清除补丁
        XposedHelpers.callStaticMethod(shareTinkerInternals, "cleanPatch", applicationLike)
        //关闭tinker
        XposedHelpers.callStaticMethod(
            shareTinkerInternals,
            "setTinkerDisableWithSharedPreferences",
            application
        )
    }


}


