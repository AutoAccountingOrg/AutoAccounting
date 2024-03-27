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

package net.ankio.auto.utils.event

object EventBus {
    private val listenersMap = mutableMapOf<Class<*>, MutableList<(Event) -> Unit>>()

    // 注册监听器
    fun <T : Event> register(eventType: Class<T>, listener: (T) -> Unit) {
        val listeners = listenersMap.getOrPut(eventType) { mutableListOf() }
        @Suppress("UNCHECKED_CAST")
        listeners.add(listener as (Event) -> Unit)
    }

    // 注销监听器
    fun <T : Event> unregister(eventType: Class<T>, listener: (T) -> Unit) {
        listenersMap[eventType]?.remove(listener)
    }

    // 发布事件
    fun post(event: Event) {
        listenersMap[event::class.java]?.forEach { listener ->
            listener(event)
        }
    }
}