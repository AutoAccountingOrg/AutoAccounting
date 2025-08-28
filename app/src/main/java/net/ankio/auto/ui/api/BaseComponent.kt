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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.SystemUtils.findLifecycleOwner
import kotlin.coroutines.cancellation.CancellationException

/**
 * UI组件基类，提供生命周期管理和ViewBinding支持
 *
 * 该类实现了DefaultLifecycleObserver接口，自动管理组件的生命周期，
 * 子类可以通过重写相应的方法来处理生命周期事件。
 *
 * 设计原则（遵循Linus好品味）：
 * 1. 简化构造：只需要ViewBinding，自动推断LifecycleOwner
 * 2. 消除重复：不再需要手动传递lifecycle参数
 * 3. 向后兼容：保留原有构造函数，支持渐进式迁移
 *
 * @param T ViewBinding类型，用于类型安全的视图绑定
 * @param binding ViewBinding实例，提供对视图的访问
 */
abstract class BaseComponent<T : ViewBinding> : DefaultLifecycleObserver {

    /**
     * ViewBinding 实例，在组件销毁时会被置空以防止内存泄漏
     */
    private var _binding: T? = null

    /**
     * 对外暴露的 ViewBinding 属性，提供非空访问
     * 在组件生命周期内可以安全使用
     */
    protected val binding get() = _binding!!

    /** 生命周期对象，从ViewBinding的Context自动推断 */
    private val lifecycle: Lifecycle

    /**
     * 推荐的构造函数 - 只需要ViewBinding，自动推断生命周期
     *
     * @param binding ViewBinding实例
     */
    constructor(binding: T) {
        this._binding = binding
        this.lifecycle = binding.root.context.findLifecycleOwner().lifecycle
        lifecycle.addObserver(this)
    }



    /** 上下文对象，从binding的根视图获取 */
    protected val context: Context get() = binding.root.context

    /** 组件级别的协程作用域，用于管理异步操作 */
    protected val componentScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    protected fun launch(block: suspend CoroutineScope.() -> Unit) {
        componentScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                Logger.d("组件协程已取消: ${e.message}")
            } catch (e: Exception) {
                Logger.e("组件协程执行错误", e)
            }
        }
    }

    /**
     * 组件初始化方法，在组件创建后调用
     *
     * 子类应该重写此方法来执行初始化操作，如设置监听器、加载数据等。
     * 此方法在bindAs扩展函数中被自动调用。
     */
    @CallSuper
    open fun onComponentCreate() {
        //Logger.d("BaseComponent init called: ${this.javaClass.simpleName}")
    }

    /**
     * 页面恢复时调用，子类可实现
     *
     * 当组件所在的页面从后台恢复到前台时，此方法会被调用。
     * 适合在这里执行需要重新激活的操作，如刷新数据、恢复动画等。
     */
    @CallSuper
    open fun onComponentResume() {
        //Logger.d("BaseComponent resume called: ${this.javaClass.simpleName}")
    }

    /**
     * 页面停止时调用，子类可实现
     *
     * 当组件所在的页面进入后台时，此方法会被调用。
     * 适合在这里执行暂停操作，如停止动画、暂停数据更新等。
     */
    @CallSuper
    open fun onComponentStop() {
        // Logger.d("BaseComponent stop called: ${this.javaClass.simpleName}")
    }

    /**
     * 页面销毁时调用，子类可清理资源
     *
     * 当组件被销毁时，此方法会被调用。
     * 适合在这里执行清理操作，如取消网络请求、释放资源等。
     */
    @CallSuper
    open fun onComponentDestroy() {
        // 取消组件协程作用域，防止内存泄露
        componentScope.cancel()
        // 清理 binding 中的所有监听器，防止内存泄漏
        clearBindingListeners()
        // 将 ViewBinding 实例置空以防止内存泄漏
        _binding = null
    }

    /**
     * 清理 ViewBinding 中的监听器，防止内存泄漏
     * 
     * 通过反射清理 binding.root 及其子视图的所有监听器，
     * 这是防止 BaseComponent 持有视图引用导致内存泄漏的关键步骤。
     */
    private fun clearBindingListeners() {
        try {
            // 在置空前清理根视图及其所有子视图的监听器
            _binding?.root?.let { rootView ->
                clearViewListeners(rootView)
            }
        } catch (e: Exception) {
            Logger.e("清理 ViewBinding 监听器失败", e)
        }
    }

    /**
     * 递归清理视图及其子视图的所有监听器
     */
    private fun clearViewListeners(view: android.view.View) {
        try {
            // 清理点击监听器
            view.setOnClickListener(null)
            view.setOnLongClickListener(null)
            view.setOnTouchListener(null)
            view.setOnFocusChangeListener(null)

            // 如果是 ViewGroup，递归清理子视图
            if (view is android.view.ViewGroup) {
                for (i in 0 until view.childCount) {
                    clearViewListeners(view.getChildAt(i))
                }
            }
        } catch (e: Exception) {
            // 忽略清理过程中的异常，避免影响正常销毁流程
            Logger.d("清理视图监听器时出现异常: ${e.message}")
        }
    }

    /**
     * 生命周期恢复事件处理
     * 当页面恢复时自动调用resume方法
     */
    final override fun onResume(owner: LifecycleOwner) = onComponentResume()

    /**
     * 生命周期停止事件处理
     * 当页面停止时自动调用stop方法
     */
    final override fun onStop(owner: LifecycleOwner) = onComponentStop()

    /**
     * 生命周期销毁事件处理
     * 当页面销毁时自动清理资源并移除生命周期观察者
     */
    final override fun onDestroy(owner: LifecycleOwner) {
        //Logger.d("BaseComponent onDestroy called: ${this.javaClass.simpleName}")
        lifecycle.removeObserver(this)
        onComponentDestroy()
    }
}


/**
 * ViewBinding扩展函数，创建BaseComponent实例
 *
 * 此函数通过反射查找匹配的构造函数，创建BaseComponent实例并自动调用初始化方法。
 * 自动从ViewBinding的Context推断LifecycleOwner，无需手动传递参数。
 *
 * 支持两种使用方式：
 * 1. 显式指定类型：binding.component.bindAs<MyComponent>()
 * 2. 类型推断：val component: MyComponent = binding.component.bindAs()
 *
 * @param T BaseComponent的具体类型（可显式指定或由编译器推断）
 * @return 初始化完成的BaseComponent实例
 * @throws IllegalArgumentException 当找不到匹配的构造函数时抛出
 */
inline fun <reified T : BaseComponent<*>> ViewBinding.bindAs(): T {
    // 查找匹配的构造函数：只接受ViewBinding的单参数构造函数
    val constructor = T::class.constructors.find {
        it.parameters.size == 1 &&
                it.parameters[0].type.classifier == this::class
    }
        ?: error("Constructor (binding: ${this::class}) not found in ${T::class}")

    // 创建实例并调用组件初始化方法
    return constructor.call(this).apply {
        onComponentCreate()
    }
}
