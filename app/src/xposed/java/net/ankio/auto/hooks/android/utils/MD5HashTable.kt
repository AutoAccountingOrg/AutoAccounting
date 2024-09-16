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

package net.ankio.auto.hooks.android.utils

import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class MD5HashTable(private val expirationTime: Long = 60) {
    // 使用ConcurrentHashMap存储MD5和对应的时间戳
    private val hashTable = ConcurrentHashMap<String, Long>()

    // 获取当前时间戳（以秒为单位）
    private fun getCurrentTime(): Long {
        return System.currentTimeMillis() / 1000
    }

    // 判断时间戳是否过期
    private fun isExpired(timestamp: Long): Boolean {
        return getCurrentTime() - timestamp > expirationTime
    }

    // 生成MD5哈希值
    fun md5(data: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(data.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // 添加数据到哈希表
    fun add(md5: String) {
        hashTable[md5] = getCurrentTime()
    }

    // 判断某个MD5是否存在并未过期
    fun contains(md5: String): Boolean {
        removeExpiredEntries()
        val timestamp = hashTable[md5] ?: return false
        if (isExpired(timestamp)) {
            hashTable.remove(md5)
            return false
        }
        return true
    }

    // 自动删除过期的哈希数据
    fun removeExpiredEntries() {
        val currentTime = getCurrentTime()
        val iterator = hashTable.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (isExpired(entry.value)) {
                iterator.remove()
            }
        }
    }
}