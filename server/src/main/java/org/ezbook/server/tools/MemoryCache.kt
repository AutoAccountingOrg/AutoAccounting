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

package org.ezbook.server.tools

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class MemoryCache {

    private val DEFAULT_DURATION_SECONDS = 30L
    val cacheMap = ConcurrentHashMap<String, CacheItem<*>>()

    private val cleaner = ScheduledThreadPoolExecutor(1).apply {
        scheduleWithFixedDelay(::cleanExpired, 5, 5, TimeUnit.MINUTES)
    }

    fun <T> put(key: String, value: T, durationSeconds: Long = DEFAULT_DURATION_SECONDS) {
        cacheMap[key] = CacheItem(value, nowPlus(durationSeconds))
    }

    inline fun <reified T> get(key: String, deleteAfterRead: Boolean = false): T? {
        val item = cacheMap[key] ?: return null
        return when {
            item.isExpired() -> {
                cacheMap.remove(key); null
            }

            item.value !is T -> null // 类型不匹配
            deleteAfterRead -> cacheMap.remove(key)?.value as T
            else -> item.value as T
        }
    }

    fun clear() = cacheMap.clear()
    fun remove(key: String) = cacheMap.remove(key)
    val size: Int get() = cacheMap.size

    private fun cleanExpired() {
        cacheMap.entries.removeIf { (_, item) -> item.isExpired() }
    }

    private fun nowPlus(seconds: Long): Long =
        System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds)

    data class CacheItem<T>(val value: T, val expireTime: Long) {
        fun isExpired(): Boolean = System.currentTimeMillis() >= expireTime
    }

    fun shutdown() {
        cleaner.shutdown()
    }
}