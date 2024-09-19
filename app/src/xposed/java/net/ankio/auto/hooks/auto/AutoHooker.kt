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

package net.ankio.auto.hooks.auto

import android.Manifest
import android.app.Application
import android.os.Build
import net.ankio.auto.BuildConfig
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.api.PartHooker
import net.ankio.auto.hooks.auto.hooks.ActiveHooker
import net.ankio.dex.model.Clazz


class AutoHooker : HookerManifest() {
    override var minVersion: Int = 212
    override val packageName: String
        get() = "net.ankio.auto.xposed"
    override val appName: String = "自动记账"
    override fun hookLoadPackage(application: Application?, classLoader: ClassLoader) {

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
            Manifest.permission.SYSTEM_ALERT_WINDOW,
          //  "android.permission.SYSTEM_OVERLAY_WINDOW",
            // Query all packages
            Manifest.permission.QUERY_ALL_PACKAGES,

            Manifest.permission.FOREGROUND_SERVICE
        )
}
