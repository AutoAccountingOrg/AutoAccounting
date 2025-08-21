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

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

/**
 * PackageManager 兼容性工具类
 * 统一处理不同 Android 版本的 API 差异，消除重复的版本判断代码
 */
object PackageManagerCompat {

    /**
     * 获取包信息，兼容不同API版本
     * @param packageManager PackageManager实例
     * @param packageName 包名
     * @param flags 标志位，默认为0
     * @return PackageInfo对象，获取失败返回null
     */
    fun getPackageInfo(
        packageManager: PackageManager,
        packageName: String,
        flags: Long = 0
    ): PackageInfo? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(flags)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, flags.toInt())
            }
        }.getOrNull()
    }

    /**
     * 获取应用信息，兼容不同API版本
     * @param packageManager PackageManager实例
     * @param packageName 包名
     * @param flags 标志位，默认为0
     * @return ApplicationInfo对象，获取失败返回null
     */
    fun getApplicationInfo(
        packageManager: PackageManager,
        packageName: String,
        flags: Long = 0
    ): ApplicationInfo? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(flags)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, flags.toInt())
            }
        }.getOrNull()
    }

    /**
     * 检查应用是否已安装
     * @param packageManager PackageManager实例
     * @param packageName 包名
     * @return 已安装返回true，否则返回false
     */
    fun isPackageInstalled(packageManager: PackageManager, packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return getPackageInfo(packageManager, packageName) != null
    }
}
