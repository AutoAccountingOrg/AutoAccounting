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

import java.util.concurrent.TimeUnit

class MemoryCache {

    private val DEFAULT_DURATION_SECONDS = 30L
    private val MAX_SIZE = 20

    // 使用 LinkedHashMap 实现 LRU，accessOrder = true 表示按访问顺序排序
    val cacheMap = object : LinkedHashMap<String, CacheItem<*>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheItem<*>>?): Boolean {
            return size > MAX_SIZE
        }
    }

    @Synchronized
    fun <T> put(key: String, value: T, durationSeconds: Long = DEFAULT_DURATION_SECONDS) {
        cacheMap[key] = CacheItem(value, nowPlus(durationSeconds))
    }

    @Synchronized
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

    @Synchronized
    fun clear() = cacheMap.clear()

    @Synchronized
    fun remove(key: String) = cacheMap.remove(key)

    @get:Synchronized
    val size: Int get() = cacheMap.size

    private fun nowPlus(seconds: Long): Long =
        System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds)

    data class CacheItem<T>(val value: T, val expireTime: Long) {
        fun isExpired(): Boolean = System.currentTimeMillis() >= expireTime
    }
}