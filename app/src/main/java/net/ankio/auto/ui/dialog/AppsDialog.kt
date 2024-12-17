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

package net.ankio.auto.ui.dialog

import android.content.Context
import android.content.pm.PackageManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.databinding.DialogAppsBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.AppsAdapter
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.models.AppInfo

class AppsDialog(private val context: Context,private val callback:(AppInfo)->Unit) : BaseSheetDialog(context) {
    private lateinit var binding: DialogAppsBinding
    private lateinit var appsAdapter: AppsAdapter
    private val appsList = mutableListOf<AppInfo>()
    private var filtered = mutableListOf<AppInfo>()
    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogAppsBinding.inflate(inflater)

        setupRecyclerView()
        setupSearch()
        loadApps()
        
        return binding.root
    }

    private fun setupRecyclerView() {
        appsAdapter = AppsAdapter(filtered) { appInfo ->
            // 处理应用选择回调
            callback(appInfo)
            dismiss()
        }

        val recyclerView = binding.status.contentView!!
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = GridLayoutManager(context, 4)
        recyclerView.adapter = appsAdapter
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(afterTextChanged = { editable ->
            val text = editable.toString()
            filterApps(text)
        })
    }

    private fun loadApps() {
        binding.status.showLoading()
        lifecycleScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .map { 
                    AppInfo(
                        appName = it.loadLabel(pm).toString(),
                        packageName = it.packageName,
                        icon = it.loadIcon(pm),
                        pkg = it
                    )
                }
                .sortedBy { it.appName }
            
            withContext(Dispatchers.Main) {
                if (apps.isEmpty()) {
                    binding.status.showEmpty()
                    return@withContext
                }
                appsList.clear()
                appsList.addAll(apps)
                filtered.clear()
                filtered.addAll(appsList)
                appsAdapter.notifyDataSetChanged()
                binding.status.showContent();
            }
        }
    }

    private fun filterApps(query: String) {
        if (query.isEmpty()) {
            appsAdapter.updateData(appsList)
            return
        }
        
        filtered = appsList.filter {
            it.appName.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
        }.toMutableList()
        appsAdapter.updateData(filtered)
    }
}