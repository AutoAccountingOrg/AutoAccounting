/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.content.res.ResourcesCompat

data class AppInfo(val name: String, val icon: Drawable?, val version: String)


class AppInfoUtils(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    fun getAppInfoFromPackageName(packageName: String): AppInfo? {
        try {

            val app: ApplicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                context.packageManager
                    .getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            }

            val appName = packageManager.getApplicationLabel(app).toString()


            val appIcon =  try {
                val resources: Resources = context.packageManager.getResourcesForApplication(app.packageName)
                ResourcesCompat.getDrawable(resources,app.icon, context.theme)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                null
            }


            val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                context.packageManager
                    .getPackageInfo(packageName, PackageManager.GET_META_DATA)
            }
            val appVersion = packageInfo.versionName
            return AppInfo(appName, appIcon, appVersion)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }
}