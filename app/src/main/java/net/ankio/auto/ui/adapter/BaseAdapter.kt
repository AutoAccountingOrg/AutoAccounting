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

package net.ankio.auto.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import net.ankio.auto.models.BaseModel
import net.ankio.auto.ui.viewModes.BaseViewModel
import net.ankio.auto.storage.Logger

abstract class BaseAdapter< T:ViewBinding, E: BaseModel>(private val viewModel: BaseViewModel<out BaseModel>) : RecyclerView.Adapter<BaseViewHolder<T,E>>() {

    init {
        viewModel.dataList.observeForever { newData ->
            notifyDataSetChanged() // 整个数据集更新
        }
    }

   abstract fun getViewBindingClazz():Class<T>

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BaseViewHolder<T, E> {
        // 反射判断viewBindingClazz是否为viewbing的子类，并存在inflate方法，存在就调用
        val inflateMethod =
            getViewBindingClazz().getDeclaredMethod(
                "inflate",
                LayoutInflater::class.java,
                ViewGroup::class.java,
                Boolean::class.java,
            )
        val viewBinding = inflateMethod.invoke(null, LayoutInflater.from(parent.context), parent, false) as T
        return BaseViewHolder(viewBinding,parent.context)
    }

    override fun getItemCount(): Int {
        return viewModel.dataList.value?.size?:0
    }


    abstract fun onBindView(
        holder: BaseViewHolder<T,E>,
        item: Any,
    )

    abstract fun onInitView(holder: BaseViewHolder<T,E>)

    override fun onBindViewHolder(
        holder: BaseViewHolder<T,E>,
        position: Int,
    ) {
        runCatching {
            val item = viewModel.dataList.value?.get(position)?: throw IllegalArgumentException("position error! index=$position total=${itemCount}")
            holder.item = item as E
            if (!holder.hasInit) {
                onInitView(holder)
                holder.hasInit = true
            }
            onBindView(holder, item)
        }.onFailure {
           Logger.e("ItemException->",it)
        }
    }

     fun getHolderIndex(holder: BaseViewHolder<T, E>): Int{
        return viewModel.dataList.value?.indexOf(holder.item!!) ?: -1

     }
}



