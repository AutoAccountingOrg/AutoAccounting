package net.ankio.auto.utils

import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicReference

/**
 * 防抖器 - 多次调用时，仅在最后一次调用后 [delayMs] 执行，保证最终一定会执行
 * 与 [Throttle] 区别：Throttle 可能跳过执行；Debounce 保证最后一次调用对应的逻辑会执行
 *
 * @param T 参数类型
 * @param delayMs 延迟执行时间（毫秒）
 * @param block 要执行的函数块
 */
class Debounce<T>(
    private val delayMs: Long = 300,
    private val block: (T) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private val pendingParam = AtomicReference<T?>(null)
    private val runnable = Runnable {
        pendingParam.getAndSet(null)?.let { param -> block(param) }
    }

    /**
     * 触发执行：取消之前的调度，重新延迟 [delayMs] 后执行
     * 最后一次调用后一定会执行
     */
    fun run(param: T) {
        pendingParam.set(param)
        handler.removeCallbacks(runnable)
        handler.postDelayed(runnable, delayMs)
    }

    @Suppress("UNCHECKED_CAST")
    fun run() = run(Unit as T)

    /** 取消待执行的调用 */
    fun cancel() {
        handler.removeCallbacks(runnable)
        pendingParam.set(null)
    }

    companion object {
        fun <T> asFunction(delayMs: Long = 300, block: (T) -> Unit): (T) -> Unit {
            val debounce = Debounce(delayMs, block)
            return { param -> debounce.run(param) }
        }

        fun asFunction(delayMs: Long = 300, block: () -> Unit): () -> Unit {
            val debounce = Debounce(delayMs) { _: Unit -> block() }
            return { debounce.run(Unit) }
        }

        fun asFunction(block: () -> Unit): () -> Unit = asFunction(300, block)
    }
}
