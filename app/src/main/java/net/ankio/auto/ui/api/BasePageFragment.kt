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

package net.ankio.auto.ui.api

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.ui.componets.StatusPage

/**
 * 基础的BasePageFragment
 */
abstract class BasePageFragment<T> : BaseFragment() {
    /**
     * 初始化View
     */
    lateinit var statusPage: StatusPage
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    /**
     * 当前页码
     */
    var page = 1

    /**
     * 每页数据大小
     */
    val pageSize = 20

    /**
     * 数据列表
     */
    val pageData = mutableListOf<T>()

    /**
     * 需要实现加载数据的逻辑
     */
    abstract suspend fun loadData(callback: (resultData: List<T>) -> Unit)

    abstract fun onCreateAdapter()
    /**
     * 重置页面
     */
    protected fun resetPage() {
        page = 1
        val total = pageData.size
        pageData.clear()
        lifecycleScope.launch {
            statusPage.contentView?.adapter?.notifyItemRangeRemoved(0, total)
        }
    }

    /**
     * 获取数据
     */
    protected open fun loadDataInside(callback: ((Boolean, Boolean) -> Unit)? = null) {
        // 没有附加到Activity上，不加载数据
        if (activity == null || !isAdded) return
        if (page == 1) {
            resetPage()
        }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                loadData { resultData ->
                    if (resultData.isEmpty()) {
                        if (pageData.isEmpty()) {
                            statusPage.showEmpty()
                        }
                        callback?.invoke(true, false)
                        return@loadData
                    }

                    pageData.addAll(resultData)
                    statusPage.showContent()
                    statusPage.contentView?.adapter?.notifyItemInserted(pageData.size - pageSize)

                    val total = page * pageSize

                    if (callback != null && isAdded) callback(true, total > pageData.size)
                }
            }
        }
    }


    private fun bindLoadDataEvent() {
        swipeRefreshLayout.setOnRefreshListener {
            page = 1
            loadDataInside { success, hasMore ->
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

   private fun loadMoreDataEvent(){
        statusPage.contentView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            var loading = false // 防抖标志

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                // 计算是否达到80%的位置
                if (!loading && lastVisibleItemPosition >= totalItemCount * 0.8) {
                    loading = true // 设置防抖标志
                    page++
                    loadDataInside { success, hasMore ->
                        if (!success) {
                            page--
                        }
                        loading = false // 重置防抖标志
                    }
                }
            }
        })
   }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        statusPage = view.findViewById(R.id.status_page)
        statusPage.showLoading()
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)
        onCreateAdapter()
        bindLoadDataEvent()
        loadMoreDataEvent()
        reload()
    }



    fun reload(){
        resetPage()
        statusPage.showLoading()
        loadDataInside()
    }

}