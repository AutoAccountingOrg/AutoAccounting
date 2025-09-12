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

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.ankio.auto.xposed.core.App.Companion.TAG
import io.github.oshai.kotlinlogging.KotlinLogging

class ThreadUtils {

    private val logger = KotlinLogging.logger(this::class.java.name)
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    fun launch(dispatcher: CoroutineDispatcher, block: suspend CoroutineScope.() -> Unit) {
        scope.launch(dispatcher) {
            runCatching {
                block()
            }.onFailure {
                logger.error(it) { }
            }
        }
    }

    fun launch(block: suspend CoroutineScope.() -> Unit) {
        scope.launch {
            runCatching {

                block()

            }.onFailure {
                logger.error(it) { }
            }
        }
    }

    fun cancel() {
        job.cancel()
    }

    /**
     * 在主线程运行
     * @param function Function0<Unit>
     */
    fun runOnUiThread(function: () -> Unit) {
        if (Looper.getMainLooper().thread != Thread.currentThread()) {
            Handler(Looper.getMainLooper()).post { function() }
        } else {
            function()
        }
    }


    companion object {
        private val instance by lazy { ThreadUtils() }
        fun launch(block: suspend CoroutineScope.() -> Unit) {
            instance.launch(block)
        }

        fun launch(dispatcher: CoroutineDispatcher, block: suspend CoroutineScope.() -> Unit) {
            instance.launch(dispatcher, block)
        }

        fun cancel() {
            instance.cancel()
        }

        fun runOnUiThread(function: () -> Unit) {
            instance.runOnUiThread(function)
        }
    }

}