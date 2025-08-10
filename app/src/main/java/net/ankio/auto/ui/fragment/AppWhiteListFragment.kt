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
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.navigation.fragment.findNavController
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentNoticeBinding
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.storage.SpUtils
import net.ankio.auto.ui.adapter.AppAdapter
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.components.MaterialSearchView
import net.ankio.auto.ui.components.WrapContentLinearLayoutManager
import net.ankio.auto.ui.models.AppInfo
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting

/**
 * 通知来源应用选择 Fragment
 *
 * - 列出已安装应用，支持搜索过滤
 * - 勾选将应用加入通知监听白名单
 * - 离开页面时持久化保存到 appWhiteList
 */
class AppWhiteListFragment : BasePageFragment<AppInfo, FragmentNoticeBinding>() {

    /** 搜索关键字 */
    private var query: String = ""

    /** 当前已选择的应用包名集合 */
    private var selectedApps: MutableList<String> = mutableListOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 返回按钮
        binding.topAppBar.setNavigationOnClickListener { findNavController().popBackStack() }
        // 搜索
        setUpSearch()
    }

    /**
     * 加载数据：读取本机安装应用并按搜索条件与已选状态进行排序
     */
    override suspend fun loadData(): List<AppInfo> {
        // 读取配置中的白名单（以逗号分隔）
        selectedApps = PrefManager.appWhiteList
        Logger.d("selected apps: $selectedApps")

        val pm = requireContext().packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .map { applicationInfo ->
                AppInfo(
                    appName = applicationInfo.loadLabel(pm).toString() ?: "",
                    packageName = applicationInfo.packageName,
                    icon = applicationInfo.loadIcon(pm),
                    pkg = applicationInfo
                )
            }
            .toMutableList()

        // 标记选中状态
        apps.forEach { it.isSelected = selectedApps.contains(it.packageName) }

        // 按搜索过滤
        val filtered = if (query.isEmpty()) {
            apps
        } else {
            apps.filter {
                it.appName.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }.toMutableList()
        }

        // 排序
        sortApps(filtered)
        return filtered
    }

    /**
     * 创建并返回适配器，同时设置 RecyclerView 布局管理器
     */
    override fun onCreateAdapter(): AppAdapter {
        val recyclerView = binding.statusPage.contentView!!
        recyclerView.layoutManager = WrapContentLinearLayoutManager(requireContext())
        return AppAdapter(requireActivity().packageManager) { appInfo ->
            // 根据当前选中状态就地更新白名单列表，保持唯一性
            if (!appInfo.isSelected) {
                // 当前为未选中状态，表示用户取消选择 → 从白名单移除
                selectedApps.removeAll { packageName -> packageName == appInfo.packageName }
            } else {
                // 当前为选中状态，表示用户选择 → 加入白名单（避免重复）
                if (!selectedApps.contains(appInfo.packageName)) {
                    selectedApps.add(appInfo.packageName)
                }
            }
        }
    }

    /**
     * 页面停止时持久化保存选中应用列表
     */
    override fun onStop() {
        super.onStop()
        PrefManager.appWhiteList = selectedApps
    }

    /**
     * 设置顶部搜索
     */
    private fun setUpSearch() {
        val searchItem = binding.topAppBar.menu.findItem(R.id.action_search)
        if (searchItem != null) {
            val searchView = searchItem.actionView as MaterialSearchView
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = true
                override fun onQueryTextChange(newText: String?): Boolean {
                    query = newText ?: ""
                    reload()
                    return true
                }
            })
        }
    }

    /**
     * 应用排序：优先显示已选应用、金融相关关键词、用户应用、名称
     */
    private fun sortApps(mutableList: MutableList<AppInfo>) {
        val financialKeywords = listOf("bank", "finance", "wallet", "pay")
        mutableList.forEach { it.isSelected = selectedApps.contains(it.packageName) }
        mutableList.sortWith(
            compareByDescending<AppInfo> { it.isSelected }
                .thenByDescending { appInfo ->
                    financialKeywords.any { keyword ->
                        appInfo.pkg.packageName.contains(keyword, ignoreCase = true)
                    }
                }
                .thenByDescending { appInfo ->
                    (appInfo.pkg.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                }
                .thenBy { it.appName }
        )
        Logger.d("sorted apps: $mutableList")
    }
}

