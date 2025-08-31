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

package net.ankio.auto.xposed.hooks.wechat.hooks

import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.DataUtils

class PayToolsHooker : PartHooker() {

    companion object{
        const val PAY_TOOLS = "cachedPayTools"
        const val PAY_MONEY = "cachedPayMoney"
        const val PAY_SHOP = "cachedPayShop"
    }

    override fun hook() {


        Hooker.after(
            "com.tencent.kinda.framework.widget.base.MMKRichText",
            "appendText",
            String::class.java,
        ){ param ->
            val text = param.args[0] as String
            AppRuntime.manifest.logD("Text: $text")

            when {
                Regex(".*(卡|零钱).*").matches(text) -> {
                    AppRuntime.manifest.logD("支付方式Hook: $text")
                    DataUtils.set(PAY_TOOLS, text)
                }
                Regex(".*([￥$]).*").matches(text) -> {
                    AppRuntime.manifest.logD("支付金额Hook: $text")
                    DataUtils.set(PAY_MONEY, text)
                }
                Regex(".*(转账|红包|付款给).*").matches(text) -> {
                    AppRuntime.manifest.logD("支付对象hook: $text")
                    DataUtils.set(PAY_SHOP, text)
                }
            }
        }
    }

}
