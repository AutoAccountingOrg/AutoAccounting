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

package net.ankio.auto.hooks.wechat.hooks

import android.app.Application
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.core.App
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.api.PartHooker

class PayToolsHooker : PartHooker() {

    override fun hook(
        hookerManifest: HookerManifest,
        application: Application?,
        classLoader: ClassLoader
    ) {
        XposedHelpers.findAndHookMethod(
            "com.tencent.kinda.framework.widget.base.MMKRichText",
            classLoader,
            "appendText",
            String::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val text = param.args[0] as String
                    hookerManifest.logD("Text: $text")
                    when {

                        text.contains(Regex("\\d{4}")) -> {
                            hookerManifest.logD("卡号Hook: $text")
                            App.set("cachedPayToolsNumber", text)
                        }

                        text.contains("卡") || text.contains("零钱") -> {
                            hookerManifest.logD("支付方式Hook: $text")
                            App.set("cachedPayTools", text)
                        }

                        text.contains("￥") || text.contains("$") -> {
                            hookerManifest.logD("支付金额Hook: $text")
                            App.set("cachedPayMoney", text)
                        }

                        text.contains("转账") || text.contains("红包") || text.contains("付款给") -> {
                            hookerManifest.logD("支付对象hook: $text")
                            App.set("cachedPayShop", text)
                        }
                    }
                }
            },
        )
    }
}
