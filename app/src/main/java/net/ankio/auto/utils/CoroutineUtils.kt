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

package net.ankio.auto.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.storage.Logger
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * 协程工具类
 *
 * 设计原则（遵循Linus好品味）：
 * 1. 简洁实用：提供最常用的协程操作
 * 2. 消除重复：统一的线程切换逻辑和全局协程管理
 * 3. 类型安全：使用内联函数避免性能损失
 * 4. 零依赖：不依赖特定的UI框架
 * 5. 统一管理：融合了CoroutineManager的功能
 */
object CoroutineUtils {

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 统一的协程异常处理器：防止单个异常导致整个作用域崩溃
     */
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Logger.e("协程执行异常: ${throwable.message}", throwable)
    }

    /** 全局协程作用域 - 使用 SupervisorJob 防止异常传播 */
    private val mainJob = SupervisorJob()
    private val mainScope = CoroutineScope(Dispatchers.Main + mainJob + exceptionHandler)

    /**
     * 切换到主线程执行
     * 用于在IO线程中切换到主线程执行UI操作
     *
     * @param block 需要在主线程执行的代码块
     */
    suspend inline fun withMain(crossinline block: suspend () -> Unit) {
        withContext(Dispatchers.Main) { block() }
    }

    /**
     * 切换到IO线程执行
     * 用于执行网络请求、文件操作等IO密集型任务
     *
     * @param block 需要在IO线程执行的代码块
     * @return 执行结果
     */
    suspend inline fun <T> withIO(crossinline block: suspend () -> T): T {
        return withContext(Dispatchers.IO) { block() }
    }

    /**
     * 切换到Default线程执行
     * 用于执行CPU密集型任务，如数据处理、计算等
     *
     * @param block 需要在Default线程执行的代码块
     * @return 执行结果
     */
    suspend inline fun <T> withDefault(crossinline block: suspend () -> T): T {
        return withContext(Dispatchers.Default) { block() }
    }

    /**
     * 在主线程执行操作（非协程版本）
     * 如果当前已在主线程，直接执行；否则post到主线程
     *
     * @param action 需要在主线程执行的操作
     */
    fun runOnUiThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    /**
     * 延迟在主线程执行操作
     *
     * @param delayMillis 延迟时间（毫秒）
     * @param action 需要执行的操作
     */
    fun runOnUiThreadDelayed(delayMillis: Long, action: () -> Unit) {
        mainHandler.postDelayed(action, delayMillis)
    }

    /**
     * 检查当前是否在主线程
     *
     * @return true表示在主线程，false表示在其他线程
     */
    fun isMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }

    /**
     * 在全局主线程作用域启动协程
     * 融合了原CoroutineManager的功能
     *
     * @param context 协程上下文
     * @param block 协程代码块
     */
    fun launchOnMain(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend CoroutineScope.() -> Unit
    ) {
        mainScope.launch(context) {
            try {
                block()
            } catch (e: CancellationException) {
                Logger.d("全局协程已取消: ${e.message}")
            }
        }
    }

    /**
     * 取消所有全局协程
     * 用于应用退出时的清理
     */
    fun cancelAll() {
        mainJob.cancel()
    }
}
