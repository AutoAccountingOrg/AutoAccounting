package net.ankio.auto.utils.event

import android.os.Handler
import android.os.Looper
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object EventBus {
    private val listenersMap = ConcurrentHashMap<Class<*>, CopyOnWriteArrayList<(Event) -> Boolean>>()

    // 注册监听器
    fun <T : Event> register(
        eventType: Class<T>,
        listener: (T) -> Boolean,
    ) {
        val listeners = listenersMap.getOrPut(eventType) { CopyOnWriteArrayList() }
        @Suppress("UNCHECKED_CAST")
        listeners.add(listener as (Event) -> Boolean)
    }

    // 注销监听器
    fun <T : Event> unregister(
        eventType: Class<T>,
        listener: (T) -> Boolean,
    ) {
        listenersMap[eventType]?.remove(listener)
    }

    // 发布事件
    fun post(event: Event) {
        listenersMap[event::class.java]?.forEach { listener ->
            runOnUiThread {
                if(listener(event)){
                    unregister(event::class.java, listener as (Event) -> Boolean)
                }
            }
        }
    }

    private fun runOnUiThread(block: () -> Unit) {
        if (Thread.currentThread() == Looper.getMainLooper().thread) {
            block()
        } else {
            Handler(Looper.getMainLooper()).post(block)
        }
    }
}
