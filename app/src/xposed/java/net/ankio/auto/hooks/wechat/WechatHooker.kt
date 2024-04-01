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

import android.content.Context
import net.ankio.auto.api.Hooker
import net.ankio.auto.api.PartHooker
import net.ankio.auto.hooks.auto.hooks.ActiveHooker
import net.ankio.auto.hooks.wechat.hooks.ChatUserHooker
import net.ankio.auto.hooks.wechat.hooks.DatabaseHooker
import net.ankio.auto.hooks.wechat.hooks.PayToolsHooker
import net.ankio.auto.hooks.wechat.hooks.TransferHooker
import net.ankio.dex.model.Clazz
import net.ankio.dex.model.ClazzField
import net.ankio.dex.model.ClazzMethod


class WechatHooker: Hooker(){
    override val packPageName: String = "com.tencent.mm"
    override val appName: String = "微信"
    override var partHookers: MutableList<PartHooker> = arrayListOf(
        DatabaseHooker(this),
        TransferHooker(this),
        ChatUserHooker(this),
        PayToolsHooker(this)
    )

    override var clazz = hashMapOf(
        "remittance.model" to ""
    )

    override val rule = arrayListOf(
        Clazz(
            name = "remittance.model",
            nameRule = "com.tencent.mm.plugin.remittance.model\\..+",
            methods = arrayListOf(
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


    override fun hookLoadPackage(classLoader: ClassLoader, context: Context) {

    }
}
