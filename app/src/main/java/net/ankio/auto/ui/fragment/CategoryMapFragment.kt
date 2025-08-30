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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentCategoryMapBinding
import net.ankio.auto.http.api.CategoryMapAPI
import net.ankio.auto.ui.adapter.CategoryMapAdapter
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.components.MaterialSearchView
import net.ankio.auto.ui.components.WrapContentLinearLayoutManager
import org.ezbook.server.db.model.CategoryMapModel

/**
 * 分类映射Fragment
 *
 * 该Fragment负责显示和管理分类映射列表，提供以下功能：
 * - 显示所有分类映射列表
 * - 搜索分类映射
 * - 编辑分类映射
 * - 分页加载更多数据
 *
 * 继承自BasePageFragment提供分页功能
 */
class CategoryMapFragment : BasePageFragment<CategoryMapModel, FragmentCategoryMapBinding>() {

    /** 搜索关键词 */
    private var searchData: String = ""

    /**
     * 加载分类映射数据
     * @return 当前页的分类映射列表
     */
    override suspend fun loadData(): List<CategoryMapModel> {
        return CategoryMapAPI.list(page, pageSize, searchData)
    }

    /**
     * 创建RecyclerView适配器
     * @return CategoryMapAdapter实例
     */
    override fun onCreateAdapter(): RecyclerView.Adapter<*> {
        // 设置RecyclerView布局管理器
        statusPage.contentView?.layoutManager = WrapContentLinearLayoutManager(context)
        return CategoryMapAdapter(requireActivity())
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.topAppBar.setNavigationOnClickListener { findNavController().popBackStack() }
        // 设置工具栏搜索功能
        setupSearchView()
    }

    /**
     * 设置搜索视图
     */
    private fun setupSearchView() {
        val searchItem = binding.topAppBar.menu.findItem(R.id.action_search)

        if (searchItem != null) {
            val searchView = searchItem.actionView as MaterialSearchView
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    searchData = newText ?: ""
                    reload() // 重新加载数据
                    return true
                }
            })
        }
    }
}
