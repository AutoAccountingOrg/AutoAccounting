/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentDataBinding
import net.ankio.auto.databinding.FragmentDataRuleBinding
import net.ankio.auto.databinding.FragmentLogBinding
import net.ankio.auto.ui.adapter.AppDataAdapter
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.componets.CustomNavigationRail
import net.ankio.auto.ui.componets.MaterialSearchView
import net.ankio.auto.ui.models.RailMenuItem
import net.ankio.auto.ui.models.ToolbarMenuItem
import net.ankio.auto.ui.utils.ViewFactory.createBinding
import net.ankio.auto.ui.utils.viewBinding
import org.ezbook.server.constant.DataType
import org.ezbook.server.db.model.AppDataModel
import java.lang.ref.WeakReference

class DataFragment : BasePageFragment<AppDataModel>(), Toolbar.OnMenuItemClickListener {
    var app: String = ""
    var type: String = ""
    var match = false
    var searchData = ""
    override suspend fun loadData(callback: (resultData: List<AppDataModel>) -> Unit) {
        AppDataModel.list(app, type,match, page, pageSize, searchData).let { result ->
            callback(result)
        }
    }

    override fun onCreateAdapter() {
        val recyclerView = binding.statusPage.contentView!!
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = AppDataAdapter(pageData, requireActivity() as BaseActivity)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = binding.root

    override val binding: FragmentDataBinding by viewBinding(FragmentDataBinding::inflate)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadLeftData(binding.leftList)
        setupChipEvent()
        val leftList = binding.leftList
        lifecycleScope.launch {
            AppDataModel.apps().let { result ->
                leftData = result
                var i = 0
                for (key in result.keySet()) {
                    i++
                    var app = App.getAppInfoFromPackageName(key)

                    if (app == null){
                        if (App.debug){
                            app = arrayOf(key, ResourcesCompat.getDrawable(App.app.resources,R.drawable.default_asset,null),"")
                        }else{
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

        binding.toolbar.setOnMenuItemClickListener(this)
        setUpSearch()
    }
    private var leftData = JsonObject()
    private fun loadLeftData(leftList: CustomNavigationRail) {

        leftList.setOnItemSelectedListener {
            val id = it.id
            page = 1
            app = leftData.keySet().elementAt(id - 1)
            statusPage.showLoading()
            loadDataInside()
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

    private fun setUpSearch(){
        val searchItem = binding.toolbar.menu.findItem(R.id.action_search)
        if(searchItem != null){
            val searchView = searchItem.actionView as MaterialSearchView
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
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
        when(item?.itemId){
            R.id.item_clear -> {
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(requireActivity().getString(R.string.delete_data))
                    .setMessage(requireActivity().getString(R.string.delete_msg))
                    .setPositiveButton(requireActivity().getString(R.string.sure_msg)) { _, _ ->
                        lifecycleScope.launch {
                            AppDataModel.clear()
                            page = 1
                            loadDataInside()
                        }
                    }
                    .setNegativeButton(requireActivity().getString(R.string.cancel_msg)) { _, _ -> }
                    .show()
                return true
            }
            else -> {
               return false
            }
        }
    }
}
