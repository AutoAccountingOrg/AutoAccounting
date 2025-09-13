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
import org.ezbook.server.tools.MemoryCache

class PayToolsHooker : PartHooker() {

    companion object{
        const val PAY_TOOLS = "cachedPayTools"
        const val PAY_MONEY = "cachedPayMoney"
        const val PAY_SHOP = "cachedPayShop"
        private const val DURATION_SECONDS = 120L
    }

    override fun hook() {


        Hooker.after(
            "com.tencent.kinda.framework.widget.base.MMKRichText",
            "appendText",
            String::class.java,
        ){ param ->
            val text = param.args[0] as String
            logD("Text: $text")
            // 这里的数据只缓存2分钟，超过2分钟自动失效
            when {
                Regex(".*(卡|零钱).*").matches(text) -> {
                    logD("支付方式Hook: $text")
                    MemoryCache.put(PAY_TOOLS, text, DURATION_SECONDS)
                }
                Regex(".*([￥$]).*").matches(text) -> {
                    logD("支付金额Hook: $text")
                    MemoryCache.put(PAY_MONEY, text, DURATION_SECONDS)
                }
                Regex(".*(转账|红包|付款给).*").matches(text) -> {
                    logD("支付对象hook: $text")
                    MemoryCache.put(PAY_SHOP, text, DURATION_SECONDS)
                }
            }
        }
    }

}
