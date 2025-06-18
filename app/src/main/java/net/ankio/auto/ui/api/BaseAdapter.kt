package net.ankio.auto.ui.api

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.util.concurrent.ConcurrentHashMap

abstract class BaseAdapter<T : ViewBinding, E> : RecyclerView.Adapter<BaseViewHolder<T, E>>() {

    private val items = mutableListOf<E>()


    fun updateItem(index: Int, item: E) {
        if (index in items.indices) {
            items[index] = item
            notifyItemChanged(index)
        }
    }

    fun size(): Int = items.size

    fun removeItem(index: Int) {
        if (index in items.indices) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun removeItem(item: E) {
        val index = items.indexOf(item)
        if (index >= 0) removeItem(index)
    }

    fun indexOf(item: E): Int = items.indexOf(item)

    fun getItems(): List<E> = items.toList()

    fun submitItems(newItems: List<E>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun updateItems(newItems: List<E>) {
        val diffResult = DiffUtil.calculateDiff(AdapterDiffCallback(items, newItems))
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<T, E> {
        val binding = try {

            val type = javaClass.genericSuperclass as ParameterizedType
            val bindingClass = type.actualTypeArguments.firstOrNull {
                it is Class<*> && ViewBinding::class.java.isAssignableFrom(it)
            } as? Class<E>
                ?: throw IllegalStateException("Cannot infer ViewBinding type for ${javaClass.name}")

            val method = bindingClass.getDeclaredMethod(
                "inflate",
                LayoutInflater::class.java,
                ViewGroup::class.java,
                Boolean::class.java
            )


            method.invoke(null, LayoutInflater.from(parent.context), parent, false) as T
        } catch (e: Exception) {
            throw IllegalStateException("ViewBinding inflation failed", e)
        }

        return BaseViewHolder<T, E>(binding).also { onInitViewHolder(it) }
    }

    override fun onBindViewHolder(holder: BaseViewHolder<T, E>, position: Int) {
        val item = items[position]
        holder.item = item
        onBindViewHolder(holder, item, position)
    }

    override fun getItemCount(): Int = items.size

    override fun onViewRecycled(holder: BaseViewHolder<T, E>) {
        super.onViewRecycled(holder)
        holder.clear()
    }

    override fun onFailedToRecycleView(holder: BaseViewHolder<T, E>): Boolean {
        holder.clear()
        return super.onFailedToRecycleView(holder)
    }

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

    /** 子类用于判断两个数据是否代表同一个实体 */
    protected abstract fun areItemsSame(oldItem: E, newItem: E): Boolean

    /** 子类用于判断两个实体内容是否一致 */
    protected abstract fun areContentsSame(oldItem: E, newItem: E): Boolean

    /** 可选的初始化 ViewHolder 操作 */
    protected abstract fun onInitViewHolder(holder: BaseViewHolder<T, E>)

    /** 子类负责将 item 数据绑定到 holder */
    protected abstract fun onBindViewHolder(holder: BaseViewHolder<T, E>, data: E, position: Int)
}
