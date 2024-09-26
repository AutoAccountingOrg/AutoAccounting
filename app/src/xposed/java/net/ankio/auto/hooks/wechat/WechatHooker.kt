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

package net.ankio.auto.hooks.wechat

import android.app.Application
import net.ankio.auto.core.App
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.api.PartHooker
import net.ankio.auto.hooks.wechat.hooks.ChatUserHooker
import net.ankio.auto.hooks.wechat.hooks.DatabaseHooker
import net.ankio.auto.hooks.wechat.hooks.PayToolsHooker
import net.ankio.auto.hooks.wechat.hooks.RedPackageHooker
import net.ankio.auto.hooks.wechat.hooks.TransferHooker
import net.ankio.dex.model.Clazz
import net.ankio.dex.model.ClazzField
import net.ankio.dex.model.ClazzMethod


class WechatHooker : HookerManifest() {
    override val packageName: String
        get() = "com.tencent.mm"
    override val appName: String = "微信"
    override fun hookLoadPackage(application: Application?, classLoader: ClassLoader) {
       // App.set("adaptation","")
    }

    override var minVersion: Int
        get() = 0
        set(value) {}

    override var applicationName = "com.tencent.mm.app.Application"
    override var partHookers: MutableList<PartHooker> = mutableListOf(
        DatabaseHooker(),
        TransferHooker(),
        RedPackageHooker(),
        ChatUserHooker(),
        PayToolsHooker()
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
            )
        )
        set(value) {}

    override var clazz = hashMapOf(
        "remittance.model" to "" //8.0.48 com.tencent.mm.plugin.remittance.model.c1.onGYNetEnd
    )


}


