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
import androidx.recyclerview.widget.ConcatAdapter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentNoticeBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.AppAdapter
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.components.MaterialSearchView
import net.ankio.auto.ui.components.WrapContentLinearLayoutManager
import net.ankio.auto.ui.models.AppInfo
import net.ankio.auto.utils.PrefManager

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

    /** 应用名称缓存：减少重复 label 解析 */
    private val appNameCache: MutableMap<String, String> = mutableMapOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 返回按钮
        binding.topAppBar.setNavigationOnClickListener { findNavController().popBackStack() }
        // 搜索
        setUpSearch()
        // 将说明卡片作为 RecyclerView 头部，避免覆盖与滚动冲突
        val rv = binding.statusPage.contentView
        val current = rv?.adapter
        if (current != null && current !is ConcatAdapter) {
            rv.adapter = ConcatAdapter(WhitelistInfoHeaderAdapter(), current)
        }
    }

    /**
     * 加载数据：读取本机安装应用并按搜索条件与已选状态进行排序
     */
    override suspend fun loadData(): List<AppInfo> {
        // 读取配置中的白名单（以逗号分隔）
        selectedApps = PrefManager.appWhiteList
        Logger.d("selected apps size: ${selectedApps.size}")

        val pm = requireContext().packageManager
        // 使用 flag=0，避免不必要的 meta 读取，加快速度
        val apps = pm.getInstalledApplications(0)
            .map { applicationInfo ->
                // 不在此阶段解析图标和名称，避免 O(N) UI 阻塞
                AppInfo(
                    appName = "",
                    packageName = applicationInfo.packageName,
                    icon = null,
                    pkg = applicationInfo
                )
            }
            .toMutableList()

        // 标记选中状态
        apps.forEach { it.isSelected = selectedApps.contains(it.packageName) }

        // 按搜索过滤（当有关键字时才解析名称，并做简单缓存）
        val filtered = if (query.isEmpty()) {
            apps
        } else {
            val q = query
            apps.filter { info ->
                val name = appNameCache.getOrPut(info.packageName) {
                    runCatching { info.pkg.loadLabel(pm).toString() }.getOrDefault("")
                }
                name.contains(q, ignoreCase = true) || info.packageName.contains(
                    q,
                    ignoreCase = true
                )
            }.toMutableList()
        }

        // 排序（优先：已选 → 金融关键词 → 用户应用 → 名称/包名）
        sortApps(filtered, pm)
        return filtered
    }

    /**
     * 创建并返回适配器，同时设置 RecyclerView 布局管理器
     */
    override fun onCreateAdapter(): AppAdapter {
        val recyclerView = binding.statusPage.contentView!!
        recyclerView.layoutManager = WrapContentLinearLayoutManager(requireContext())
        return AppAdapter()
            .setPackageManager(requireActivity().packageManager)
            .setOnAppSelectionChangedListener { appInfo ->
                if (!appInfo.isSelected) {
                    selectedApps.removeAll { packageName -> packageName == appInfo.packageName }
                } else {
                    if (!selectedApps.contains(appInfo.packageName)) {
                        selectedApps.add(appInfo.packageName)
                    }
                }
                PrefManager.appWhiteList = selectedApps
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
    private fun sortApps(mutableList: MutableList<AppInfo>, pm: android.content.pm.PackageManager) {
        val financialKeywords = listOf("bank", "finance", "wallet", "pay", "支付", "银行", "钱包")
        mutableList.forEach { it.isSelected = selectedApps.contains(it.packageName) }
        mutableList.sortWith(
            compareByDescending<AppInfo> { it.isSelected }
                .thenByDescending { appInfo ->
                    financialKeywords.any { keyword ->
                        appInfo.pkg.packageName.contains(keyword, ignoreCase = true) ||
                                appNameCache.getOrPut(appInfo.packageName) {
                                    runCatching {
                                        appInfo.pkg.loadLabel(pm).toString()
                                    }.getOrDefault("")
                                }.contains(keyword, ignoreCase = true)
                    }
                }
                .thenByDescending { appInfo ->
                    (appInfo.pkg.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                }
                .thenBy { appInfo ->
                    val name = appNameCache.getOrPut(appInfo.packageName) {
                        runCatching { appInfo.pkg.loadLabel(pm).toString() }.getOrDefault("")
                    }
                    if (name.isNotEmpty()) name else appInfo.packageName
                }
        )
        Logger.d("sorted apps size: ${mutableList.size}")
    }

    private class WhitelistInfoHeaderAdapter :
        RecyclerView.Adapter<WhitelistInfoHeaderAdapter.VH>() {
        class VH(val view: View) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_whitelist_info, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val card = holder.view.findViewById<MaterialCardView>(R.id.whitelist_info_card)
            card.setCardBackgroundColor(net.ankio.auto.ui.theme.DynamicColors.SurfaceColor2)
        }

        override fun getItemCount(): Int = 1
        override fun getItemViewType(position: Int): Int = R.layout.item_whitelist_info
    }
}

