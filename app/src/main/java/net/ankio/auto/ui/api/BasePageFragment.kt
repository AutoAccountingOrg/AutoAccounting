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

        // 初始化核心UI组件
        initializeComponents(view)

        // 设置适配器和数据绑定
        setupAdapter()

        // 配置事件监听器
        setupEventListeners()

        // 启动首次数据加载
        resetPage()
        loadDataInside()
    }

    /**
     * 初始化核心UI组件
     * 获取StatusPage和SwipeRefreshLayout的引用并配置关联关系
     */
    private fun initializeComponents(view: View) {
        _statusPage = view.findViewById(R.id.status_page)
        _swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)

        // 关联下拉刷新组件到状态页面
        statusPage.swipeRefreshLayout = _swipeRefreshLayout
        statusPage.showLoading()
    }

    /**
     * 设置RecyclerView适配器
     * 创建适配器实例并绑定到RecyclerView
     */
    private fun setupAdapter() {
        adapter = onCreateAdapter()
        recyclerView = statusPage.contentView
        recyclerView?.adapter = adapter
    }

    /**
     * 配置所有事件监听器
     * 包括下拉刷新和上拉加载更多
     */
    private fun setupEventListeners() {
        bindLoadDataEvent()
        bindLoadMoreEvent()
    }

    override fun onDestroyView() {
        // 彻底清理资源，防止内存泄漏
        cleanupResources()
        super.onDestroyView()
    }

    /**
     * 清理所有资源引用
     * 移除监听器并置空引用，确保Fragment销毁时无内存泄漏
     */
    private fun cleanupResources() {
        recyclerView?.clearOnScrollListeners()
        _statusPage = null
        _swipeRefreshLayout = null
        recyclerView = null
        adapter = null
    }

    /**
     * 重置分页状态
     * 清空页码、数据和适配器，为新的数据加载做准备
     */
    protected fun resetPage() {
        Logger.d("重置分页状态：清空页码和数据")
        page = 1
        pageData.clear()
        (adapter as? BaseAdapter<*, T>)?.updateItems(emptyList())
    }

    /**
     * 重新加载数据
     * 重置分页状态并重新开始加载，通常用于下拉刷新
     */
    fun reload() {
        Logger.d("重新加载数据：从第一页开始")
        resetPage()
        hasMoreData = true
        statusPage.showLoading()
        loadDataInside()
    }

    /**
     * 内部数据加载核心方法
     * 负责协调异步数据加载、状态更新和错误处理
     *
     * @param callback 加载完成后的回调，参数为(是否成功, 是否还有更多数据)
     */
    protected open fun loadDataInside(callback: ((success: Boolean, hasMore: Boolean) -> Unit)? = null) {
        // 防护检查：Fragment状态和重复加载
        if (!isAdded || activity == null || isLoading) {
            Logger.d("数据加载中止：Fragment未添加或正在加载中")
            callback?.invoke(false, hasMoreData)
            return
        }
        
        isLoading = true
        Logger.d("开始加载数据：第${page}页")

        launch {

            // 在IO线程中执行数据加载，避免阻塞UI
            val resultData = withContext(Dispatchers.IO) { loadData() }

            // 根据加载结果更新UI状态
            if (resultData.isEmpty()) {
                Logger.d("第${page}页无数据返回")
                handleEmptyResult()
                hasMoreData = false
                callback?.invoke(true, false)
            } else {
                Logger.d("第${page}页加载成功：${resultData.size}条数据")
                handleDataResult(resultData)
                hasMoreData = resultData.size >= pageSize
                callback?.invoke(true, hasMoreData)
            }
        }
    }

    /**
     * 处理空数据结果
     * 根据是否为首页决定显示空状态还是内容状态
     */
    private fun handleEmptyResult() {
        if (pageData.isEmpty()) {
            statusPage.showEmpty()
        } else {
            statusPage.showContent()
        }
    }

    /**
     * 处理有数据的结果
     * 更新数据集合和适配器
     */
    private fun handleDataResult(resultData: List<T>) {
        pageData.addAll(resultData)
        statusPage.showContent()
        (adapter as? BaseAdapter<*, T>)?.updateItems(pageData.toList())
    }

    /**
     * 绑定下拉刷新事件
     * 设置合理的触发距离和防重复处理
     */
    private fun bindLoadDataEvent() {
        swipeRefreshLayout.setDistanceToTriggerSync(200)
        swipeRefreshLayout.setOnRefreshListener {
            if (isLoading) {
                Logger.d("下拉刷新忽略：正在加载中")
                return@setOnRefreshListener
            }

            Logger.d("触发下拉刷新")
            reload()
        }
    }

    /**
     * 绑定上拉加载更多事件
     * 智能预加载：距离底部5个item时触发加载
     */
    private fun bindLoadMoreEvent() {
        val recyclerView = statusPage.contentView ?: return
        recyclerView.clearOnScrollListeners()
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                // 快速退出：无更多数据、正在加载或非向下滚动
                if (!hasMoreData || isLoading || dy <= 0) return

                val layoutManager = rv.layoutManager as? LinearLayoutManager ?: return
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                // 预加载阈值：距离底部5个item时触发
                if (lastVisibleItem >= totalItemCount - 5) {
                    Logger.d("触发加载更多：当前位置${lastVisibleItem}，总数${totalItemCount}")
                    
                    page++
                    loadDataInside { success, hasMore ->
                        if (!success) {
                            Logger.d("加载更多失败：回退页码")
                            page--
                        }
                        hasMoreData = hasMore
                    }
                }
            }
        })
    }

    // ================================
    // 列表操作辅助函数
    // ================================

    /**
     * 添加单个数据项到列表末尾
     * @param item 要添加的数据项
     */
    protected fun addItem(item: T) {
        pageData.add(item)
        updateAdapter()
        Logger.d("添加数据项：当前总数${pageData.size}")
    }

    /**
     * 添加多个数据项到列表末尾
     * @param items 要添加的数据项列表
     */
    protected fun addItems(items: List<T>) {
        pageData.addAll(items)
        updateAdapter()
        Logger.d("批量添加数据项：${items.size}条，当前总数${pageData.size}")
    }

    /**
     * 在指定位置插入数据项
     * @param index 插入位置
     * @param item 要插入的数据项
     */
    protected fun insertItem(index: Int, item: T) {
        if (index in 0..pageData.size) {
            pageData.add(index, item)
            updateAdapter()
            Logger.d("在位置${index}插入数据项：当前总数${pageData.size}")
        } else {
            Logger.w("插入位置无效：${index}，列表大小：${pageData.size}")
        }
    }

    /**
     * 移除指定数据项
     * @param item 要移除的数据项
     * @return 是否移除成功
     */
    protected fun removeItem(item: T): Boolean {
        val removed = pageData.remove(item)
        if (removed) {
            updateAdapter()
            Logger.d("移除数据项成功：当前总数${pageData.size}")

            // 如果列表为空，显示空状态
            if (pageData.isEmpty()) {
                statusPage.showEmpty()
            }
        } else {
            Logger.w("移除数据项失败：项目不存在")
        }
        return removed
    }

    /**
     * 移除指定位置的数据项
     * @param index 要移除的位置
     * @return 被移除的数据项，如果位置无效则返回null
     */
    protected fun removeItemAt(index: Int): T? {
        return if (index in 0 until pageData.size) {
            val removedItem = pageData.removeAt(index)
            updateAdapter()
            Logger.d("移除位置${index}的数据项：当前总数${pageData.size}")

            // 如果列表为空，显示空状态
            if (pageData.isEmpty()) {
                statusPage.showEmpty()
            }

            removedItem
        } else {
            Logger.w("移除位置无效：${index}，列表大小：${pageData.size}")
            null
        }
    }

    /**
     * 更新指定位置的数据项
     * @param index 要更新的位置
     * @param item 新的数据项
     * @return 是否更新成功
     */
    protected fun updateItem(index: Int, item: T): Boolean {
        return if (index in 0 until pageData.size) {
            pageData[index] = item
            updateAdapter()
            Logger.d("更新位置${index}的数据项")
            true
        } else {
            Logger.w("更新位置无效：${index}，列表大小：${pageData.size}")
            false
        }
    }

    /**
     * 替换指定数据项
     * @param oldItem 要替换的旧数据项
     * @param newItem 新的数据项
     * @return 是否替换成功
     */
    protected fun replaceItem(oldItem: T, newItem: T): Boolean {
        val index = pageData.indexOf(oldItem)
        return if (index != -1) {
            pageData[index] = newItem
            updateAdapter()
            Logger.d("替换数据项成功：位置${index}")
            true
        } else {
            Logger.w("替换数据项失败：旧项目不存在")
            false
        }
    }

    /**
     * 清空所有数据
     */
    protected fun clearItems() {
        pageData.clear()
        updateAdapter()
        statusPage.showEmpty()
        Logger.d("清空所有数据项")
    }

    /**
     * 获取数据项数量
     * @return 当前数据项总数
     */
    protected fun getItemCount(): Int = pageData.size

    /**
     * 获取指定位置的数据项
     * @param index 位置索引
     * @return 数据项，如果位置无效则返回null
     */
    protected fun getItem(index: Int): T? {
        return if (index in 0 until pageData.size) {
            pageData[index]
        } else {
            Logger.w("获取位置无效：${index}，列表大小：${pageData.size}")
            null
        }
    }

    /**
     * 查找数据项的位置
     * @param item 要查找的数据项
     * @return 位置索引，如果不存在则返回-1
     */
    protected fun indexOf(item: T): Int = pageData.indexOf(item)

    /**
     * 检查是否包含指定数据项
     * @param item 要检查的数据项
     * @return 是否包含
     */
    protected fun contains(item: T): Boolean = pageData.contains(item)

    /**
     * 更新适配器数据
     * 统一的适配器更新方法，确保数据同步
     */
    private fun updateAdapter() {
        (adapter as? BaseAdapter<*, T>)?.updateItems(pageData.toList())
    }
}
