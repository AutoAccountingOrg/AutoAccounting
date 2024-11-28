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
import net.ankio.auto.xposed.core.App.Companion.TAG
import net.ankio.auto.xposed.core.App.Companion.modulePath
import net.ankio.auto.xposed.core.logger.Logger

object JsEngine {
    fun init() {
        // JsEngine
        // 判断当前手机的架构并选择相应的库
        val framework = when {
            Build.SUPPORTED_64_BIT_ABIS.contains("arm64-v8a") -> "arm64"
            Build.SUPPORTED_64_BIT_ABIS.contains("x86_64") -> "x86_64"
            Build.SUPPORTED_32_BIT_ABIS.contains("armeabi-v7a") -> "arm"
            Build.SUPPORTED_32_BIT_ABIS.contains("x86") -> "x86"
            else -> "unsupported"
        }

        // 如果架构不支持，则记录日志并返回
        if (framework == "unsupported") {
            Logger.logD(TAG,"Unsupported architecture")
            return
        }
        val libquickjs = modulePath.replace("/base.apk", "") + "/lib/$framework/libquickjs-android.so"
        val libmimalloc = modulePath.replace("/base.apk", "") + "/lib/$framework/libmimalloc.so"

        try {
            System.load(libmimalloc)
            System.load(libquickjs)
            Logger.logD(TAG,"Load quickjs-android success")
        } catch (e: Throwable) {
            Logger.logD(TAG,"Load quickjs-android failed : $e")
            Logger.logE(TAG,e)
        }
    }
}