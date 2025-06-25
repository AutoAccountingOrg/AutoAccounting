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

import java.util.concurrent.atomic.AtomicLong

class Throttle(
    private val intervalMs: Long = 300,
    private val block: () -> Unit
) : Runnable {
    private val lastTime = AtomicLong(0)

    override fun run() {
        val now = System.currentTimeMillis()
        val prev = lastTime.get()
        if (now - prev >= intervalMs) {
            if (lastTime.compareAndSet(prev, now)) {
                block()
            }
        }
    }

    /**
     * 主动手动调用（更符合 throttle.run { ... } 习惯）
     */
    fun runThrottle() = run()

    companion object {
        /**
         * 兼容你最早的函数式写法：返回一个可调用的函数（函数式 API）
         */
        fun asFunction(intervalMs: Long = 300, block: () -> Unit): () -> Unit {
            val throttle = Throttle(intervalMs, block)
            return { throttle.run() }
        }

        fun asFunction(block: () -> Unit): () -> Unit = asFunction(300, block)
    }
}
