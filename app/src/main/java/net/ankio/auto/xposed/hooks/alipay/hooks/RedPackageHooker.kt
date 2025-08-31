/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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

package net.ankio.auto.xposed.hooks.alipay.hooks

import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AppRuntime
import org.ezbook.server.constant.DataType


class RedPackageHooker : PartHooker() {


    override fun hook() {
        val proguard = Hooker.loader("com.alipay.mobile.redenvelope.proguard.c.b")
        val syncMessage =
            Hooker.loader("com.alipay.mobile.rome.longlinkservice.syncmodel.SyncMessage")

        Hooker.before(proguard, "onReceiveMessage", syncMessage) { param ->
            val syncMessageObject = param.args[0]
            val result = XposedHelpers.callMethod(syncMessageObject, "getData") as String
            AppRuntime.manifest.logD("Hooked Alipay RedPackage： $result")
            AppRuntime.manifest.analysisData(DataType.DATA, result)
        }
    }
}