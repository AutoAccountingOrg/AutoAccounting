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
import org.ezbook.server.constant.DataType


class TransferHooker : PartHooker() {
    override fun hook(
        hookerManifest: HookerManifest,
        application: Application?,
        classLoader: ClassLoader
    ) {

        val model = hookerManifest.clazz("remittance.model",classLoader)
        XposedHelpers.findAndHookMethod(model, "onGYNetEnd",
            Int::class.java,
            String::class.java,
            org.json.JSONObject::class.java, object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {

                    val json = param.args[2] as org.json.JSONObject

                    json.put("hookUser", App.get("hookerUser"))
                    json.put("cachedPayTools", App.get("cachedPayTools"))
                    json.put("cachedPayMoney", App.get("cachedPayMoney"))
                    json.put("cachedPayShop", App.get("cachedPayShop"))
                    hookerManifest.logD("Wechat Transfer hook： $json")
                    hookerManifest.analysisData(DataType.DATA, json.toString())
                }
            })

    }


}