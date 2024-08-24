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

package net.ankio.auto.hooks.systemui

import android.app.Application
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.api.PartHooker
import net.ankio.dex.model.Clazz
import org.ezbook.server.Server

class SystemUiHooker: HookerManifest() {

    override val packageName: String
        get() = "com.android.systemui"
    override val appName: String
        get() = "Android SystemUI"

    override fun hookLoadPackage(application: Application?,classLoader: ClassLoader) {
        try {
            Server(application!!).startServer()
            logD("SystemUi server hook success")
        } catch (e: Exception) {
            logE(e)
            logD("SystemUi server onInit error: ${e.message}")
        }
    }

    override var permissions: MutableList<String> = mutableListOf(
        //网络权限
        "android.permission.INTERNET",
        //读取网络状态
        "android.permission.ACCESS_NETWORK_STATE",
    )

    override var partHookers: MutableList<PartHooker> = mutableListOf()
    override var rules: MutableList<Clazz>
        get() = mutableListOf()
        set(value) {}



}