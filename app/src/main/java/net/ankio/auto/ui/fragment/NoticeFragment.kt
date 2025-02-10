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
import androidx.appcompat.widget.SearchView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentNoticeBinding
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.storage.SpUtils
import net.ankio.auto.ui.adapter.AppAdapter
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.componets.MaterialSearchView
import net.ankio.auto.ui.componets.WrapContentLinearLayoutManager
import net.ankio.auto.ui.models.AppInfo
import net.ankio.auto.ui.utils.viewBinding
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting


class NoticeFragment : BasePageFragment<AppInfo>() {
    private var query = ""
    override suspend fun loadData(callback: (resultData: List<AppInfo>) -> Unit) {
        if (appsList.isEmpty()) {
            loadApps()
        }
        val newFiltered = mutableListOf<AppInfo>()
        if (query.isEmpty()) {
            if (pageData.isEmpty()) {
                newFiltered.addAll(appsList)
            }

        } else {
            val filter = appsList.filter {
                it.appName.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
            if (pageData != filter) {
                newFiltered.addAll(filter)
            }
        }
        sorApps(newFiltered)
        callback(newFiltered)
    }

    private fun sorApps(mutableList: MutableList<AppInfo>) {
        val financialKeywords = listOf("bank", "finance", "wallet", "pay")
        mutableList.map {
            it.isSelected = selectedApps.contains(it.packageName)
        }
        mutableList.sortWith(
            compareByDescending<AppInfo> { it.isSelected }
                .thenByDescending { app ->
                    // 检查是否是金融类应用
                    financialKeywords.any { keyword ->
                        app.pkg.packageName.contains(keyword, ignoreCase = true)
                    }
                }
                .thenByDescending {
                    (it.pkg.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                }
                .thenBy {
                    it.appName
                }
        )
        Logger.d("sorted apps: $mutableList")
    }

    override fun onCreateAdapter() {
        selectedApps =
            ConfigUtils.getString(Setting.LISTENER_APP_LIST, DefaultData.NOTICE_FILTER).split(",")
                .filter { it.isNotEmpty() }
        Logger.d("selected apps: $selectedApps")
        val recyclerView = binding.statusPage.contentView!!
        recyclerView.layoutManager = WrapContentLinearLayoutManager(requireContext())
        recyclerView.adapter = AppAdapter(requireActivity().packageManager) {
            selectedApps = if (!it.isSelected) {
                selectedApps.filter { packageName -> packageName != it.packageName }
            } else {
                selectedApps + it.packageName
            }
            selectedApps = selectedApps.distinct()

        }
    }

    override fun onStop() {
        super.onStop()
        val str = selectedApps.joinToString(",")
        ConfigUtils.putString(Setting.LISTENER_APP_LIST, str)
        SpUtils.putString(Setting.LISTENER_APP_LIST, str)
    }

    private var selectedApps: List<String> = emptyList()
    private val appsList = mutableListOf<AppInfo>()
    private var filtered = mutableListOf<AppInfo>()
    override val binding: FragmentNoticeBinding by viewBinding(FragmentNoticeBinding::inflate)

    private suspend fun loadApps() = withContext(Dispatchers.IO) {
        val pm = requireContext().packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            // .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map {
                AppInfo(
                    appName = it.loadLabel(pm).toString(),
                    packageName = it.packageName,
                    icon = it.loadIcon(pm),
                    pkg = it
                )
            }


        withContext(Dispatchers.Main) {
            appsList.clear()
            appsList.addAll(apps)
            sorApps(appsList)
            filtered.clear()
            filtered.addAll(appsList)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpSearch()

    }

    private fun setUpSearch() {
        val searchItem = binding.topAppBar.menu.findItem(R.id.action_search)
        if (searchItem != null) {
            val searchView = searchItem.actionView as MaterialSearchView
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    query = newText ?: ""
                    reload()
                    return true
                }

            })
        }
    }

}