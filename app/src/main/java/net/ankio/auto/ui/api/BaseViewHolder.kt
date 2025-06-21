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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import net.ankio.auto.storage.Logger

/**
 * RecyclerView ViewHolder 基类
 *
 * 提供通用的 ViewHolder 功能，包括：
 * - ViewBinding 支持
 * - 协程作用域管理
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

    /** ViewHolder 的协程作用域，用于管理异步操作 */
    private val viewHolderScope by lazy {
        CoroutineScope(Dispatchers.Main + SupervisorJob())
    }

    /**
     * 在 ViewHolder 的协程作用域中启动协程
     *
     * 用于执行异步操作，如网络请求、图片加载等。
     * 当 ViewHolder 被回收时，所有协程会自动取消。
     *
     * @param block 要执行的协程代码块
     */
    fun launch(block: suspend CoroutineScope.() -> Unit) = viewHolderScope.launch {
        try {
            block()
        } catch (e: CancellationException) {
            // 协程被取消，这是正常情况，不需要记录日志
            Logger.d("Coroutine cancelled in ViewHolder: ${e.message}")
        } catch (e: Exception) {
            Logger.e("Error in ViewHolder coroutine", e)
        }
    }

    /**
     * 清理 ViewHolder 资源
     *
     * 取消所有正在运行的协程，防止内存泄漏
     */
    fun clear() {
        viewHolderScope.coroutineContext.cancelChildren()
        Logger.d("Cleared ViewHolder resources")
    }
}