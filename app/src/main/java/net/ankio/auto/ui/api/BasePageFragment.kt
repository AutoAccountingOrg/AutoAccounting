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
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.components.StatusPage

/**
 * 基础分页Fragment抽象类
 *
 * 提供通用的分页加载功能，包括：
 * - 下拉刷新
 * - 上拉加载更多
 * - 加载状态管理
 * - 错误处理和空状态显示
 *
 * @param T 数据项的类型
 * @param VB ViewBinding的类型
 */
abstract class BasePageFragment<T, VB : ViewBinding> : BaseFragment<VB>() {

    /** 当前页码，从1开始 */
    var page = 1

    /** 每页数据条数 */
    val pageSize = 100

    /** 存储所有已加载的数据 */
    val pageData = mutableListOf<T>()

    /**
     * 加载数据的抽象方法，子类需要实现
     * @return 返回当前页的数据列表
     */
    abstract suspend fun loadData(): List<T>
    
    /**
     * 创建RecyclerView适配器的抽象方法，子类需要实现
     * @return 返回适配器实例
     */
    abstract fun onCreateAdapter(): RecyclerView.Adapter<*>

    /** 是否正在加载数据 */
    private var isLoading = false

    /** 是否还有更多数据可以加载 */
    private var hasMoreData = true

    /** 状态页面组件，用于显示加载、错误、空状态 */
    private var _statusPage: StatusPage? = null

    /** 下拉刷新组件 */
    private var _swipeRefreshLayout: SwipeRefreshLayout? = null

    /** RecyclerView实例 */
    private var recyclerView: RecyclerView? = null

    /** 适配器实例 */
    private var adapter: RecyclerView.Adapter<*>? = null

    /** 状态页面的公共访问器 */
    val statusPage get() = _statusPage!!

    /** 下拉刷新组件的公共访问器 */
    val swipeRefreshLayout get() = _swipeRefreshLayout!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化UI组件
        _statusPage = view.findViewById(R.id.status_page)
        _swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)

        // 显示加载状态
        // statusPage.showLoading()

        // 创建并设置适配器
        adapter = onCreateAdapter()
        recyclerView = statusPage.contentView
        recyclerView?.adapter = adapter

        // 绑定事件监听器
        bindLoadDataEvent()
        bindLoadMoreEvent()

        // 开始加载数据
        loadDataInside()
    }

    override fun onDestroyView() {
        // 清理资源，避免内存泄漏
        recyclerView?.clearOnScrollListeners()
        _statusPage = null
        _swipeRefreshLayout = null
        recyclerView = null
        adapter = null
        super.onDestroyView()
    }

    /**
     * 重置分页状态
     * 清空页码、数据和适配器
     */
    protected fun resetPage() {
        Logger.d("resetPage: Resetting page and data")
        page = 1
        pageData.clear()
        (adapter as? BaseAdapter<*, T>)?.updateItems(emptyList())
    }

    /**
     * 重新加载数据
     * 重置分页状态并重新开始加载
     */
    fun reload() {
        Logger.d("reload: Reloading data from page 1")
        resetPage()
        hasMoreData = true
        // statusPage.showLoading()
        loadDataInside()
    }

    /**
     * 内部数据加载方法
     *
     * @param callback 加载完成后的回调，参数为(是否成功, 是否还有更多数据)
     */
    protected open fun loadDataInside(callback: ((success: Boolean, hasMore: Boolean) -> Unit)? = null) {
        // 检查Fragment是否有效且未在加载中
        if (!isAdded || activity == null || isLoading) {
            Logger.d("loadDataInside: Fragment not added or already loading, abort load")
            return
        }
        
        isLoading = true
        swipeRefreshLayout.isRefreshing = true
        Logger.d("loadDataInside: Start loading data for page $page")
        lifecycleScope.launch {
            try {
                // 在IO线程中加载数据
                val resultData = withContext(Dispatchers.IO) { loadData() }
                isLoading = false

                if (resultData.isEmpty()) {
                    Logger.d("loadDataInside: No data loaded for page $page")
                    // 没有数据的情况
                    if (pageData.isEmpty()) statusPage.showEmpty()
                    hasMoreData = false
                    callback?.invoke(true, false)
                } else {
                    Logger.d("loadDataInside: Loaded ${resultData.size} items for page $page")
                    // 有数据的情况
                    pageData.addAll(resultData)
                    statusPage.showContent()
                    (adapter as? BaseAdapter<*, T>)?.updateItems(pageData.toList())
                    hasMoreData = resultData.size >= pageSize
                    callback?.invoke(true, hasMoreData)
                }
            } catch (e: Exception) {
                Logger.e("loadDataInside: Load data failed", e)
                // 加载失败的处理
                isLoading = false
                statusPage.showError()
                callback?.invoke(false, hasMoreData)
            } finally {
                Logger.d("loadDataInside: Loading finished for page $page")
                // 停止刷新动画
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    /**
     * 绑定下拉刷新事件
     * 只有在列表顶部时才能触发刷新
     */
    private fun bindLoadDataEvent() {
        // 设置下拉刷新的触发距离
        swipeRefreshLayout.setDistanceToTriggerSync(200)
        swipeRefreshLayout.setOnRefreshListener {
            val recyclerView = statusPage.contentView ?: return@setOnRefreshListener
            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager

            // 检查是否在列表顶部
            val isAtTop = layoutManager != null &&
                    layoutManager.findFirstCompletelyVisibleItemPosition() == 0 &&
                    recyclerView.getChildAt(0)?.top == 0

            // 如果不在顶部或正在加载，则取消刷新
            if (!isAtTop || isLoading) {
                Logger.d("bindLoadDataEvent: Not at top or already loading, ignore refresh")

                swipeRefreshLayout.isRefreshing = false
                return@setOnRefreshListener
            }
            Logger.d("bindLoadDataEvent: Pull to refresh triggered")
            // 执行刷新
            reload()
        }
    }

    /**
     * 绑定上拉加载更多事件
     * 当滚动到底部附近时自动加载下一页
     */
    private fun bindLoadMoreEvent() {
        val recyclerView = statusPage.contentView ?: return
        recyclerView.clearOnScrollListeners()
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                // 检查是否可以加载更多：有更多数据、未在加载、向下滚动
                if (!hasMoreData || isLoading || dy <= 0) return

                val layoutManager = rv.layoutManager as? LinearLayoutManager ?: return
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                // 当最后一个可见项距离底部5个位置时开始加载下一页
                if (lastVisibleItem >= totalItemCount - 5) {
                    Logger.d("bindLoadMoreEvent: Load more triggered at item $lastVisibleItem of $totalItemCount")

                    page++
                    loadDataInside { success, hasMore ->
                        // 如果加载失败，回退页码
                        if (!success) {
                            Logger.d("bindLoadMoreEvent: Load more failed, revert page index")
                            page--
                        }
                        hasMoreData = hasMore
                    }
                }
            }
        })
    }
}
