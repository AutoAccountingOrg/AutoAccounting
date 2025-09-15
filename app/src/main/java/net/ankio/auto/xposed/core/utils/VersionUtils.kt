/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

package net.ankio.auto.xposed.core.utils

import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.logger.Logger

object VersionUtils {
    /**
     * 获取当前应用版本信息。
     *
     * 在 API 28+ 返回 `longVersionCode`，同时附带 `versionName`。
     * 当 `application` 为空时，返回 `0L to ""`。
     *
     * @return `Pair<versionCode, versionName>`
     */
    fun version(): Pair<Long, String> {
        val app = AppRuntime.application ?: return 0L to ""
        val pm = app.packageManager
        val pi = pm.getPackageInfo(app.packageName, 0)
        val versionName = pi.versionName ?: ""
        val codeLong = pi.longVersionCode
        return codeLong to versionName
    }

    fun check(manifest: HookerManifest): Boolean {
        if (manifest.minVersion == 0L) return true
        val (code, name) = version()

        Logger.i("应用版本号: $code, 版本名: $name")

        // 检查App版本是否过低，过低无法使用
        return code >= manifest.minVersion
    }
}