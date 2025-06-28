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

import android.app.Activity
import android.content.Context
import androidx.annotation.CallSuper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import net.ankio.auto.storage.Logger

/**
 * UI组件基类，提供生命周期管理和ViewBinding支持
 *
 * 该类实现了DefaultLifecycleObserver接口，自动管理组件的生命周期，
 * 子类可以通过重写相应的方法来处理生命周期事件。
 *
 * @param T ViewBinding类型，用于类型安全的视图绑定
 * @param binding ViewBinding实例，提供对视图的访问
 * @param lifecycle 生命周期对象，用于监听生命周期事件
 */
abstract class BaseComponent<T : ViewBinding>(
    protected val binding: T,
    private val lifecycle: Lifecycle
) : DefaultLifecycleObserver {

    /** 上下文对象，从binding的根视图获取 */
    protected val context: Context = binding.root.context

    init {
        // 将当前组件注册为生命周期观察者
        lifecycle.addObserver(this)
    }

    protected lateinit var activity: Activity
    fun bindActivity(activity: Activity) {
        this.activity = activity
    }
    /**
     * 组件初始化方法，在组件创建后调用
     *
     * 子类应该重写此方法来执行初始化操作，如设置监听器、加载数据等。
     * 此方法在bindAs扩展函数中被自动调用。
     */
    @CallSuper
    open fun init() {
        Logger.d("BaseComponent init called: ${this.javaClass.simpleName}")
    }

    /**
     * 页面恢复时调用，子类可实现
     *
     * 当组件所在的页面从后台恢复到前台时，此方法会被调用。
     * 适合在这里执行需要重新激活的操作，如刷新数据、恢复动画等。
     */
    @CallSuper
    open fun resume() {
        Logger.d("BaseComponent resume called: ${this.javaClass.simpleName}")
    }

    /**
     * 页面停止时调用，子类可实现
     *
     * 当组件所在的页面进入后台时，此方法会被调用。
     * 适合在这里执行暂停操作，如停止动画、暂停数据更新等。
     */
    @CallSuper
    open fun stop() {
        Logger.d("BaseComponent stop called: ${this.javaClass.simpleName}")
    }

    /**
     * 页面销毁时调用，子类可清理资源
     *
     * 当组件被销毁时，此方法会被调用。
     * 适合在这里执行清理操作，如取消网络请求、释放资源等。
     */
    @CallSuper
    open fun cleanup() {
        Logger.d("BaseComponent cleanup called: ${this.javaClass.simpleName}")
    }

    /**
     * 生命周期恢复事件处理
     * 当页面恢复时自动调用resume方法
     */
    final override fun onResume(owner: LifecycleOwner) = resume()

    /**
     * 生命周期停止事件处理
     * 当页面停止时自动调用stop方法
     */
    final override fun onStop(owner: LifecycleOwner) = stop()

    /**
     * 生命周期销毁事件处理
     * 当页面销毁时自动清理资源并移除生命周期观察者
     */
    final override fun onDestroy(owner: LifecycleOwner) {
        Logger.d("BaseComponent onDestroy called: ${this.javaClass.simpleName}")
        lifecycle.removeObserver(this)
        cleanup()
    }
}

/**
 * ViewBinding扩展函数，用于创建并初始化BaseComponent实例
 *
 * 此函数通过反射查找匹配的构造函数，创建BaseComponent实例并自动调用init方法。
 * 使用此函数可以简化BaseComponent的创建过程。
 *
 * @param T BaseComponent的具体类型
 * @param lifecycle 生命周期对象
 * @return 初始化完成的BaseComponent实例
 * @throws IllegalArgumentException 当找不到匹配的构造函数时抛出
 */
inline fun <reified T : BaseComponent<*>> ViewBinding.bindAs(
    lifecycle: Lifecycle,
    activity: Activity? = null
): T {
    // 查找匹配的构造函数：第一个参数是当前ViewBinding类型，第二个参数是Lifecycle类型
    val constructor = T::class.constructors.find {
        it.parameters.size == 2 &&
                it.parameters[0].type.classifier == this::class &&
                it.parameters[1].type.classifier == Lifecycle::class
    }
        ?: error("Constructor (binding: ${this::class}, lifecycle: Lifecycle) not found in ${T::class}")

    // 创建实例并调用init方法
    return constructor.call(this, lifecycle).apply {
        if (activity != null) {
            bindActivity(activity)
        }
        init()
    }
}
