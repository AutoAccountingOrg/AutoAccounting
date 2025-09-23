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
    PackageManagerCompat.isPackageInstalled(packageManager, packageName)



/**
 * 将Context转换为主题化的Context
 * 应用当前主题设置（动态颜色、夜间模式等）
 */
fun Context.toThemeCtx(): Context {
    return ThemeUtils.themedCtx(this)
}

data class InstalledAppInfo(
    val name: String,
    val icon: Drawable?,
    val version: String
)

fun getAppInfoFromPackageName(packageName: String): InstalledAppInfo? {
    val pm = autoApp.packageManager
    
    val appInfo = PackageManagerCompat.getApplicationInfo(pm, packageName) ?: return null
    val packageInfo = PackageManagerCompat.getPackageInfo(pm, packageName) ?: return null
    
    val appName = pm.getApplicationLabel(appInfo).toString()
    val appIcon = runCatching { pm.getApplicationIcon(appInfo) }.getOrNull()
    val appVersion = packageInfo.versionName ?: ""

    return InstalledAppInfo(appName, appIcon, appVersion)
}
