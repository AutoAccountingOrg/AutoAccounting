package net.ankio.auto.utils

import java.util.concurrent.atomic.AtomicLong

class Throttle<T>(
    private val intervalMs: Long = 300,
    private val block: (T) -> Unit
) {
    private val lastTime = AtomicLong(0)

    fun run(param: T) {
        val now = System.currentTimeMillis()
        val prev = lastTime.get()
        if (now - prev >= intervalMs) {
            if (lastTime.compareAndSet(prev, now)) {
                block(param)
            }
        }
    }

    /**
     * 兼容不带参数的调用习惯（T=Unit）
     */
    fun run() = run(Unit as T)

    companion object {
        /**
         * 支持带参数的函数式写法
         */
        fun <T> asFunction(intervalMs: Long = 300, block: (T) -> Unit): (T) -> Unit {
            val throttle = Throttle(intervalMs, block)
            return { param -> throttle.run(param) }
        }

        /**
         * 支持无参数写法
         */
        fun asFunction(intervalMs: Long = 300, block: () -> Unit): () -> Unit {
            val throttle = Throttle(intervalMs) { _: Unit -> block() }
            return { throttle.run(Unit) }
        }

        fun asFunction(block: () -> Unit): () -> Unit = asFunction(300, block)
    }
}
