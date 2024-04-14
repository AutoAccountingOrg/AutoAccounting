package net.ankio.auto.utils.event

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object EventBus {
    private val listenersMap = ConcurrentHashMap<Class<*>, CopyOnWriteArrayList<(Event) -> Unit>>()

    // 注册监听器
    fun <T : Event> register(
        eventType: Class<T>,
        listener: (T) -> Unit,
    ) {
        val listeners = listenersMap.getOrPut(eventType) { CopyOnWriteArrayList() }
        @Suppress("UNCHECKED_CAST")
        listeners.add(listener as (Event) -> Unit)
    }

    // 注销监听器
    fun <T : Event> unregister(
        eventType: Class<T>,
        listener: (T) -> Unit,
    ) {
        listenersMap[eventType]?.remove(listener)
    }

    // 发布事件
    fun post(event: Event) {
        listenersMap[event::class.java]?.forEach { listener ->
            listener(event)
        }
    }
}
