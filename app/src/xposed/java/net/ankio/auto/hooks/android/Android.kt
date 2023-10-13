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

package net.ankio.auto.hooks.android

import android.content.Context
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.api.Hooker
import net.ankio.auto.api.PartHooker


class Android : Hooker() {
    override var partHookers: MutableList<PartHooker> = arrayListOf()
    override fun hookLoadPackage(classLoader: ClassLoader?, context: Context?) {
        val activityManagerService = XposedHelpers.findClass("com.android.server.am.ActivityManagerService", classLoader)
        val serviceManager = XposedHelpers.findClass("android.os.ServiceManager", classLoader)
        XposedBridge.hookAllConstructors(activityManagerService, object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun afterHookedMethod(param: MethodHookParam) {
                val mAccountingService = AccountingService(
                    XposedHelpers.getObjectField(
                        param.thisObject,
                        "mContext"
                    ) as Context
                )

                XposedHelpers.callStaticMethod(
                    serviceManager,
                    "addService",
                    mAccountingService.getServiceName(),
                    mAccountingService,
                    true
                )

                XposedBridge.hookAllMethods(
                    activityManagerService,
                    "systemReady",
                    object : XC_MethodHook() {
                        @Throws(Throwable::class)
                        override fun afterHookedMethod(param: MethodHookParam) {
                            mAccountingService.systemReady()
                        }
                    })
            }
        })
    }

    override val packPageName: String = "android"
    override val appName: String = "Android系统"
    override val needHelpFindApplication: Boolean = false


}