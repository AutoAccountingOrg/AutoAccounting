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

import android.health.connect.datatypes.units.Length
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.api.RefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.ui.componets.StatusPage

/**
 * 基础的BasePageFragment
 */
abstract class BasePageFragment<T>: BaseFragment() {
    /**
     * 初始化View
     */
    lateinit var statusPage: StatusPage

    /**
     * 当前页码
     */
     var page = 1

    /**
     * 每页数据大小
      */
    val pageSize = 10

    /**
     * 数据列表
     */
     val pageData = mutableListOf<T>()

    /**
     * 需要实现加载数据的逻辑
     */
    abstract suspend fun loadData(callback: (resultData: List<T>) -> Unit)

    /**
     * 重置页面
     */
    protected fun resetPage(){
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
    protected fun loadDataInside(callback: ((Boolean, Boolean) -> Unit)?=null){
        if (page == 1) {
          resetPage()
        }
        lifecycleScope.launch {
            withContext(Dispatchers.IO){
                loadData { resultData ->
                    if (resultData.isEmpty()) {
                        callback?.invoke(true, false)
                        return@loadData
                    }
                    pageData.addAll(resultData)
                    statusPage.contentView?.adapter?.notifyItemInserted(pageData.size - pageSize)

                    val total = page * pageSize

                    if (callback != null) callback(true, total > pageData.size)
                }
            }
        }
    }

    private fun  changeState(success:Boolean,length: Int){
        if (!success){
            statusPage.showError()
            return
        }
        if (length == 0){
            statusPage.showEmpty()
            return
        }
        statusPage.showContent()
    }

    /**
     * 加载数据事件
     */
    protected fun loadDataEvent(refreshLayout: RefreshLayout) {
        refreshLayout.setRefreshHeader(ClassicsHeader(requireContext()))
        refreshLayout.setRefreshFooter(ClassicsFooter(requireContext()))
        refreshLayout.setOnRefreshListener {
            page = 1
            loadDataInside { success, hasMore ->
                changeState(success,pageData.size)
                it.resetNoMoreData()
                it.finishRefresh(0, success, hasMore) //传入false表示刷新失败
            }
        }
        refreshLayout.setOnLoadMoreListener {
            page++
            loadDataInside { success, hasMore ->
                changeState(success,pageData.size)
                it.finishLoadMore(0, success, hasMore) //传入false表示加载失败
            }
        }
    }

    /**
     * 加载数据
     */
    override fun onResume() {
        super.onResume()
        statusPage.showLoading()
        loadDataInside()
    }

}