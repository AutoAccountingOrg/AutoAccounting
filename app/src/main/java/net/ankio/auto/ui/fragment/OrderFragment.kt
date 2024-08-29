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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentOrderBinding
//import net.ankio.auto.ui.adapter.OrderAdapter
import net.ankio.auto.utils.AppUtils
import org.ezbook.server.db.model.BillInfoModel
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.utils.ToolbarMenuItem

class OrderFragment : BaseFragment() {
    override val menuList: ArrayList<ToolbarMenuItem>
        get() =
            arrayListOf(
                ToolbarMenuItem(R.string.item_sync, R.drawable.float_round) {
                    // 同步账单
                    AppUtils.startBookApp()
                },
            )
    private lateinit var binding: FragmentOrderBinding
    private lateinit var recyclerView: RecyclerView
    //private lateinit var adapter: OrderAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private val dataItems = ArrayList<Pair<String, List<BillInfoModel>>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentOrderBinding.inflate(layoutInflater)
        recyclerView = binding.recyclerView
        layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager

       /* adapter = OrderAdapter(dataItems)

        recyclerView.adapter = adapter*/
        scrollView = recyclerView
        return binding.root
    }

  /*  private fun loadMoreData() {
        val loading = LoadingUtils(requireActivity())
        loading.show(R.string.loading)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val autoAccountingConfig = AppUtils.getService().config()
                val list = BillInfo.getBillListGroup(500)
                dataItems.clear()

                list.forEach {
                    dataItems.add(Pair(it.first, BillInfo.getBillByIds(it.second)))
                }
                adapter.notifyConfig(autoAccountingConfig)
            }

            adapter.notifyDataSetChanged()
            binding.empty.root.visibility = if (dataItems.isEmpty()) View.VISIBLE else View.GONE
            loading.close()
        }
    }

    override fun onResume() {
        super.onResume()
        // 加载数据
        loadMoreData()
    }*/
}
