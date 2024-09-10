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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentLogBinding
import net.ankio.auto.databinding.FragmentOrderBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.BillInfoAdapter
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.models.ToolbarMenuItem
import net.ankio.auto.utils.DateUtils
import org.ezbook.server.db.model.BillInfoModel

open class OrderFragment : BasePageFragment<Pair<String, List<BillInfoModel>>>() {
    override suspend fun loadData(callback: (resultData: List<Pair<String, List<BillInfoModel>>>) -> Unit) {
       val list =  BillInfoModel.list(page, pageSize)
        Logger.i("list size: ${list.size}")
        list.forEach {
            val item = it
            val day = DateUtils.stampToDate(it.time,"yyyy-MM-dd")
            val dayItem = pageData.find{ pair -> pair.first == day}
            if (dayItem == null){
                pageData.add(Pair(day, listOf(item)))
                withContext(Dispatchers.Main){
                    statusPage.contentView?.adapter?.notifyItemInserted(pageData.size - 1)
                }
            } else {
                val index = pageData.indexOf(dayItem)
                val items = dayItem.second.toMutableList()
                items.add(item)
                pageData.remove(dayItem)
                withContext(Dispatchers.Main){
                    statusPage.contentView?.adapter?.notifyItemRemoved(index)
                }
                pageData.add(Pair(day, items))
                withContext(Dispatchers.Main){
                    statusPage.contentView?.adapter?.notifyItemInserted(pageData.size - 1)
                }
            }
        }

       withContext(Dispatchers.Main){
           callback.invoke(if (list.isEmpty()) emptyList() else pageData)
       }


    }

    override fun loadDataInside(callback: ((Boolean, Boolean) -> Unit)?){
        if (page == 1) {
            resetPage()
        }
        lifecycleScope.launch {
            withContext(Dispatchers.IO){
                loadData { resultData ->
                    if (pageData.isEmpty()) {
                        statusPage.showEmpty()
                        callback?.invoke(true, false)
                        return@loadData
                    }
                    statusPage.showContent()


                    if (callback != null) callback(true, resultData.isNotEmpty())
                }
            }
        }
    }

    override val menuList: ArrayList<ToolbarMenuItem>
        get() =
            arrayListOf(
                ToolbarMenuItem(R.string.item_sync, R.drawable.float_round) {
                    // 同步账单
                    App.startBookApp()
                },
            )
    private lateinit var binding: FragmentLogBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentLogBinding.inflate(layoutInflater)
        statusPage = binding.statusPage
        val recyclerView = binding.statusPage.contentView!!
        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = BillInfoAdapter(pageData)
        scrollView = recyclerView

        loadDataEvent(binding.refreshLayout)

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        statusPage.showLoading()
        loadDataInside()
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
