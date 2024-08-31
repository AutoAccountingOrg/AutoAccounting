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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentLogBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.storage.SpUtils
import net.ankio.auto.ui.adapter.AppAdapter
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.models.AppInfo
import net.ankio.auto.ui.models.ToolbarMenuItem
import org.ezbook.server.db.model.SettingModel


class NoticeFragment: BasePageFragment<AppInfo>() {


    //TODO 需要根据已有规则支持显示推荐勾选、一键勾选推荐应用

    private var selectedApps: List<String> = emptyList()

    override val menuList: ArrayList<ToolbarMenuItem> = arrayListOf(
        ToolbarMenuItem(R.string.item_search, R.drawable.menu_icon_search,true) {
           loadDataInside()
        }
    )

    override suspend fun loadData(callback: (resultData: List<AppInfo>) -> Unit) {
       if (page > 1){
           withContext(Dispatchers.Main) {
               callback(emptyList())
           }
           return
       }
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
            var appName = ""

            //忽略大小写
            if (searchData!=="") {
                appName = packageManager.getApplicationLabel(applicationInfo).toString()
                if (!appName.contains(searchData,true)) continue
            }

            val isSystemApp = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            if (isSystemApp)continue

            var isSelected = false
            if (selectedApps.contains(packageName)) {
                 isSelected = true // 默认未选择
            }

            // 创建AppInfo对象并添加到列表
            val appInfo = AppInfo(packageName, appName,applicationInfo, isSelected)
            appInfos.add(appInfo)
        }

        // 按是否已选择和应用名称排序
        appInfos.sortWith(compareBy<AppInfo> { !it.isSelected }.thenBy { it.appName })


        withContext(Dispatchers.Main) {
            callback(appInfos)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentLogBinding.inflate(layoutInflater)
        statusPage = binding.statusPage
        val recyclerView = binding.statusPage.contentView!!
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        scrollView = recyclerView
        selectedApps = SpUtils.getString("selectedApps", "").split(",")
        Logger.d("selectedApps => $selectedApps")
        recyclerView.adapter = AppAdapter(pageData,requireActivity().packageManager){
            selectedApps = if (!it.isSelected) {
                selectedApps.filter { packageName -> packageName != it.packageName }
            } else {
                selectedApps + it.packageName
            }
            Logger.d("selectedApps => $selectedApps")
        }

        loadDataEvent(binding.refreshLayout)

        return binding.root
    }



    override fun onPause() {
        super.onPause()
        //去重
        selectedApps = selectedApps.distinct()
        val str = selectedApps.joinToString(",")
        SpUtils.putString("selectedApps", str)
        App.launch {
            SettingModel.set("selectedApps", str)
            Logger.d("selectedApps => $selectedApps")
        }
    }
}