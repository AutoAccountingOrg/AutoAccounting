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

import android.app.Application
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.utils.AppUtils
import net.ankio.auto.xposed.hooks.wechat.hooks.ChatUserHooker
import net.ankio.auto.xposed.hooks.wechat.hooks.DatabaseHooker
import net.ankio.auto.xposed.hooks.wechat.hooks.PayToolsHooker
import net.ankio.auto.xposed.hooks.wechat.hooks.RedPackageHooker
import net.ankio.auto.xposed.hooks.wechat.hooks.TransferHooker
import net.ankio.dex.model.Clazz
import net.ankio.dex.model.ClazzField
import net.ankio.dex.model.ClazzMethod
import java.io.File


class WechatHooker : HookerManifest() {
    override val packageName: String
        get() = "com.tencent.mm"
    override val appName: String = "微信"
    override fun hookLoadPackage(application: Application?, classLoader: ClassLoader) {
        if (application == null) {
            return
        }


       /* tinker日志
       Hooker.allMethodsBefore(Hooker.loader("com.tencent.tinker.loader.shareutil.ShareTinkerLog")){
            param->
            val method = param.method
            val args = param.args
            log("ShareTinkerLog: ${method.name} ${args.joinToString(" ")}")
        }*/

        // 腾讯tinker热更新框架在加载后会导致hook无效，最简单的办法是删掉
        // 判断目录/data/data/com.tencent.mm/tinker/下是否有patch-开头的文件夹，如果有就删除
    /*    XposedHelpers.callStaticMethod(
            XposedHelpers.findClass("com.tencent.tinker.loader.shareutil.ShareTinkerInternals", application.classLoader),
            "setTinkerDisableWithSharedPreferences",
            application
        )*/
       val tinkerDir = File(application.dataDir, "tinker")

        if (tinkerDir.exists()) {
            log("tinkerDir: ${tinkerDir.absolutePath}")
            tinkerDir.listFiles()?.forEach {
                if (it.isDirectory && it.name.startsWith("patch-")) {
                    log("find tinker patch dir: ${it.absolutePath}, delete it")
                    it.deleteRecursively()
                }
           }
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
        PayToolsHooker()
    )
    override fun beforeAdapter(application: Application,file:String){
   runCatching {

           //TODO 删除tinker会导致微信冷启动变慢，主要原因是删除后微信加载tinker补丁失败，会启动另一套启动流程，甚至还会频繁触发微信闪退错误页面。
           //TODO 这里需要重新优化Xposed hook，应该hook微信加载tinker之后的application，而不是简单粗暴的删除tinker补丁。
          if (file.contains("tinker")) {
              log("disable tinker patch dir: $file")
           //    File(file).delete()
              closeTinker(application.classLoader)
               AppUtils.restart()
           }
       }.onFailure{
           logE(it)
       }
    }
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
            )
        )
        set(value) {}

    override var clazz = hashMapOf(
        "remittance.model" to "" //8.0.48 com.tencent.mm.plugin.remittance.model.c1.onGYNetEnd
    )


    private fun closeTinker(classLoader: ClassLoader){
        val tinkerManager = runCatching { XposedHelpers.findClass("com.tencent.tinker.lib.tinker.TinkerApplicationHelper",classLoader) }.getOrNull()?:return
        val applicationLike = XposedHelpers.callStaticMethod(tinkerManager,"getTinkerApplicationLike") as Class<*>
        val shareTinkerInternals = XposedHelpers.findClass("com.tencent.tinker.loader.shareutil.ShareTinkerInternals", classLoader)
        //关闭其他进程
        val application = XposedHelpers.callMethod(applicationLike,"getApplication")
        XposedHelpers.callStaticMethod(shareTinkerInternals, "killAllOtherProcess", application)
        //清除补丁
        XposedHelpers.callStaticMethod(shareTinkerInternals, "cleanPatch", applicationLike)
        //关闭tinker
        XposedHelpers.callStaticMethod(shareTinkerInternals, "setTinkerDisableWithSharedPreferences", application)
    }


}


