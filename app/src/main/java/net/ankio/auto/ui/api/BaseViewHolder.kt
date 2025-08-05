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

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * RecyclerView ViewHolder 基类
 *
 * 提供通用的 ViewHolder 功能，包括：
 * - ViewBinding 支持
 * - 数据绑定
 * - 生命周期管理
 *
 * @param T ViewBinding 类型
 * @param E 数据实体类型
 * @param binding ViewBinding 实例
 */
open class BaseViewHolder<T : ViewBinding, E>(val binding: T) :
    RecyclerView.ViewHolder(binding.root) {

    /** 当前绑定的数据项 */
    var item: E? = null

    /** 上下文对象 */
    var context: Context = binding.root.context

    /**
     * 清理ViewHolder资源
     *
     * 在ViewHolder被回收时调用，用于清理相关资源
     */
    open fun clear() {
        // 清空数据项引用
        item = null
        // 子类可以重写此方法来清理特定资源
    }
}