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

package net.ankio.auto.xposed.hooks.android.hooks

import android.os.Build
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.hooks.android.hooks.permission.PermissionHooker29
import net.ankio.auto.xposed.hooks.android.hooks.permission.PermissionHooker30
import net.ankio.auto.xposed.hooks.android.hooks.permission.PermissionHooker31
import net.ankio.auto.xposed.hooks.android.hooks.permission.PermissionHooker33
import net.ankio.auto.xposed.hooks.android.hooks.permission.PermissionHooker34

/**
 * PermissionHooker
 * 授权
 */
class PermissionHooker : PartHooker() {
    /**
     * hook PermissionManagerService，并自动向特定的应用程序授予特定权限。
     */
    override fun hook() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE){ //14
            AppRuntime.manifest.i("PermissionHooker34")
            PermissionHooker34(AppRuntime.manifest, AppRuntime.classLoader).startHook()
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){ //13
            AppRuntime.manifest.i("PermissionHooker33")
            PermissionHooker33(AppRuntime.manifest, AppRuntime.classLoader).startHook()
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){ //12 - 12L
            AppRuntime.manifest.i("PermissionHooker31")
            PermissionHooker31(AppRuntime.manifest, AppRuntime.classLoader).startHook()
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){ //11
            AppRuntime.manifest.i("PermissionHooker30")
            PermissionHooker30(AppRuntime.manifest, AppRuntime.classLoader).startHook()
        }else{
            AppRuntime.manifest.i("PermissionHooker29") // 10
            PermissionHooker29(AppRuntime.manifest, AppRuntime.classLoader).startHook()
        }


    }

}