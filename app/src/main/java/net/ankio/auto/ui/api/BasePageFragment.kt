package net.ankio.auto.ui.api

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.ui.components.StatusPage

/**
 * 基础分页Fragment类
 * @param T 数据类型
 * @param VB ViewBinding类型
 */
abstract class BasePageFragment<T, VB : ViewBinding> : BaseFragment<VB>() {

    /** 当前页码 */
    var page = 1

    /** 每页数据条数 */
    val pageSize = 100

    /** 存储所有加载的数据 */
    val pageData = mutableListOf<T>()

    /**
     * 加载数据的抽象方法
     * @param callback 数据加载完成的回调函数
     */
    abstract suspend fun loadData(callback: (resultData: List<T>) -> Unit)

    /**
     * 创建适配器的抽象方法
     */
    abstract fun onCreateAdapter()

    /** 是否正在加载数据 */
    private var isLoading = false

    /** 是否还有更多数据 */
    private var hasMoreData = true

    /** 状态页面组件 */
    private var _statusPage: StatusPage? = null

    /** 下拉刷新组件 */
    private var _swipeRefreshLayout: SwipeRefreshLayout? = null

    /** 状态页面组件的非空访问器 */
    val statusPage get() = _statusPage!!

    /** 下拉刷新组件的非空访问器 */
    val swipeRefreshLayout get() = _swipeRefreshLayout!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _statusPage = view.findViewById(R.id.status_page)
        _swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)

        statusPage.showLoading()
        onCreateAdapter()
        bindLoadDataEvent()
        bindLoadMoreEvent()

    }

    override fun onDestroy() {
        super.onDestroy()
        _statusPage = null
        _swipeRefreshLayout = null
    }

    /**
     * 重置页码和数据
     */
    protected fun resetPage() {
        page = 1
        pageData.clear()
        lifecycleScope.launch {
            val adapter = statusPage.contentView?.adapter as? BaseAdapter<*, T>
            adapter?.updateItems(emptyList())
        }
    }

    /**
     * 重新加载数据
     */
    fun reload() {
        resetPage()
        hasMoreData = true
        statusPage.showLoading()
        loadDataInside()
    }

    /**
     * 内部加载数据的方法
     * @param callback 加载完成的回调函数，返回是否成功和是否还有更多数据
     */
    protected open fun loadDataInside(callback: ((success: Boolean, hasMore: Boolean) -> Unit)? = null) {
        if (!isAdded || activity == null || isLoading) return
        isLoading = true

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                loadData { resultData ->
                    isLoading = false

                    if (resultData.isEmpty()) {
                        if (pageData.isEmpty()) {
                            lifecycleScope.launch { statusPage.showEmpty() }
                        }
                        hasMoreData = false
                        callback?.invoke(true, false)
                        return@loadData
                    }

                    pageData.addAll(resultData)
                    lifecycleScope.launch {
                        statusPage.showContent()
                        val adapter = statusPage.contentView?.adapter as? BaseAdapter<*, T>
                        adapter?.updateItems(pageData.toList())
                    }

                    hasMoreData = resultData.size >= pageSize
                    callback?.invoke(true, hasMoreData)
                }
            }
        }
    }

    /**
     * 绑定下拉刷新事件（要求 RecyclerView 滚动到顶部才生效）
     */
    private fun bindLoadDataEvent() {
        swipeRefreshLayout.setDistanceToTriggerSync(200) // 防止误触
        swipeRefreshLayout.setOnRefreshListener {
            val recyclerView = statusPage.contentView ?: return@setOnRefreshListener
            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager

            // 判断 RecyclerView 是否滚动到顶部
            val isAtTop = layoutManager != null &&
                    layoutManager.findFirstCompletelyVisibleItemPosition() == 0 &&
                    recyclerView.getChildAt(0)?.top == 0

            if (!isAtTop || isLoading) {
                swipeRefreshLayout.isRefreshing = false
                return@setOnRefreshListener
            }

            page = 1
            hasMoreData = true
            loadDataInside { _, _ ->
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    /**
     * 绑定加载更多事件（滚动到底部自动加载）
     */
    private fun bindLoadMoreEvent() {
        statusPage.contentView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!hasMoreData || isLoading || dy <= 0) return

                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (lastVisibleItem >= totalItemCount - 5) {
                    page++
                    loadDataInside { success, hasMore ->
                        if (!success) page--
                        hasMoreData = hasMore
                    }
                }
            }
        })
    }
}