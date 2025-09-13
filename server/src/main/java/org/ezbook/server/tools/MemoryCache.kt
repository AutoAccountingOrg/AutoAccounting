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

/**
 * 简单的内存缓存实现，支持LRU淘汰和过期时间
 * 线程安全，使用synchronized保证并发访问正确性
 */
class MemoryCache {

    companion object {
        private const val DEFAULT_DURATION_SECONDS = 30L
        private const val MAX_SIZE = 20

        // 使用object声明替代复杂的双重检查单例
        val instance: MemoryCache by lazy { MemoryCache() }

        /**
         * 便捷方法：写入缓存（通过伴生对象转发到实例）
         * @param key 缓存键
         * @param value 缓存值
         * @param durationSeconds 过期时间（秒），默认30秒
         */
        fun put(key: String, value: Any, durationSeconds: Long = DEFAULT_DURATION_SECONDS) {
            instance.put(key, value, durationSeconds)
        }

        /**
         * 便捷方法：读取缓存（通过伴生对象转发到实例）
         * @param key 缓存键
         * @return 缓存值，如果不存在或已过期返回null
         */
        fun get(key: String): Any? = instance.get(key)
    }

    // LRU缓存实现，accessOrder=true按访问顺序排序
    private val cache = object : LinkedHashMap<String, CacheItem>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheItem>?) =
            size > MAX_SIZE
    }

    /**
     * 存储数据到缓存
     * @param key 缓存键
     * @param value 缓存值
     * @param durationSeconds 过期时间（秒），默认30秒
     */
    @Synchronized
    fun put(key: String, value: Any, durationSeconds: Long = DEFAULT_DURATION_SECONDS) {
        cache[key] = CacheItem(
            value,
            System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(durationSeconds)
        )
    }

    /**
     * 从缓存获取数据
     * @param key 缓存键
     * @return 缓存值，如果不存在或已过期返回null
     */
    @Synchronized
    fun get(key: String): Any? {
        val item = cache[key] ?: return null

        // 检查是否过期，过期则删除并返回null
        if (System.currentTimeMillis() >= item.expireTime) {
            cache.remove(key)
            return null
        }

        return item.value
    }

    /**
     * 从缓存获取数据并删除
     * @param key 缓存键
     * @return 缓存值，如果不存在或已过期返回null
     */
    @Synchronized
    fun pop(key: String): Any? {
        val value = get(key)
        if (value != null) cache.remove(key)
        return value
    }

    /**
     * 删除指定缓存项
     */
    @Synchronized
    fun remove(key: String) = cache.remove(key)

    /**
     * 清空所有缓存
     */
    @Synchronized
    fun clear() = cache.clear()

    /**
     * 获取当前缓存大小
     */
    @get:Synchronized
    val size: Int get() = cache.size

    /**
     * 缓存项数据类
     */
    data class CacheItem(val value: Any, val expireTime: Long)
}