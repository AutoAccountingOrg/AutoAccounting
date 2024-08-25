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

package net.ankio.auto.hooks.android.hooks

import android.app.Application
import android.app.Notification
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.api.PartHooker


class NotificationHooker:PartHooker {
    override fun hook(
        hookerManifest: HookerManifest,
        application: Application?,
        classLoader: ClassLoader
    ) {

        val notificationManagerService = XposedHelpers.findClass(
            "com.android.server.notification.NotificationManagerService",
            classLoader
        )

        XposedBridge.hookAllMethods(
            notificationManagerService,
            "enqueueNotificationInternal",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val app = param.args[0] as String
                    val opkg = param.args[1] as String

                    var notification: Notification? = null

                    for (i in 0 until param.args.size) {
                        if (param.args[i] is Notification) {
                            notification = param.args[i] as Notification
                            break
                        }
                    }

                    if (notification == null) {
                        XposedBridge.log("unknown notification")
                        return
                    }


                    val originalTitle = notification.extras.getString(Notification.EXTRA_TITLE)
                    val originalText = notification.extras.getString(Notification.EXTRA_TEXT)


                    hookerManifest.logD("Notification App: $opkg")
                    hookerManifest.logD("Notification App2: $app")
                    hookerManifest.logD("Notification Title: $originalTitle")
                    hookerManifest.logD("Notification Content: $originalText")

                    // TODO 1、判断是否在监测范围
                    // TODO 2、调用analyze函数进行分析


                }
            }
        )


    }
}