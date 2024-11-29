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

import android.app.AndroidAppHelper
import de.robv.android.xposed.XposedBridge
import net.ankio.auto.xposed.core.App.Companion.TAG
import net.ankio.auto.xposed.core.logger.Logger
import net.ankio.auto.xposed.core.utils.AppRuntime
import org.ezbook.server.Server

object CommonHooker {
    fun init(){
        Logger.logD(TAG, "Start server...: ${AndroidAppHelper.currentPackageName()}")
        try {
            /**
             * js引擎
             */
            JsEngine.init()
            /**
             * 解锁屏幕
             */
            UnLockScreen.init()
            /**
             * 启动自动记账服务
             */
            Server(AppRuntime.application!!).startServer()
            Logger.logD(TAG, "Server start success")
        } catch (e: Throwable) {
            XposedBridge.log("Server start failed")
            XposedBridge.log(e)
            Logger.logD(TAG,e.message?:"")
        }
    }
}