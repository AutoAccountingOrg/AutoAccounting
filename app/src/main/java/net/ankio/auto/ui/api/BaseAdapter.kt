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
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class BaseAdapter<T:ViewBinding,E>(private val bindingClass: Class<T>,private val list:MutableList<E>) : RecyclerView.Adapter<BaseViewHolder<T>>() {
    /**
     * 初始化View
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<T> {
        val method = bindingClass.getMethod(
            "inflate",
            LayoutInflater::class.java,
            ViewGroup::class.java,
            Boolean::class.javaPrimitiveType
        )
        val binding = method.invoke(null, LayoutInflater.from(parent.context), parent, false) as T

        val viewHolder = BaseViewHolder(binding, parent.context)

        onInitViewHolder(viewHolder)

        return viewHolder
    }

    /**
     * 初始化ViewHolder，执行监听等动作
     */
    abstract fun onInitViewHolder(holder: BaseViewHolder<T>)

    /**
     * 获取数据
     */
    override fun getItemCount(): Int {
        return list.size
    }


}