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

import android.os.Build
import net.ankio.auto.BuildConfig
import net.ankio.auto.xposed.core.App.Companion.TAG
import net.ankio.auto.xposed.core.logger.Logger
import net.ankio.auto.xposed.core.utils.AppRuntime


object JsEngine {
    private fun initSoDir() {
        val framework = when {
            Build.SUPPORTED_64_BIT_ABIS.contains("arm64-v8a") -> "arm64"
            Build.SUPPORTED_64_BIT_ABIS.contains("x86_64") -> "x86_64"
            Build.SUPPORTED_32_BIT_ABIS.contains("armeabi-v7a") -> "arm"
            Build.SUPPORTED_32_BIT_ABIS.contains("x86") -> "x86"
            else -> "unsupported"
        }

        // 如果架构不支持，则记录日志并返回
        if (framework == "unsupported") {
            Logger.logD(TAG, "Unsupported architecture")
            return
        }

        if (AppRuntime.modulePath.contains("lspatch")) {
            runCatching {
                val pm = AppRuntime.application!!.packageManager
                val appInfo = pm.getApplicationInfo(BuildConfig.APPLICATION_ID, 0)
                // APK 文件路径
                AppRuntime.moduleSoPath = appInfo.sourceDir
            }.onFailure {
                Logger.logE(TAG, it)
            }
        }

        AppRuntime.moduleSoPath =
            AppRuntime.modulePath.replace("/base.apk", "") + "/lib/$framework/"

        Logger.logD(TAG, "Module so path: ${AppRuntime.moduleSoPath}")
    }

    fun init() {
        initSoDir()

        AppRuntime.load("mimalloc")
        AppRuntime.load("quickjs-android")
    }
}