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
import net.ankio.auto.ui.adapter.OrderAdapter
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.models.ToolbarMenuItem
import net.ankio.auto.utils.DateUtils
import org.ezbook.server.constant.BillState
import org.ezbook.server.constant.DataType
import org.ezbook.server.db.model.BillInfoModel
import java.lang.ref.WeakReference

open class OrderFragment : BasePageFragment<Pair<String, List<BillInfoModel>>>() {
    override suspend fun loadData(callback: (resultData: List<Pair<String, List<BillInfoModel>>>) -> Unit) {
        val list = BillInfoModel.list(page, pageSize)

        val newIndex = mutableListOf<Int>()
        val updateIndex = mutableListOf<Int>()


        list.forEach { item ->
            val day = DateUtils.stampToDate(item.time, "yyyy-MM-dd")
            val dayItem = pageData.find { pair -> pair.first == day }

            if (dayItem == null) {
                // 新的一天，插入一个新的 Pair
                val pair = Pair(day, listOf(item))
                pageData.add(pair)
                newIndex.add(pageData.indexOf(pair))
            } else {
                // 该天已存在，更新现有条目
                val index = pageData.indexOf(dayItem)
                val items = dayItem.second.toMutableList()
                items.add(item)
                pageData[index] = Pair(day, items) // 直接更新 pageData 而不是 remove 和 add
                updateIndex.add(index)
            }
        }

        withContext(Dispatchers.Main) {
            // 统一更新 RecyclerView 的数据
            newIndex.forEach { index ->
                statusPage.contentView?.adapter?.notifyItemInserted(index)
            }
            updateIndex.forEach { index ->
                statusPage.contentView?.adapter?.notifyItemChanged(index)
            }

            callback.invoke(if (list.isEmpty()) emptyList() else pageData)
        }
    }


    override fun loadDataInside(callback: ((Boolean, Boolean) -> Unit)?) {
        if (page == 1) {
            resetPage()
        }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
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
    private lateinit var binding: FragmentOrderBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentOrderBinding.inflate(layoutInflater)
        statusPage = binding.statusPage
        val recyclerView = binding.statusPage.contentView!!
        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = OrderAdapter(pageData)
        // scrollView = WeakReference(recyclerView)

        loadDataEvent(binding.refreshLayout)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        statusPage.showLoading()
        loadDataInside()
    }


}
