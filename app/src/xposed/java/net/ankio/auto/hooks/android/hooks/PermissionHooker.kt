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
import android.os.Build
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.Apps
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.api.PartHooker
import net.ankio.auto.hooks.android.hooks.permission.PermissionHooker29
import net.ankio.auto.hooks.android.hooks.permission.PermissionHooker30
import net.ankio.auto.hooks.android.hooks.permission.PermissionHooker31
import net.ankio.auto.hooks.android.hooks.permission.PermissionHooker33
import net.ankio.auto.hooks.android.hooks.permission.PermissionHooker34

/**
 * PermissionHooker
 * 授权
 */
class PermissionHooker : PartHooker() {
    /**
     * hook PermissionManagerService，并自动向特定的应用程序授予特定权限。
     * @param hookerManifest HookerManifest
     * @param application Application
     */
    override fun hook(
        hookerManifest: HookerManifest,
        application: Application?,
        classLoader: ClassLoader
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE){ //14
            hookerManifest.log("PermissionHooker34")
            PermissionHooker34(hookerManifest, classLoader).startHook()
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){ //13
            hookerManifest.log("PermissionHooker33")
            PermissionHooker33(hookerManifest, classLoader).startHook()
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){ //12 - 12L
            hookerManifest.log("PermissionHooker31")
            PermissionHooker31(hookerManifest, classLoader).startHook()
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){ //11
            hookerManifest.log("PermissionHooker30")
            PermissionHooker30(hookerManifest, classLoader).startHook()
        }else{
            hookerManifest.log("PermissionHooker29") // 10
            PermissionHooker29(hookerManifest, classLoader).startHook()
        }


    }

}