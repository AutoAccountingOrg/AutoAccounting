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

package net.ankio.auto.xposed.hooks.auto

import android.Manifest
import android.os.Build
import net.ankio.auto.BuildConfig
import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.hooks.auto.hooks.ActiveHooker
import net.ankio.auto.xposed.hooks.common.CommonHooker
import net.ankio.dex.model.Clazz


class AutoHooker : HookerManifest() {
    override var minVersion: Long = 212
    override val packageName: String
        get() = BuildConfig.APPLICATION_ID
    override val appName: String = "自动记账"
    override fun hookLoadPackage() {
        if (BuildConfig.DEBUG) {
            CommonHooker.init()
        }
    }

    override var partHookers: MutableList<PartHooker> = mutableListOf(
        ActiveHooker()
    )
    override var rules: MutableList<Clazz>
        get() = mutableListOf()
        set(value) {}

    override var permissions: MutableList<String> =
        mutableListOf(
            //网络权限
            Manifest.permission.INTERNET,
            //读取网络状态
            Manifest.permission.ACCESS_NETWORK_STATE,
            //悬浮窗权限
            Manifest.permission.SYSTEM_ALERT_WINDOW
        )

    init {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 (API 30)
            permissions.add(Manifest.permission.QUERY_ALL_PACKAGES)
        }


    }
}
