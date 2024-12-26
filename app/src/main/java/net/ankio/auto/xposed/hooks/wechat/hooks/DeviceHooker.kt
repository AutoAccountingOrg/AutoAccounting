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
import net.ankio.auto.xposed.core.logger.Logger
import net.ankio.auto.xposed.core.utils.AppRuntime
import org.ezbook.server.constant.DefaultData

class DeviceHooker:PartHooker() {
    override fun hook() {

        val pkg = AppRuntime.application!!.packageName
        val alias = DefaultData.WECHAT_PACKAGE_ALIAS
        Logger.logD("DeviceHooker", "hook $pkg, alias $alias")
        if (pkg != alias){
            return
        }
        hookAsSamsung()
    }

    private fun hookAsSamsung(){
        val clazz = AppRuntime.manifest.clazz("wechatTablet")
        val method = AppRuntime.manifest.method("wechatTablet","isSamsungFoldableDevice")
        Hooker.after(clazz,method){
            val result = it.result as Boolean
            Logger.log("DeviceHooker","Origin isSamsungFoldableDevice result: $result")
            it.result = true
            Logger.log("DeviceHooker","Change isSamsungFoldableDevice result: true")
        }
    }


}