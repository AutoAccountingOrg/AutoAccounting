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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentDataBinding
import net.ankio.auto.ui.activity.BaseActivity
import net.ankio.auto.ui.adapter.AppDataAdapter
import net.ankio.auto.ui.dialog.FilterDialog
import net.ankio.auto.ui.utils.MenuItem
import net.ankio.auto.ui.viewModes.AppDataViewModel

class DataFragment : BaseFragment() {
    private lateinit var binding: FragmentDataBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppDataAdapter
    private val viewModel: AppDataViewModel by viewModels()
    private lateinit var layoutManager: LinearLayoutManager
    override val menuList: ArrayList<MenuItem> =
        arrayListOf(
            MenuItem(R.string.item_filter, R.drawable.menu_icon_filter) {
                FilterDialog(requireActivity() as BaseActivity) {
                    //TODO LOADDATA
                }.show(false)
            },
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentDataBinding.inflate(layoutInflater)
        recyclerView = binding.recyclerView
        layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        scrollView = recyclerView
        adapter =
            AppDataAdapter(
                requireActivity(),
                viewModel,
            )

        recyclerView.adapter = adapter

        viewModel.loadMoreData()

        return binding.root
    }

   /* private fun loadMoreData() {
        val loading = LoadingUtils(requireActivity())
        loading.show(R.string.loading)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val dataList = viewModel.fetchData(1, 100, hashMapOf())

                val resultList = mutableListOf<AppDataModel>()
                resultList.addAll(dataList)

                // 在这里处理搜索逻辑
                val resultSearch =
                    resultList.filter {
                        var include = true

                        val dataType = SpUtils.getInt("dialog_filter_data_type", 0)

                        if (dataType != 0) {
                            if (it.type != dataType) {
                                include = false
                            }
                        }

                        val match = SpUtils.getInt("dialog_filter_match", 0)
                        if (match != 0) {
                            if (it.match ==1 && match == 2) {
                                include = false
                            }

                            if (it.match ==0 && match == 1) {
                                include = false
                            }
                        }

                        val upload = SpUtils.getInt("dialog_filter_upload", 0)

                        if (upload != 0) {
                            if (it.issue != 0 && upload == 2) {
                                include = false
                            }

                            if (it.issue == 0 && upload == 1) {
                                include = false
                            }
                        }

                        val keywords = SpUtils.getString("dialog_filter_data", "")

                        if (keywords != "") {
                            if (
                                !it.data.contains(keywords)
                            ) {
                                include = false
                            }
                        }

                        include
                    }

                // 倒序排列resultSearch
                dataItems.clear()
                dataItems.addAll(resultSearch)
                withContext(Dispatchers.Main) {
                    adapter.notifyDataSetChanged()
                    binding.empty.root.visibility = if (dataItems.isEmpty()) View.VISIBLE else View.GONE
                    loading.close()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 加载数据
        loadMoreData()
    }*/
}
