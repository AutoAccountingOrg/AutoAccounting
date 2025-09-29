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

package net.ankio.auto.xposed.hooks.common

import net.ankio.auto.BuildConfig
import net.ankio.auto.xposed.core.App.Companion.TAG
import net.ankio.auto.xposed.core.logger.Logger
import net.ankio.auto.xposed.core.utils.AppRuntime
import org.ezbook.server.Server
import java.net.ServerSocket

object CommonHooker {

    /**
     * 检查端口是否被占用。
     * 占用则返回 true；未占用则返回 false。
     */
    private fun isPortOccupied(port: Int): Boolean {
        return try {
            ServerSocket(port).use { false }
        } catch (_: Throwable) {
            true
        }
    }
    fun init() {
        Logger.d("Start server...: ${AppRuntime.manifest.packageName}")
        // 端口占用检查：最前置，未通过则直接返回，避免后续初始化浪费
        if (isPortOccupied(Server.PORT)) {
            Logger.d("Server port ${Server.PORT} is occupied, skip start")
            return
        }
        Logger.d("Start server...: ${AppRuntime.manifest.packageName}")
        try {
            /**
             * js引擎
             */
            JsEngine.init()
            /**
             * 启动自动记账服务
             */
            val server = Server(AppRuntime.application!!)
            Server.versionName = BuildConfig.VERSION_NAME
            Server.packageName = BuildConfig.APPLICATION_ID
            Server.debug = AppRuntime.debug
            server.startServer()
            AppInstaller.init(AppRuntime.application!!, server)
            UnLockScreen.init()
            Logger.d("Server start success")
        } catch (e: Throwable) {
            Logger.d(e.message ?: "")
        }
    }
}