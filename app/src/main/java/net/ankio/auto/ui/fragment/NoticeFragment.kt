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

package net.ankio.auto.ui.fragment

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.utils.AppInfo


class NoticeFragment: BasePageFragment<AppInfo>() {

    private var searchKey: String = ""
    override suspend fun loadData(callback: (resultData: List<AppInfo>) -> Unit) {
        resetPage()
        // 获取PackageManager实例
        val packageManager: PackageManager = requireActivity().packageManager


        // 获取所有已安装的应用程序
        val packageInfos = packageManager.getInstalledPackages(0)


        // 创建列表用于存储应用程序信息
        val appInfos: MutableList<AppInfo> = ArrayList()


        // 遍历应用程序列表并填充数据类
        for (packageInfo in packageInfos) {
            val applicationInfo = packageInfo.applicationInfo

            val packageName = packageInfo.packageName
            val appName = packageManager.getApplicationLabel(applicationInfo).toString()

            if (searchKey!=="" && !appName.contains(searchKey)) {
                continue
            }

            val appIcon = packageManager.getApplicationIcon(applicationInfo)
            val isSystemApp = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isSelected = false // 默认未选择

            // 创建AppInfo对象并添加到列表
            val appInfo = AppInfo(packageName, appName, appIcon, isSystemApp, isSelected)
            appInfos.add(appInfo)
        }

        // 按是否已选择和应用名称排序
        appInfos.sortWith { o1, o2 ->
            // 首先按是否已选择排序（已选择在前面）
            if (o1.isSelected && !o2.isSelected) {
                -1
            } else if (!o1.isSelected && o2.isSelected) {
                1
            } else {
                // 然后按应用名称排序
                o1.appName.compareTo(o2.appName)
            }
        }
    }


}