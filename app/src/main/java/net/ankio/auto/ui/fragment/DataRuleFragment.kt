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

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentDataRuleBinding
import net.ankio.auto.ui.adapter.DataRuleAdapter
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.componets.CustomNavigationRail
import net.ankio.auto.ui.componets.MaterialSearchView
import net.ankio.auto.ui.models.RailMenuItem
import net.ankio.auto.ui.utils.viewBinding
import org.ezbook.server.constant.DataType
import org.ezbook.server.db.model.RuleModel

/**
 * 数据规则Fragment
 */
class DataRuleFragment : BasePageFragment<RuleModel>(), Toolbar.OnMenuItemClickListener {
    private var searchData = ""
    private var app = ""
    private var type = ""
    override suspend fun loadData(callback: (resultData: List<RuleModel>) -> Unit) {
        RuleModel.list(app, type, page, pageSize, searchData).let { result ->
            callback(result)
        }
    }

    override fun onCreateAdapter() {
        val recyclerView = binding.statusPage.contentView!!
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = DataRuleAdapter(pageData)
    }

    override val binding: FragmentDataRuleBinding by viewBinding(FragmentDataRuleBinding::inflate)

    private var leftData = JsonObject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val leftList = binding.leftList
        lifecycleScope.launch {
            RuleModel.apps().let { result ->
                leftData = result
                var i = 0
                for (key in result.keySet()) {
                    i++
                    var app = App.getAppInfoFromPackageName(key)

                    if (app == null) {
                        //  arrayOf(appName, appIcon, appVersion)
                        if (App.debug) {
                            app = arrayOf(
                                key,
                                ResourcesCompat.getDrawable(
                                    App.app.resources,
                                    R.drawable.default_asset,
                                    null
                                ),
                                ""
                            )
                        } else {
                            continue
                        }

                    }

                    leftList.addMenuItem(
                        RailMenuItem(i, app[1] as Drawable, app[0] as String)
                    )

                }
                if (!leftList.triggerFirstItem()) {
                    statusPage.showEmpty()
                }
            }
        }
        loadLeftData(binding.leftList)
        chipEvent()
        binding.toolbar.setOnMenuItemClickListener(this)
        setUpSearch()
    }

    private fun loadLeftData(leftList: CustomNavigationRail) {
        leftList.setOnItemSelectedListener {
            val id = it.id
            page = 1
            app = leftData.keySet().elementAt(id - 1)
            statusPage.showLoading()
            loadDataInside()
        }
    }


    private fun chipEvent() {
        binding.chipGroup.isSingleSelection = true
        binding.chipAll.visibility = View.VISIBLE
        binding.chipAll.isChecked = true
        binding.chipNotify.isChecked = false
        binding.chipData.isChecked = false
        binding.chipMatch.visibility = View.GONE
        binding.chipGroup.setOnCheckedStateChangeListener { group, checkedId ->
            val chipId = checkedId.firstOrNull() ?: R.id.chip_all

            when (chipId) {
                R.id.chip_all -> {
                    type = ""
                }

                R.id.chip_notify -> {
                    type = DataType.NOTICE.name
                }

                R.id.chip_data -> {
                    type = DataType.DATA.name
                }
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

            R.id.item_notice -> {
                navigate(R.id.action_dataRuleFragment_to_noticeFragment)
            }

            R.id.item_sms -> {
                navigate(R.id.action_dataRuleFragment_to_smsFragment)
            }
        }
        return true
    }
}