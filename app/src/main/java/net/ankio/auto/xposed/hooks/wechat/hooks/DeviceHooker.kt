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
        hookBuild()
        hookAsSamsung()
        hookPref()
    }

    private fun hookPref() {
        val clazz = AppRuntime.manifest.clazz("wechatPreference")
        val method = AppRuntime.manifest.method("wechatPreference", "setBoolean")
        Hooker.before(clazz, method, Boolean::class.javaPrimitiveType!!) {
            val args = it.args
            val str = args[0] as String
            val bool = args[1] as Boolean
            if (str == "phone_and_pad") {
                args[1] = false
            }
        }
    }

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
            "samsung"
        )
        XposedHelpers.setStaticObjectField(
            build,
            "BRAND",
            "samsung"
        )
        XposedHelpers.setStaticObjectField(
            build,
            "MODEL",
            "SM-F9560"
        )
    }

}