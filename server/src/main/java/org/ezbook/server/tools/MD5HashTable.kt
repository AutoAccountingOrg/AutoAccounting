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
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * 线程安全的内存去重缓存。
 *
 * @param ttlMs            单条记录生存时间（毫秒）
 * @param clock            时间源，默认 System.currentTimeMillis()，方便单元测试注入
 */
class MD5HashTable(
    private val ttlMs: Long = 60_000,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    private val map = ConcurrentHashMap<String, Long>()

    /**
     * 添加哈希键；若已存在则更新时间戳（滑动过期）。
     * 调用期间会进行一次惰性删除。
     */
    fun put(key: String) {
        cleanup()
        val k = md5(key)
        map[k] = clock()
    }

    /**
     * 判断键是否存在且未过期。
     * 过期会顺带删除并返回 false。
     */
    fun contains(key: String): Boolean {
        val k = md5(key)
        val ts = map[k] ?: return false
        return if (clock() - ts <= ttlMs) {
            true
        } else {
            map.remove(k, ts)
            false
        }
    }

    /**
     * 主动清理过期条目。
     * 可被外部手动触发，也被定时器/惰性策略复用。
     */
    private fun cleanup() {
        val now = clock()
        map.entries.removeIf { now - it.value > ttlMs }
    }


    companion object {
        fun md5(input: String): String = digest("MD5", input)

        private fun digest(alg: String, data: String): String =
            MessageDigest.getInstance(alg)
                .digest(data.toByteArray())
                .joinToString("") { "%02x".format(it) }
    }
}
