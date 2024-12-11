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

open class BaseViewHolder<T : ViewBinding, E>(val binding: T) :
    RecyclerView.ViewHolder(binding.root) {

    var item: E? = null
    var context: Context = binding.root.context

    private val viewHolderScope by lazy {
        CoroutineScope(Dispatchers.Main + SupervisorJob())
    }

    fun launch(block: suspend CoroutineScope.() -> Unit) = viewHolderScope.launch {
        try {
            block()
        } catch (e: CancellationException) {
            // 可以添加日志
        }
    }

    fun clear() {
        viewHolderScope.coroutineContext.cancelChildren()
    }
}