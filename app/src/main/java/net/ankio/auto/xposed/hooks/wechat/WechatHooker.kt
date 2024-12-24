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
import net.ankio.auto.xposed.core.utils.DataUtils
import net.ankio.auto.xposed.hooks.wechat.hooks.ChatUserHooker
import net.ankio.auto.xposed.hooks.wechat.hooks.DatabaseHooker
import net.ankio.auto.xposed.hooks.wechat.hooks.PayToolsHooker
import net.ankio.auto.xposed.hooks.wechat.hooks.RedPackageHooker
import net.ankio.auto.xposed.hooks.wechat.hooks.TransferHooker
import net.ankio.auto.xposed.hooks.wechat.hooks.WebViewHooker
import net.ankio.dex.model.Clazz
import net.ankio.dex.model.ClazzField
import net.ankio.dex.model.ClazzMethod
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import java.io.File


class WechatHooker : HookerManifest() {
    override val packageName: String  = DefaultData.WECHAT_PACKAGE
    override var aliasPackageName: String  = DefaultData.WECHAT_PACKAGE_ALIAS

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


    override var minVersion: Int
        get() = 0
        set(value) {}

    override var applicationName = "com.tencent.tinker.loader.app.TinkerApplication"
    override var partHookers: MutableList<PartHooker> = mutableListOf(
        DatabaseHooker(),
        TransferHooker(),
        RedPackageHooker(),
        ChatUserHooker(),
        PayToolsHooker(),
        WebViewHooker()
    )

    override var rules: MutableList<Clazz>
        get() = mutableListOf(
            Clazz(
                type = "class",
                name = "remittance.model",
                nameRule = "com.tencent.mm.plugin.remittance.model.\\w+",
                methods = arrayListOf(
                    ClazzMethod(
                        name = "constructor",
                        parameters = arrayListOf(
                            // int v, String s, String s1, int v1, String s2
                            ClazzField(
                                type = "int"
                            ),
                            ClazzField(
                                type = "java.lang.String"
                            ),
                            ClazzField(
                                type = "java.lang.String"
                            ),
                            ClazzField(
                                type = "int"
                            ),
                            ClazzField(
                                type = "java.lang.String"
                            ),
                        )
                    ),

                    ClazzMethod(
                        name = "onGYNetEnd",
                        returnType = "void",
                        parameters = arrayListOf(
                            ClazzField(
                                type = "int"
                            ),
                            ClazzField(
                                type = "java.lang.String"
                            ),
                            ClazzField(
                                type = "org.json.JSONObject"
                            ),
                        )

                    )
                )
            ),
            Clazz(
                type = "class",
                name = "luckymoney.model",
                nameRule = "com.tencent.mm.plugin.luckymoney.model.\\w+",
                methods = arrayListOf(
                    ClazzMethod(
                        name = "constructor",
                        parameters = arrayListOf(
                            // public n5(int v, int v1, String s, String s1, String s2, String s3, String s4, String s5, String s6, String s7) {
                            //
                            ClazzField(
                                type = "int"
                            ),
                            ClazzField(
                                type = "int"
                            ),
                            ClazzField(
                                type = "java.lang.String"
                            ),
                            ClazzField(
                                type = "java.lang.String"
                            ),
                            ClazzField(
                                type = "java.lang.String"
                            ),
                            ClazzField(
                                type = "java.lang.String"
                            ),
                            ClazzField(
                                type = "java.lang.String"
                            ),
                            ClazzField(
                                type = "java.lang.String"
                            ),
                            ClazzField(
                                type = "java.lang.String"
                            ),
                            ClazzField(
                                type = "java.lang.String"
                            ),
                        )
                    ),

                    ClazzMethod(
                        name = "onGYNetEnd",
                        returnType = "void",
                        parameters = arrayListOf(
                            ClazzField(
                                type = "int"
                            ),
                            ClazzField(
                                type = "java.lang.String"
                            ),
                            ClazzField(
                                type = "org.json.JSONObject"
                            ),
                        )

                    )
                )
            ),
            Clazz(
                type = "class",
                name = "wechat_user",
                fields = arrayListOf(
                    ClazzField(
                        type = "java.lang.String",
                        name = "field_conRemark"
                    ),
                    ClazzField(
                        type = "java.lang.String",
                        name = "field_nickname"
                    ),
                    ClazzField(
                        type = "java.lang.String",
                        name = "field_username"
                    ),
                ),
                methods = arrayListOf(
                    ClazzMethod(
                        name = "convertFrom",
                        parameters = arrayListOf(
                            ClazzField(
                                type = "android.database.Cursor"
                            )
                        )
                    )
                )
            ),

        )
        set(value) {}

    override var clazz = hashMapOf(
        "remittance.model" to "" //8.0.48 com.tencent.mm.plugin.remittance.model.c1.onGYNetEnd
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


