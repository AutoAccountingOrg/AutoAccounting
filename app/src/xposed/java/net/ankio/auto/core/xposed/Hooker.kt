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

package net.ankio.auto.core.xposed

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedHelpers

/**
 * Xposed hooker
 */
object Hooker {
    fun hookOnce(
        clazz: Class<*>,
        method: String,
        vararg parameterTypes: Class<*>,
        hook: (param: MethodHookParam) -> Unit
    ) {
        var unhook: XC_MethodHook.Unhook? = null
        // 一次性hook
        unhook = XposedHelpers.findAndHookMethod(
            clazz,
            method,
            *parameterTypes,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    hook(param)
                    unhook?.unhook()
                }
            })
    }

}