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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.*
import net.ankio.auto.storage.Logger
import java.lang.reflect.ParameterizedType

/**
 * 基础 Fragment 类，提供 ViewBinding 的自动绑定功能
 *
 * 该类通过反射机制自动推断 ViewBinding 类型，简化了 Fragment 的创建过程。
 * 子类只需要继承此类并指定对应的 ViewBinding 泛型参数即可。
 *
 * @param VB ViewBinding 类型，用于绑定视图
 */
abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    /**
     * ViewBinding 实例，在 Fragment 销毁时会被置空以防止内存泄漏
     */
    private var _binding: VB? = null

    /**
     * 对外暴露的 ViewBinding 属性，提供非空访问
     * 在 Fragment 生命周期内可以安全使用
     */
    protected val binding get() = _binding!!

    /**
     * 保存的滚动位置
     */
    private var savedScrollY: Int = 0

    /**
     * 创建 Fragment 的视图
     *
     * 通过反射机制自动推断 ViewBinding 类型并创建绑定实例。
     * 该方法会：
     * 1. 获取泛型参数中的 ViewBinding 类型
     * 2. 调用 ViewBinding 的 inflate 方法创建绑定实例
     * 3. 返回根视图
     *
     * @param inflater 布局填充器
     * @param container 父容器
     * @param savedInstanceState 保存的状态
     * @return 创建的视图，如果创建失败则返回 null
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        try {
            // 通过反射获取泛型参数中的 ViewBinding 类型
            val type = javaClass.genericSuperclass as ParameterizedType
            @Suppress("UNCHECKED_CAST") val bindingClass = type.actualTypeArguments.firstOrNull {
                it is Class<*> && ViewBinding::class.java.isAssignableFrom(it)
            } as? Class<VB>
                ?: throw IllegalStateException("Cannot infer ViewBinding type for ${javaClass.name}")

            // 获取 ViewBinding 的 inflate 方法
            val method = bindingClass.getDeclaredMethod(
                "inflate",
                LayoutInflater::class.java,
                ViewGroup::class.java,
                Boolean::class.java
            )

            // 调用 inflate 方法创建绑定实例
            @Suppress("UNCHECKED_CAST")
            _binding = method.invoke(null, inflater, container, false) as VB

            return binding.root
        } catch (e: Exception) {
            Logger.e("Failed to create view for ${javaClass.simpleName}", e)
            return null
        }
    }

    /**
     * 视图创建后恢复滚动位置
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 如果有保存的滚动位置，恢复它
        if (savedScrollY > 0) {
            view.post {
                findScrollView()?.scrollTo(0, savedScrollY)
            }
        }
    }

    /**
     * Fragment暂停时保存滚动位置
     */
    override fun onPause() {
        super.onPause()
        savedScrollY = findScrollView()?.scrollY ?: 0
    }

    /**
     * 查找第一个ScrollView或NestedScrollView
     */
    private fun findScrollView(): View? {
        return findScrollViewRecursive(binding.root)
    }

    /**
     * 递归查找滚动视图
     */
    private fun findScrollViewRecursive(view: View): View? {
        when (view) {
            is ScrollView, is NestedScrollView -> return view
            is ViewGroup -> {
                for (i in 0 until view.childCount) {
                    val found = findScrollViewRecursive(view.getChildAt(i))
                    if (found != null) return found
                }
            }
        }
        return null
    }

    /**
     * Fragment 视图销毁时的清理工作
     *
     * 将 ViewBinding 实例置空以防止内存泄漏。
     * 这是 ViewBinding 使用的最佳实践。
     */
    override fun onDestroyView() {
        super.onDestroyView()
        Logger.d("Destroying view for ${javaClass.simpleName}")
        _binding = null
    }

    /**
     * 检查 Fragment 的 UI 是否已准备就绪
     *
     * 该方法检查以下条件来确定 UI 状态：
     * 1. ViewBinding 实例是否已创建且非空
     * 2. Fragment 是否已添加到 Activity 中
     * 3. Fragment 的视图是否已创建且可访问
     *
     * 在以下场景中应该使用此方法：
     * - 在异步操作完成后更新 UI 前
     * - 在生命周期回调中访问视图前
     * - 在延迟执行的任务中操作视图前
     *
     * @return true 如果 UI 已准备就绪可以安全访问，false 否则
     */
    fun uiReady(): Boolean {
        return _binding != null &&
                isAdded &&
                view != null &&
                !isDetached &&
                !isRemoving
    }

    /**
     * 在Fragment生命周期内启动协程
     * 统一处理异常，业务代码无需再捕获异常
     *
     * @param block 协程代码块，专注于业务逻辑
     * @return 协程Job，可用于取消操作
     */
    protected fun launch(block: suspend CoroutineScope.() -> Unit): Job =
        lifecycleScope.launch(CoroutineExceptionHandler { _, _ -> }, block = block).apply {
            invokeOnCompletion { e ->
                when (e) {
                    null -> Unit // 正常完成不处理
                    is CancellationException -> {
                        Logger.d("Fragment协程已取消: ${e.message}")
                    }
                    else -> {
                        Logger.e("Fragment协程执行异常: ${javaClass.simpleName}", e)
                    }
                }
            }
        }
}