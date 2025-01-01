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
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KINDither express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package net.ankio.auto.ui.api

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class BaseAdapter<T : ViewBinding, E>(
    bindingClass: Class<T>
) : RecyclerView.Adapter<BaseViewHolder<T, E>>() {

    private val items = mutableListOf<E>()

    private val inflateMethod = bindingClass.getMethod(
        "inflate",
        LayoutInflater::class.java,
        ViewGroup::class.java,
        Boolean::class.javaPrimitiveType
    )

    fun updateItem(index: Int, item: E) {
        if (index < 0 || index >= items.size) {
            return
        }
        items[index] = item
        notifyItemChanged(index)
    }

    fun size(): Int {
        return items.size
    }

    fun removeItem(index: Int) {
        if (index < 0 || index >= items.size) {
            return
        }
        items.removeAt(index)
        notifyItemRemoved(index)
    }

    fun removeItem(item: E) {
        val index = items.indexOf(item)
        if (index < 0) {
            return
        }
        items.removeAt(index)
        notifyItemRemoved(index)
    }

    fun indexOf(item: E): Int {
        return items.indexOf(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<T, E> {
        return try {
            val binding = inflateMethod.invoke(
                null,
                LayoutInflater.from(parent.context),
                parent,
                false
            ) as T
            BaseViewHolder<T, E>(binding).also { holder ->
                onInitViewHolder(holder)
            }
        } catch (e: Exception) {
            throw IllegalStateException("ViewBinding inflation failed", e)
        }
    }


    fun getItems(): List<E> = items.toList()


    abstract fun onInitViewHolder(holder: BaseViewHolder<T, E>)

    override fun getItemCount(): Int {
        return items.size
    }


    override fun onBindViewHolder(holder: BaseViewHolder<T, E>, position: Int) {
        // 绑定数据到视图
        val data = items[position]
        holder.item = data
        onBindViewHolder(holder, data, position)
    }

    abstract fun onBindViewHolder(holder: BaseViewHolder<T, E>, data: E, position: Int)

    override fun onViewRecycled(holder: BaseViewHolder<T, E>) {
        super.onViewRecycled(holder)
        holder.clear()
    }


    override fun onFailedToRecycleView(holder: BaseViewHolder<T, E>): Boolean {
        holder.clear()
        return super.onFailedToRecycleView(holder)
    }

    fun updateItems(newItems: List<E>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = items.size
            override fun getNewListSize(): Int = newItems.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return areItemsSame(items[oldItemPosition], newItems[newItemPosition])
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return areContentsSame(items[oldItemPosition], newItems[newItemPosition])
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items.clear()
        items.addAll(newItems)
        //Logger.d("updateItems: ${items.size},diffResult:$diffResult")
        diffResult.dispatchUpdatesTo(this)
    }

    // 需要在子类中实现的方法，用于判断两个项目是否是同一个项目
    protected abstract fun areItemsSame(oldItem: E, newItem: E): Boolean

    // 需要在子类中实现的方法，用于判断两个项目的内容是否相同
    protected abstract fun areContentsSame(oldItem: E, newItem: E): Boolean

}
