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

package net.ankio.auto.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import net.ankio.auto.autoApp


/**
 * 检查指定包名的 App 是否已安装
 *
 * @param packageName 要检查的应用包名
 * @return 如果已安装返回 true，否则返回 false
 */
fun Context.isAppInstalled(packageName: String?): Boolean =
    packageName
        ?.takeIf { it.isNotBlank() }
        ?.let {
            runCatching {
                // Android 13+ 推荐使用新 API
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(
                        it,
                        PackageManager.PackageInfoFlags.of(0)
                    )
                } else {
                    packageManager.getPackageInfo(it, 0)
                }
            }.isSuccess
        } ?: false


data class AppInfo(
    val name: String,
    val icon: Drawable?,
    val version: String
)

fun getAppInfoFromPackageName(packageName: String): AppInfo? {
    return try {
        val pm = autoApp.packageManager

        val appInfo: ApplicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        }

        val appName = pm.getApplicationLabel(appInfo).toString()
        val appIcon = runCatching { pm.getApplicationIcon(appInfo) }.getOrNull()

        val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            pm.getPackageInfo(packageName, PackageManager.GET_META_DATA)
        }

        val appVersion = packageInfo.versionName ?: ""

        AppInfo(appName, appIcon, appVersion)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}
