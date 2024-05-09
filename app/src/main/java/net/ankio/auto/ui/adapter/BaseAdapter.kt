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

abstract class BaseAdapter(open val dataItems: List<Any>, val viewBindingClazz: Class<*>) : RecyclerView.Adapter<BaseViewHolder>() {
    open fun wrapHolder(viewBinding: ViewBinding): BaseViewHolder {
        return BaseViewHolder(viewBinding, viewBinding.root.context)
    }

    protected var clickPosition = 0

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BaseViewHolder {
        // 反射判断viewBindingClazz是否为viewbing的子类，并存在inflate方法，存在就调用
        val inflateMethod =
            viewBindingClazz.getDeclaredMethod(
                "inflate",
                LayoutInflater::class.java,
                ViewGroup::class.java,
                Boolean::class.java,
            )
        val viewBinding = inflateMethod.invoke(null, LayoutInflater.from(parent.context), parent, false) as ViewBinding
        return wrapHolder(viewBinding)
    }

    override fun getItemCount(): Int {
        return dataItems.size
    }

    abstract fun onBindView(
        holder: BaseViewHolder,
        item: Any,
        position: Int,
    )

    abstract fun onInitView(holder: BaseViewHolder)

    override fun onBindViewHolder(
        holder: BaseViewHolder,
        position: Int,
    ) {
        holder.createScope()
        val item = dataItems[position]
        runCatching {
            clickPosition = position
            if (!holder.hasInit) {
                onInitView(holder)
                holder.hasInit = true
            }
            onBindView(holder, item, position)
        }.onFailure {
            it.printStackTrace()
        }
    }

    override fun onViewDetachedFromWindow(holder: BaseViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.cancelScope()
    }
}
