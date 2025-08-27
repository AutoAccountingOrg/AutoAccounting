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

package net.ankio.auto.ui.fragment.components

import net.ankio.auto.databinding.ComponentBookBinding
import net.ankio.auto.ui.adapter.BookSelectorAdapter
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.ui.components.WrapContentLinearLayoutManager
import net.ankio.auto.http.api.BookNameAPI
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.db.model.BookNameModel

class BookComponent(binding: ComponentBookBinding) :
    BaseComponent<ComponentBookBinding>(binding) {

    private var onBookSelected: ((BookNameModel, String) -> Unit)? = null
    private val dataItems = mutableListOf<BookNameModel>()
    private lateinit var adapter: BookSelectorAdapter
    private var showSelect: Boolean = false
    private var showEdit: Boolean = false
    /**
     * 设置账本选择回调
     * @param callback 回调函数，参数为选中的BookNameModel和BillType
     */
    fun setOnBookSelectedListener(callback: (BookNameModel, String) -> Unit) {
        this.onBookSelected = callback
    }


    fun setShowOption(select: Boolean, edit: Boolean) {
        this.showSelect = select
        this.showEdit = edit
        if (::adapter.isInitialized) {
            // 重新创建适配器以应用新的showSelect设置
            setupRecyclerView()
        }
    }

    override fun onComponentCreate() {
        super.onComponentCreate()

        setupRecyclerView()
        loadData()
        binding.statusPage.swipeRefreshLayout?.setOnRefreshListener {
            refreshData()
        }
    }

    private fun setupRecyclerView() {
        val recyclerView = binding.statusPage.contentView!!
        // 外部已注入 SwipeRefreshLayout，这里仅使用刷新动画（存在即使用）
        recyclerView.layoutManager = WrapContentLinearLayoutManager(context)

        // 创建适配器，支持showSelect参数
        adapter = BookSelectorAdapter(showSelect = showSelect, showEdit = showEdit) { item, type ->
            onBookSelected?.invoke(item, type)
        }

        recyclerView.adapter = adapter
    }

    private fun loadData() {
        binding.statusPage.showLoading()

        // 使用组件的生命周期作用域
        launch {
            try {
                dataItems.clear()
                var newData = BookNameAPI.list()

                // 如果数据为空，创建默认账本
                if (newData.isEmpty()) {
                    newData = mutableListOf(BookNameModel().apply {
                        name = "默认账本"
                        id = 1
                        icon = ""
                    })
                }

                // 对账本列表进行排序：默认账本排在第一位，其他账本按原顺序排列
                val defaultBookName = PrefManager.defaultBook
                val sortedData = newData.sortedWith(compareByDescending<BookNameModel> { book ->
                    // 如果是默认账本，返回1（排在前面），否则返回0
                    if (book.name == defaultBookName) 1 else 0
                }.thenBy { book ->
                    // 非默认账本保持原有的ID倒序排序
                    -book.id
                })

                dataItems.addAll(sortedData)
                adapter.updateItems(dataItems)
                binding.statusPage.showContent()
            } catch (e: Exception) {
                binding.statusPage.showError()
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refreshData() {
        loadData()
    }

    /**
     * 获取当前数据列表
     */
    fun getDataItems(): List<BookNameModel> = dataItems.toList()

    /**
     * 获取当前showSelect状态
     */
    fun isShowSelect(): Boolean = showSelect
}