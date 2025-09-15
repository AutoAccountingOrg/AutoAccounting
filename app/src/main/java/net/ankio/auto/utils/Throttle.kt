package net.ankio.auto.utils

import net.ankio.auto.storage.CacheManager
import java.util.concurrent.atomic.AtomicLong

/**
 * 节流器 - 限制函数调用频率，防止短时间内重复执行
 * 支持内存模式和持久化模式两种工作方式
 *
 * @param T 参数类型
 * @param intervalMs 节流间隔时间（毫秒）
 * @param persistKey 持久化存储的键名，为 null 时使用内存模式
 * @param block 要执行的函数块
 */
class Throttle<T>(
    private val intervalMs: Long = 300,
    private val persistKey: String? = null,
    private val block: (T) -> Unit
) {
    /** 内存中的 lastTime - 内存模式使用；持久化模式会从缓存初始化 */
    private val lastTime = AtomicLong(getPersistedLastTime())

    /**
     * 从持久化存储中读取上次执行时间
     * @return 上次执行时间戳，持久化模式下从 CacheManager 读取，内存模式返回 0
     */
    private fun getPersistedLastTime(): Long {
        return if (persistKey != null) {
            val key = "throttle:$persistKey"
            val value = CacheManager.getString(key)
            value?.toLongOrNull() ?: 0L
        } else {
            0L
        }
    }

    /**
     * 将执行时间持久化存储
     * @param time 要存储的时间戳
     */
    private fun persistLastTime(time: Long) {
        if (persistKey != null) {
            val key = "throttle:$persistKey"
            CacheManager.putStringForever(key, time.toString())
        }
    }

    /**
     * 执行节流控制的函数调用
     * 只有当距离上次执行时间超过设定间隔时才会执行
     *
     * @param param 传递给执行函数的参数
     */
    fun run(param: T) {
        val now = System.currentTimeMillis()
        val prev = lastTime.get()
        if (now - prev >= intervalMs) {
            if (lastTime.compareAndSet(prev, now)) {
                // 持久化模式下同步更新 SharedPreferences
                persistLastTime(now)
                block(param)
            }
        }
    }

    /**
     * 兼容不带参数的调用习惯（T=Unit）
     */
    @Suppress("UNCHECKED_CAST")
    fun run() = run(Unit as T)

    companion object {
        /**
         * 支持带参数的函数式写法（内存模式）
         * @param intervalMs 节流间隔时间（毫秒）
         * @param block 要执行的函数块
         * @return 节流控制的函数
         */
        fun <T> asFunction(intervalMs: Long = 300, block: (T) -> Unit): (T) -> Unit {
            val throttle = Throttle(intervalMs, null, block)
            return { param -> throttle.run(param) }
        }

        /**
         * 支持带参数的函数式写法（持久化模式）
         * @param intervalMs 节流间隔时间（毫秒）
         * @param persistKey 持久化存储的键名
         * @param block 要执行的函数块
         * @return 节流控制的函数
         */
        fun <T> asFunction(
            intervalMs: Long = 300,
            persistKey: String,
            block: (T) -> Unit
        ): (T) -> Unit {
            val throttle = Throttle(intervalMs, persistKey, block)
            return { param -> throttle.run(param) }
        }

        /**
         * 支持无参数写法（内存模式）
         * @param intervalMs 节流间隔时间（毫秒）
         * @param block 要执行的函数块
         * @return 节流控制的函数
         */
        fun asFunction(intervalMs: Long = 300, block: () -> Unit): () -> Unit {
            val throttle = Throttle(intervalMs, null) { _: Unit -> block() }
            return { throttle.run(Unit) }
        }

        /**
         * 支持无参数写法（持久化模式）
         * @param intervalMs 节流间隔时间（毫秒）
         * @param persistKey 持久化存储的键名
         * @param block 要执行的函数块
         * @return 节流控制的函数
         */
        fun asFunction(intervalMs: Long = 300, persistKey: String, block: () -> Unit): () -> Unit {
            val throttle = Throttle(intervalMs, persistKey) { _: Unit -> block() }
            return { throttle.run(Unit) }
        }

        /**
         * 默认参数的无参数写法（内存模式）
         * @param block 要执行的函数块
         * @return 节流控制的函数
         */
        fun asFunction(block: () -> Unit): () -> Unit = asFunction(300, block)
    }
}
