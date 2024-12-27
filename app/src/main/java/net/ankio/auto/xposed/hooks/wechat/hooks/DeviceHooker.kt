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

import android.os.Build
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AppRuntime
import org.ezbook.server.constant.DefaultData

class DeviceHooker : PartHooker() {
    override fun hook() {
        val pkg = AppRuntime.application!!.packageName
        val alias = DefaultData.WECHAT_PACKAGE_ALIAS
        if (pkg != alias) {
            return
        }

        hookModel()
        hookBuild()
        hookAsSamsung()

    }
    private fun hookModel() {
        val defaultModel = Build.MODEL
        val clazz = AppRuntime.manifest.clazz("wechatModelChild").superclass
        Hooker.allMethodsAfter(clazz) { it, method ->

            if (method.parameters.size != 1) {
                return@allMethodsAfter null
            }

            if (method.parameters[0].type != String::class.java) {
                return@allMethodsAfter null
            }

            if (method.returnType != String::class.java) {
                return@allMethodsAfter null
            }

          //AppRuntime.manifest.log("raw model: ${it.result}")

            if ( it.result == defaultModel){
                it.result = MODEL
            }

          //  AppRuntime.manifest.log("replace model: ${it.result}")
            null
        }
    }

    private val MANUFACTURER  = "samsung"
    private val MODEL = "SM-F9560"

    private fun hookAsSamsung() {
        val clazz = AppRuntime.manifest.clazz("wechatTablet")
        val method = AppRuntime.manifest.method("wechatTablet", "isSamsungFoldableDevice")
        Hooker.replaceReturn(clazz, method, true)
    }

    private fun hookBuild() {
        val build = Hooker.loader("android.os.Build")
        XposedHelpers.setStaticObjectField(
            build,
            "MANUFACTURER",
            MANUFACTURER
        )
        XposedHelpers.setStaticObjectField(
            build,
            "BRAND",
            MANUFACTURER
        )
        XposedHelpers.setStaticObjectField(
            build,
            "MODEL",
            MODEL
        )

        Hooker.after("android.os.SystemProperties", "get", String::class.java) {
            val args = it.args
            val key = args[0] as String
            if (key == "ro.product.model") {
                it.result = MODEL
            } else if (key == "ro.product.brand") {
                it.result = MANUFACTURER
            } else if (key == "ro.product.manufacturer") {
                it.result = MANUFACTURER
            } else if (key == "ro.build.fingerprint") {
                it.result = "$MANUFACTURER/${MANUFACTURER}/${MODEL}:${Build.VERSION.RELEASE}/${Build.ID}/${Build.VERSION.INCREMENTAL}:user/release-keys"
            } else if (key == "ro.build.characteristics") {
                it.result = "tablet"
            } else if (key == "ro.build.product") {
                it.result = MODEL
            }
        }

    }

}