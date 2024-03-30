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
import net.ankio.auto.constant.DataType


class TransferHooker (hooker: Hooker) : PartHooker(hooker){

    override fun onInit(classLoader: ClassLoader, context: Context) {

        XposedHelpers.findAndHookMethod(hooker.clazz["remittance.model"],classLoader, "onGYNetEnd",
            Int::class.java,
            String::class.java,
            org.json.JSONObject::class.java, object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {

                    val json = param.args[2] as org.json.JSONObject

                    logD("微信转账页面数据： $json")

                    analyzeData(DataType.App.ordinal, json.toString())
                }
            })

    }


    override val hookName: String
        get() = "微信转账页面"


}