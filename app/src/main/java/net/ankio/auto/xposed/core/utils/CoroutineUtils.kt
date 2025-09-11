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

package net.ankio.auto.xposed.core.utils

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.ankio.auto.xposed.core.App.Companion.TAG

class CoroutineUtils {
    /**
     * 协程作用域的根 Job，使用 SupervisorJob 防止子协程异常级联取消整个作用域
     */
    private var job: Job = SupervisorJob()

    /**
     * 统一的异常处理器：记录未捕获异常
     */
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, throwable.message + "\n" + throwable.stackTraceToString())
    }

    /**
     * 默认 IO 作用域，附带异常处理
     */
    private var scope: CoroutineScope = CoroutineScope(job + exceptionHandler)


    fun launch(dispatcher: CoroutineDispatcher, block: suspend CoroutineScope.() -> Unit) {
        scope.launch(dispatcher) { block() }
    }

    fun launch(block: suspend CoroutineScope.() -> Unit) {

        scope.launch { block() }
    }

    fun cancel() {
        job.cancel()
    }

    companion object {
        private val instance by lazy { CoroutineUtils() }

        /**
         * 启动一个协程
         */
        fun withIO(block: suspend CoroutineScope.() -> Unit) {
            instance.launch(Dispatchers.IO, block)
        }

        /**
         * 启动一个协程
         */
        fun withMain(block: suspend CoroutineScope.() -> Unit) {
            instance.launch(Dispatchers.Main, block)
        }

    }
}