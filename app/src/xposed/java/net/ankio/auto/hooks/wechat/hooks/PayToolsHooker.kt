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

import android.content.Context
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.api.Hooker
import net.ankio.auto.api.PartHooker
import net.ankio.auto.utils.HookUtils

class PayToolsHooker(hooker: Hooker) : PartHooker(hooker) {
    override val hookName: String
        get() = "支付方式Hook"

    override fun onInit(
        classLoader: ClassLoader,
        context: Context,
    ) {
        XposedHelpers.findAndHookMethod(
            "com.tencent.kinda.framework.widget.base.MMKRichText",
            classLoader,
            "appendText",
            String::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val text = param.args[0] as String
                    logD("Text: $text")
                    when {
                        text.contains("卡(") || text.contains("零钱") -> {
                            logD("支付方式Hook: $text")
                            HookUtils.writeData("cachedPayTools", text)
                        }
                        text.contains("￥") || text.contains("$") -> {
                            logD("支付金额Hook: $text")
                            HookUtils.writeData("cachedPayMoney", text)
                        }
                        text.contains("转账") || text.contains("红包") || text.contains("付款给") -> {
                            logD("支付对象hook: $text")
                            HookUtils.writeData("cachedPayShop", text)
                        }
                    }
                }
            },
        )
    }
}
