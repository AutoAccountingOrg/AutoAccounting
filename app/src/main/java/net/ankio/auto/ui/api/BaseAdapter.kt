package net.ankio.auto.ui.api

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.ankio.auto.storage.Logger
import java.lang.reflect.ParameterizedType
import kotlin.coroutines.cancellation.CancellationException

/**
 * RecyclerView 适配器基类
 *
 * 提供通用的 RecyclerView 适配器功能，包括：
 * - 数据管理（增删改查）
 * - ViewBinding 自动绑定
 * - DiffUtil 支持
 * - ViewHolder 生命周期管理
 *
 * @param T ViewBinding 类型，用于视图绑定
 * @param E 数据实体类型
 */
abstract class BaseAdapter<T : ViewBinding, E> : RecyclerView.Adapter<BaseViewHolder<T, E>>() {

    // Adapter 级别的协程作用域
    private val adapterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * 在Adapter生命周期内启动协程
     * 统一处理异常，业务代码无需再捕获异常
     *
     * @param block 协程代码块，专注于业务逻辑
     */
    protected fun launchInAdapter(block: suspend CoroutineScope.() -> Unit) {
        adapterScope.launch {
            runCatching {
                block()
            }.onFailure { e ->
                when (e) {
                    is CancellationException -> {
                        Logger.d("适配器协程已取消: ${e.message}")
                    }

                    else -> {
                        Logger.e("适配器协程执行异常: ${javaClass.simpleName}", e)
                        // 可以在这里添加全局异常处理逻辑
                    }
                }
            }
        }
    }

