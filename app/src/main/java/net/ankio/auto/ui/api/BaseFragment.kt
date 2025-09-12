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
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.core.view.children
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import io.github.oshai.kotlinlogging.KotlinLogging
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

    companion object {
        val logger = KotlinLogging.logger(this::class.java.name)
    }
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
     * 通用滚动容器（NestedScrollView/ScrollView）的引用
     * 仅用于保存与恢复滚动位置，避免子类重复实现。
     */
    private var _scrollContainer: View? = null

    /**
     * 通用滚动位置（纵向）保存值。
     */
    private var _savedScrollY: Int? = null

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
            val bindingClass = type.actualTypeArguments.firstOrNull {
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
            logger.error(e) { "Failed to create view for ${javaClass.simpleName}" }
            return null
        }
    }

    /**
     * 视图创建完成后，尝试定位通用滚动容器并恢复滚动位置。
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            _scrollContainer = findScrollContainer(binding.root)
            // 若之前保存过滚动位置，则在下一帧恢复，确保布局完成
            val targetY = _savedScrollY
            if (targetY != null) {
                Handler(Looper.getMainLooper()).post {
                    restoreScrollPosition(targetY)
                }
            }
        } catch (e: Exception) {
            logger.warn { "定位滚动容器失败：${e.message}" }
        }
    }

    /**
     * Fragment 视图销毁时的清理工作
     *
     * 将 ViewBinding 实例置空以防止内存泄漏。
     * 这是 ViewBinding 使用的最佳实践。
     */
    override fun onDestroyView() {
        super.onDestroyView()
        logger.debug { "Destroying view for ${javaClass.simpleName}" }
        _binding = null
        _scrollContainer = null
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
                        logger.debug { "Fragment协程已取消: ${e.message}" }
                    }

                    else -> {
                        logger.error(e) { "Fragmentt协程执行异常: ${javaClass.simpleName}" }
                    }
                }
            }
        }


    // ================================
    // 通用滚动位置保存/恢复（NestedScrollView/ScrollView）
    // ================================

    /**
     * 在Fragment暂停时保存滚动位置。
     */
    override fun onPause() {
        super.onPause()
        try {
            val sc = _scrollContainer
            if (sc is NestedScrollView) {
                _savedScrollY = sc.scrollY
                logger.debug { "保存 NestedScrollView 滚动位置：${_savedScrollY}" }
            } else if (sc is ScrollView) {
                _savedScrollY = sc.scrollY
                logger.debug { "保存 ScrollView 滚动位置：${_savedScrollY}" }
            }
        } catch (e: Exception) {
            logger.warn { "保存滚动位置失败：${e.message}" }
        }
    }

    /**
     * 恢复滚动位置。
     */
    private fun restoreScrollPosition(targetY: Int) {
        val sc = _scrollContainer
        try {
            when (sc) {
                is NestedScrollView -> sc.scrollTo(0, targetY)
                is ScrollView -> sc.scrollTo(0, targetY)
            }
            logger.debug { "恢复滚动位置到：${targetY}" }
        } catch (e: Exception) {
            logger.warn { "恢复滚动位置失败：${e.message}" }
        }
    }

    /**
     * 在根视图下查找首个 NestedScrollView 或 ScrollView。
     * 仅返回一个以保持简单，避免过度设计。
     */
    private fun findScrollContainer(root: View): View? {
        if (root is NestedScrollView || root is ScrollView) return root
        if (root is ViewGroup) {
            root.children.forEach { child ->
                val found = findScrollContainer(child)
                if (found != null) return found
            }
        }
        return null
    }
}