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

import android.Manifest
import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.core.App
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.api.PartHooker
import net.ankio.dex.model.Clazz
import org.ezbook.server.Server

class SystemUiHooker: HookerManifest() {

    override val packageName: String
        get() = "com.google.android.euicc"
    override val appName: String
        get() = "Android SystemUI"

    override fun hookLoadPackage(application: Application?,classLoader: ClassLoader) {
        log("SystemUi server hook start")
        startServer(application!!)
    }

    private fun startServer(application: Application){
        try {
            log("try start server...")
            Server(application).startServer()
            logD("SystemUi server hook success")
        } catch (e: Exception) {
            logE(e)
            logD("SystemUi server onInit error: ${e.message}")
        }
    }

    override var permissions: MutableList<String> = mutableListOf(
        //网络权限
        Manifest.permission.INTERNET,
        //读取网络状态
        Manifest.permission.ACCESS_NETWORK_STATE,
    )

    override var partHookers: MutableList<PartHooker> = mutableListOf()
    override var rules: MutableList<Clazz>
        get() = mutableListOf()
        set(value) {}



}