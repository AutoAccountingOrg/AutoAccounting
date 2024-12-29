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

package net.ankio.auto.ui.utils

import net.ankio.auto.App
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.models.AppInfo

object AppUtils {
    fun get(pkg: String): AppInfo? {
        try {
            val pm = App.app.packageManager
            val applicationInfo = pm.getApplicationInfo(pkg, 0)
            return AppInfo(
                appName = pm.getApplicationLabel(applicationInfo).toString(),
                packageName = pkg,
                icon = pm.getApplicationIcon(applicationInfo),
                pkg = applicationInfo
            )
        } catch (e: Exception) {
            Logger.e("Failed to get pkg info: $pkg", e)
            return null
        }
    }
}