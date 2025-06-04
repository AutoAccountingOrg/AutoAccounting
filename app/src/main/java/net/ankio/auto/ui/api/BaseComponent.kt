/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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
import androidx.annotation.CallSuper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding

abstract class BaseComponent<T : ViewBinding>(
    protected val binding: T,
    private val lifecycle: Lifecycle
) : DefaultLifecycleObserver {

    protected val context: Context = binding.root.context

    init {
        lifecycle.addObserver(this)
    }

    @CallSuper
    open fun init() {
    }

    /** 页面恢复时调用，子类可实现 */
    @CallSuper
    open fun resume() {
    }

    /** 页面停止时调用，子类可实现 */
    @CallSuper
    open fun stop() {
    }

    /** 页面销毁时调用，子类可清理资源 */
    @CallSuper
    open fun cleanup() {
    }

    final override fun onResume(owner: LifecycleOwner) = resume()
    final override fun onStop(owner: LifecycleOwner) = stop()
    final override fun onDestroy(owner: LifecycleOwner) {
        lifecycle.removeObserver(this)
        cleanup()
    }
}

inline fun <reified T : BaseComponent<*>> ViewBinding.bindAs(
    lifecycle: Lifecycle
): T {
    val constructor = T::class.constructors.find {
        it.parameters.size == 2 &&
                it.parameters[0].type.classifier == this::class &&
                it.parameters[1].type.classifier == Lifecycle::class
    }
        ?: error("Constructor (binding: ${this::class}, lifecycle: Lifecycle) not found in ${T::class}")

    return constructor.call(this, lifecycle).apply { init() }
}
