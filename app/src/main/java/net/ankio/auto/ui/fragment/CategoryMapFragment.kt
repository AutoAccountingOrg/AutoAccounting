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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentCategoryBinding
import net.ankio.auto.ui.adapter.CategoryMapAdapter
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.componets.MaterialSearchView
import net.ankio.auto.ui.utils.viewBinding
import org.ezbook.server.db.model.CategoryMapModel

class CategoryMapFragment : BasePageFragment<CategoryMapModel>() {
    override val binding: FragmentCategoryBinding by viewBinding(FragmentCategoryBinding::inflate)
    var searchData: String = ""
    override suspend fun loadData(callback: (resultData: List<CategoryMapModel>) -> Unit) {
        lifecycleScope.launch {
            val newData = CategoryMapModel.list(page, pageSize, searchData)
            callback(newData)
        }
    }

    override fun onCreateAdapter() {
        val recyclerView = statusPage.contentView!!
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = CategoryMapAdapter(pageData, requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = binding.root


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchItem = binding.topAppBar.menu.findItem(R.id.action_search)

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
}