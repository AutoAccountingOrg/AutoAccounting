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

import io.github.oshai.kotlinlogging.KotlinLogging
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.DataUtils
import org.ezbook.server.constant.DataType

private val logger = KotlinLogging.logger {}

class TransferHooker : PartHooker() {
    override fun hook() {
        val model = AppRuntime.manifest.clazz("remittance.model")
        Hooker.after(
            model,
            "onGYNetEnd",
            Int::class.java,
            String::class.java,
            org.json.JSONObject::class.java
        ){
            val json = it.args[2] as org.json.JSONObject
            //payer_name
            val nameKey = if (json.getBoolean("is_payer")) "payer_name" else "receiver_name"
            val nameValue = json.getString(nameKey)
            json.put(ChatUserHooker.CHAT_USER, ChatUserHooker.get(nameValue))
            json.put("cachedPayTools", DataUtils.get("cachedPayTools"))
            json.put("cachedPayMoney", DataUtils.get("cachedPayMoney"))
            json.put("cachedPayShop", DataUtils.get("cachedPayShop"))
            json.put("t", System.currentTimeMillis())
            logger.debug { "Wechat Transfer hook： $json" }
            AppRuntime.manifest.analysisData(DataType.DATA, json.toString())
        }
    }


}