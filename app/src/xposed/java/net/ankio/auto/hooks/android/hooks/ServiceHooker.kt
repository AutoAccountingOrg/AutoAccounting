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
import net.ankio.auto.BuildConfig
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.api.PartHooker
import org.ezbook.server.Server


/**
 *
 */
class ServiceHooker : PartHooker() {

    override fun hook(
        hookerManifest: HookerManifest,
        application: Application?,
        classLoader: ClassLoader
    ) {
        if (BuildConfig.DEBUG) return
        // 调试模式下，不启动服务，使用自动记账本体启动服务。
        startServer(hookerManifest, application)
    }

    companion object {
        fun startServer(
            hookerManifest: HookerManifest,
            application: Application?
        ) {
            try {
                hookerManifest.logD("try start server...")
                Server(application!!).startServer()
                hookerManifest.logD(" server hook success")
            } catch (e: Exception) {
                hookerManifest.logE(e)
                hookerManifest.logD(" server onInit error: ${e.message}")
            }
        }
    }

}