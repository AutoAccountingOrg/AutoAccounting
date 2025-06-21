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

package net.ankio.auto.ui.fragment.plugin

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentPluginDataBinding
import net.ankio.auto.http.api.AppDataAPI
import net.ankio.auto.ui.adapter.AppDataAdapter
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.components.MaterialSearchView
import net.ankio.auto.ui.components.WrapContentLinearLayoutManager
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.models.RailMenuItem
import net.ankio.auto.utils.getAppInfoFromPackageName
import org.ezbook.server.constant.DataType
import org.ezbook.server.db.model.AppDataModel

class DataFragment : BasePageFragment<AppDataModel, FragmentPluginDataBinding>(),
    Toolbar.OnMenuItemClickListener {
    var app: String = ""
    var type: String = ""
    var match = false
    var searchData = ""
    override suspend fun loadData(): List<AppDataModel> =
        AppDataAPI.list(app, type, match, page, pageSize, searchData)

    override fun onCreateAdapter(): AppDataAdapter {
        val recyclerView = binding.statusPage.contentView!!
        recyclerView.layoutManager = WrapContentLinearLayoutManager(requireContext())
        return AppDataAdapter(requireActivity() as BaseActivity)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpLeftData()
        setupChipEvent()
        binding.toolbar.setOnMenuItemClickListener(this)
        setUpSearch()
    }

    private var leftData = JsonObject()
    private fun setUpLeftData() {

        binding.leftList.setOnItemSelectedListener {
            val id = it.id
            page = 1
            app = leftData.keySet().elementAt(id - 1)
            //  statusPage.showLoading()
            reload()
        }
        refreshLeftData()
    }

    override fun onResume() {
        super.onResume()

    }

    private fun refreshLeftData() {
        lifecycleScope.launch {
            // 1. 清空列表
            binding.leftList.clear()

            // 2. 拉取 app 数据
            val result = AppDataAPI.apps()
            leftData = result

            var index = 1
            // 3. 遍历所有 app 包名
            for (packageName in result.keySet()) {
                val app = getAppInfoFromPackageName(packageName) ?: continue

                binding.leftList.addMenuItem(
                    RailMenuItem(index, app.icon!!, app.name)
                )
                index++
            }

            // 4. 若没有任何 app，展示空状态页
            if (!binding.leftList.performFirstItem()) {
                statusPage.showEmpty()
            }
        }
    }


    private fun setupChipEvent() {
        binding.chipGroup.setOnCheckedStateChangeListener { group, checkedId ->

            match = false
            type = ""

            if (R.id.chip_match in checkedId) {
                match = true
            }

            if (R.id.chip_notify in checkedId) {
                type = DataType.NOTICE.name
            }

            if (R.id.chip_data in checkedId) {
                type = DataType.DATA.name
            }

            if (R.id.chip_notify in checkedId && R.id.chip_data in checkedId) {
                type = ""
            }


            loadDataInside()
        }
    }

    private fun setUpSearch() {
        val searchItem = binding.toolbar.menu.findItem(R.id.action_search)
        if (searchItem != null) {
            val searchView = searchItem.actionView as MaterialSearchView
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    searchData = newText ?: ""
                    reload()
                    return true
                }

            })
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.item_clear -> {
                BottomSheetDialogBuilder(requireActivity())
                    .setTitle(requireActivity().getString(R.string.delete_data))
                    .setMessage(requireActivity().getString(R.string.delete_msg))
                    .setPositiveButton(requireActivity().getString(R.string.sure_msg)) { _, _ ->
                        lifecycleScope.launch {
                            AppDataAPI.clear()
                            page = 1
                            loadDataInside()
                        }
                    }
                    .setNegativeButton(requireActivity().getString(R.string.cancel_msg)) { _, _ -> }
                    .showInFragment(this, false, true)
                return true
            }

            else -> {
                return false
            }
        }
    }

}