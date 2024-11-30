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
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.storage.SpUtils
import net.ankio.auto.ui.adapter.AppAdapter
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.models.AppInfo
import net.ankio.auto.ui.models.ToolbarMenuItem
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.SettingModel
import java.lang.ref.WeakReference


class NoticeFragment : BasePageFragment<AppInfo>() {


    //TODO 需要根据已有规则支持显示推荐勾选、一键勾选推荐应用

    private var selectedApps: List<String> = emptyList()

    override val menuList: ArrayList<ToolbarMenuItem> = arrayListOf(
        ToolbarMenuItem(R.string.item_search, R.drawable.menu_icon_search, true) {
            loadDataInside()
        }
    )

    override suspend fun loadData(callback: (resultData: List<AppInfo>) -> Unit) {
        if (page > 1) {
            withContext(Dispatchers.Main) {
                callback(emptyList())
            }
            return
        }
        
        val packageManager: PackageManager = requireActivity().packageManager
        val packageInfos = packageManager.getInstalledPackages(0)
        val appInfos: MutableList<AppInfo> = ArrayList()

        for (packageInfo in packageInfos) {
            val applicationInfo = packageInfo.applicationInfo
            val packageName = packageInfo.packageName
            var appName = ""

            if (searchData.isNotEmpty()) {
                appName = packageManager.getApplicationLabel(applicationInfo).toString()
                if (!appName.contains(searchData, true)) continue
            }

            (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isSelected = selectedApps.contains(packageName)

            val appInfo = AppInfo(packageName, appName, applicationInfo, isSelected)
            appInfos.add(appInfo)
        }

        // 优化排序逻辑：已选择 > 用户应用 > 系统应用
        appInfos.sortWith(
            compareByDescending<AppInfo> { it.isSelected }
                .thenByDescending { 
                    (it.pkg.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                }
                .thenBy {
                    it.appName.ifEmpty {
                        requireActivity().packageManager.getApplicationLabel(it.pkg)
                            .toString()
                    }
                }
        )

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
       // scrollView = WeakReference(recyclerView)
        selectedApps = ConfigUtils.getString(Setting.LISTENER_APP_LIST, "").split(",")
        recyclerView.adapter = AppAdapter(pageData, requireActivity().packageManager) {
            selectedApps = if (!it.isSelected) {
                selectedApps.filter { packageName -> packageName != it.packageName }
            } else {
                selectedApps + it.packageName
            }
            selectedApps = selectedApps.distinct()
            val str = selectedApps.joinToString(",")
            ConfigUtils.putString(Setting.LISTENER_APP_LIST, str)
            SpUtils.putString(Setting.LISTENER_APP_LIST, str)
            Logger.d("selectedApps => $selectedApps")
        }

        loadDataEvent(binding.refreshLayout)

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        statusPage.showLoading()
        loadDataInside()
    }

}