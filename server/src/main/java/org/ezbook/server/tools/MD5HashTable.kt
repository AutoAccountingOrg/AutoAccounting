/*
 * Copyright 2025 ankio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ezbook.server.tools

import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * 线程安全的内存去重缓存，基于 MD5 哈希和 TTL 机制。
 *
 * 设计特点：
 * - 使用 MD5 哈希作为键，节省内存并提供固定长度的键
 * - 基于时间戳的 TTL 过期机制，支持滑动过期
 * - 线程安全，使用 ConcurrentHashMap 实现
 * - 性能优化的清理策略，避免频繁全表扫描
 *
 * 适用场景：
 * - 防重复处理（如消息去重、请求去重）
 * - 临时缓存需要自动过期的场景
 * - 高并发环境下的内存缓存
 *
 * @param ttlMs 单条记录生存时间（毫秒），默认60秒
 * @param clock 时间源函数，默认使用 System.currentTimeMillis()，便于单元测试注入
 */
class MD5HashTable(
    private val ttlMs: Long = 60_000,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    private val map = ConcurrentHashMap<String, Long>()

    // 清理计数器，避免每次 put() 都执行全表扫描
    @Volatile
    private var putCount = 0
    private val cleanupThreshold = 100 // 每100次 put() 操作执行一次清理

    /**
     * 添加哈希键到缓存中。
     * 如果键已存在，则更新其时间戳（滑动过期机制）。
     *
     * 性能优化：
     * - 不是每次都执行全表清理，而是每100次操作清理一次
     * - 使用 MD5 哈希避免原始字符串的内存占用
     *
     * @param key 原始键值，将被转换为 MD5 哈希存储
     */
    fun put(key: String) {
        // 优化清理策略：不是每次都清理，而是按计数器清理
        if (++putCount % cleanupThreshold == 0) {
            cleanup()
        }
        val k = md5(key)
        map[k] = clock()
    }

    /**
     * 判断键是否存在且未过期。
     * 过期条目会被自动删除并返回 false。
     *
     * @param key 原始键值
     * @return true 如果键存在且未过期，否则返回 false
     */
    fun contains(key: String): Boolean {
        val k = md5(key)
        val ts = map[k] ?: return false
        val isValid = clock() - ts <= ttlMs
        if (!isValid) {
            map.remove(k, ts) // 原子性删除，避免并发问题
        }
        return isValid
    }

    /**
     * 主动清理过期条目。
     *
     * 实现说明：
     * - 使用 ConcurrentHashMap.entries.removeIf() 保证线程安全
     * - 在迭代过程中安全删除过期条目
     * - 被 put() 方法按计数器策略调用，避免频繁全表扫描
     *
     * 时间复杂度：O(n)，其中 n 是当前缓存条目数量
     */
    private fun cleanup() {
        val now = clock()
        map.entries.removeIf { (_, timestamp) ->
            now - timestamp > ttlMs
        }
    }


    companion object {
        // 线程本地的 MD5 实例，避免重复创建和线程竞争
        private val threadLocalMD5 = ThreadLocal.withInitial {
            MessageDigest.getInstance("MD5")
        }

        /**
         * 计算字符串的 MD5 哈希值
         * 使用线程本地存储优化性能，避免重复创建 MessageDigest 实例
         *
         * @param input 输入字符串
         * @return 32位小写十六进制 MD5 哈希值
         */
        fun md5(input: String): String {
            val md = threadLocalMD5.get()
            md?.reset() // 重置状态，确保计算正确性
            return md?.digest(input.toByteArray())
                ?.joinToString("") { "%02x".format(it) } ?: ""
        }
    }
}
