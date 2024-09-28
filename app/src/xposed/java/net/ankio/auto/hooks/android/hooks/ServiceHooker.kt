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
import android.content.Intent
import android.os.UserHandle
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.BuildConfig
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.api.PartHooker

class ServiceHooker:PartHooker() {
    override fun hook(
        hookerManifest: HookerManifest,
        application: Application?,
        classLoader: ClassLoader
    ) {
        // UserHandle(USER_SYSTEM);
         XposedHelpers.findAndHookMethod(
            "android.app.ContextImpl",
            classLoader,
            "startServiceCommon",
            Intent::class.java,
            Boolean::class.javaPrimitiveType,
            UserHandle::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                  runCatching {
                      val intent = param.args[0] as Intent
                      val component = intent.component ?: return

                      if (component.packageName == BuildConfig.APPLICATION_ID){
                          val rawUserHandle = param.args[2]
                          val isSystem  = XposedHelpers.callMethod(rawUserHandle,"isSystem")
                          hookerManifest.log("Raw User Handler is System: ${isSystem}, ${rawUserHandle}")
                          val systemUserHandle = XposedHelpers.newInstance(XposedHelpers.findClass("android.os.UserHandle",classLoader),0)
                          param.args[2] = systemUserHandle
                          hookerManifest.log("New User Handler is : ${systemUserHandle}")
                      }
                  }
                }
            }
        )

    }
}