    /** 数据列表 */
    private val items = mutableListOf<E>()

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        adapterScope.cancel()
    }

    /**
     * 统一处理列表更新的 DiffUtil 流程，消除重复代码
     *
     * @param buildNewList 基于旧数据生成新列表的函数（应返回不可变副本）
     */
    private fun applyListUpdate(buildNewList: (List<E>) -> List<E>) {
        val oldItems = items.toList()
        val newList = buildNewList(oldItems)

        val diffResult = DiffUtil.calculateDiff(AdapterDiffCallback(oldItems, newList))

        items.clear()
        items.addAll(newList)

        diffResult.dispatchUpdatesTo(this)

        Logger.d("已使用 DiffUtil 更新数据项: 旧=${oldItems.size}项, 新=${newList.size}项")
    }
    /**
     * 更新指定位置的数据项
     *
     * @param index 要更新的位置索引
     * @param item 新的数据项
     */
    fun updateItem(index: Int, item: E): Boolean {
        if (index in items.indices) {
            items[index] = item
            notifyItemChanged(index)
            Logger.d("已更新位置 $index 的数据项")
            return true
        } else {
            Logger.w("更新数据项失败: 索引 $index 超出范围 (大小: ${items.size})")
            return false
        }
    }

    /**
     * 获取数据列表大小
     *
     * @return 数据项数量
     */
    fun size(): Int = items.size

    /**
     * 移除指定位置的数据项
     *
     * @param index 要移除的位置索引
     */
    fun removeItem(index: Int): Boolean {
        if (index in items.indices) {
            items.removeAt(index)
            notifyItemRemoved(index)
            Logger.d("已移除位置 $index 的数据项")
            return true
        } else {
            Logger.w("移除数据项失败: 索引 $index 超出范围 (大小: ${items.size})")
            return false
        }
    }

    /**
     * 移除指定的数据项
     *
     * @param item 要移除的数据项
     */
    fun removeItem(item: E): Boolean {
        val index = items.indexOf(item)
        if (index >= 0) {
            return removeItem(index)
        } else {
            Logger.w("移除数据项失败: 在列表中未找到该项")
            return false
        }
    }

    fun index(index: Int): E? = items[index]
    /**
     * 获取数据项在列表中的索引
     *
     * @param item 要查找的数据项
     * @return 数据项的索引，如果不存在则返回 -1
     */
    fun indexOf(item: E): Int = items.indexOf(item)

    /**
     * 获取当前数据列表的副本
     *
     * @return 数据列表的不可变副本
     */
    fun getItems(): List<E> = items.toList()

    /**
     * 提交新的数据列表（使用 DiffUtil 高效更新）
     *
     * @param newItems 新的数据列表
     */
    fun replaceItems(newItems: List<E>) {
        applyListUpdate { _ -> newItems }
    }

    /**
     * 追加新的数据列表（使用 DiffUtil 高效更新）
     *
     * @param newItems 追加的数据列表
     */
    fun submitItems(newItems: List<E>) {
        applyListUpdate { old -> old + newItems }
    }

    /**
     * 更新数据列表（使用 DiffUtil 进行高效更新）
     *
     * @param newItems 新的数据列表
     */
    fun updateItems(newItems: List<E>) {
        applyListUpdate { _ -> newItems }
    }

    /**
     * 创建 ViewHolder
     *
     * 通过反射自动创建 ViewBinding 实例，并调用子类的初始化方法
     *
     * @param parent 父视图组
     * @param viewType 视图类型
     * @return 创建的 ViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<T, E> {
        val binding = try {
            // 通过反射获取泛型类型信息
            val type = javaClass.genericSuperclass as ParameterizedType
            val bindingClass = type.actualTypeArguments.firstOrNull {
                it is Class<*> && ViewBinding::class.java.isAssignableFrom(it)
            } as? Class<E>
                ?: throw IllegalStateException("Cannot infer ViewBinding type for ${javaClass.name}")

            // 获取 ViewBinding 的 inflate 方法
            val method = bindingClass.getDeclaredMethod(
                "inflate",
                LayoutInflater::class.java,
                ViewGroup::class.java,
                Boolean::class.java
            )

            // 调用 inflate 方法创建 ViewBinding 实例
            method.invoke(null, LayoutInflater.from(parent.context), parent, false) as T
        } catch (e: Exception) {
            Logger.e("${javaClass.name} 的 ViewBinding 创建失败", e)
            throw IllegalStateException("ViewBinding inflation failed", e)
        }

        val holder = BaseViewHolder<T, E>(binding)
        onInitViewHolder(holder)
        return holder
    }

    /**
     * 绑定 ViewHolder 数据
     *
     * @param holder ViewHolder 实例
     * @param position 数据位置
     */
    override fun onBindViewHolder(holder: BaseViewHolder<T, E>, position: Int) {
        holder.clear()
        val item = items[position]
        holder.item = item
        onBindViewHolder(holder, item, position)
    }

    /**
     * 获取数据项数量
     *
     * @return 数据列表大小
     */
    override fun getItemCount(): Int = items.size

    /**
     * ViewHolder 被回收时的回调
     *
     * @param holder 被回收的 ViewHolder
     */
    override fun onViewRecycled(holder: BaseViewHolder<T, E>) {
        super.onViewRecycled(holder)
        holder.clear()
    }

    /**
     * ViewHolder 回收失败时的回调
     *
     * @param holder 回收失败的 ViewHolder
     * @return 是否允许回收
     */
    override fun onFailedToRecycleView(holder: BaseViewHolder<T, E>): Boolean {
        holder.clear()
        return super.onFailedToRecycleView(holder)
    }

    /**
     * DiffUtil 回调实现
     *
     * 用于高效的数据更新，避免不必要的视图刷新
     */
    private inner class AdapterDiffCallback(
        private val oldList: List<E>,
        private val newList: List<E>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return areItemsSame(oldList[oldItemPosition], newList[newItemPosition])
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return areContentsSame(oldList[oldItemPosition], newList[newItemPosition])
        }
    }

    /**
     * 判断两个数据项是否代表同一个实体
     *
     * 子类需要实现此方法来判断两个数据项是否代表同一个实体对象
     * 通常通过 ID 或其他唯一标识符来判断
     *
     * @param oldItem 旧数据项
     * @param newItem 新数据项
     * @return 如果代表同一个实体则返回 true
     */
    protected abstract fun areItemsSame(oldItem: E, newItem: E): Boolean

    /**
     * 判断两个数据项的内容是否一致
     *
     * 子类需要实现此方法来判断两个数据项的内容是否相同
     * 通常比较所有相关字段的值
     *
     * @param oldItem 旧数据项
     * @param newItem 新数据项
     * @return 如果内容一致则返回 true
     */
    protected abstract fun areContentsSame(oldItem: E, newItem: E): Boolean

    /**
     * 初始化 ViewHolder
     *
     * 子类可以在此方法中进行 ViewHolder 的初始化操作，
     * 如设置点击监听器、长按监听器等
     *
     * @param holder 要初始化的 ViewHolder
     */
    protected abstract fun onInitViewHolder(holder: BaseViewHolder<T, E>)

    /**
     * 绑定数据到 ViewHolder
     *
     * 子类需要实现此方法将数据绑定到 ViewHolder 的视图中
     *
     * @param holder ViewHolder 实例
     * @param data 要绑定的数据
     * @param position 数据在列表中的位置
     */
    protected abstract fun onBindViewHolder(holder: BaseViewHolder<T, E>, data: E, position: Int)
}
