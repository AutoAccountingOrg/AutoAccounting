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

package net.ankio.auto.storage

import android.content.Context
import net.ankio.auto.autoApp
import net.ankio.auto.utils.SystemUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.FileInputStream
import java.util.Locale
import kotlin.random.Random

/**
 * 简单的文件缓存管理器（支持自动过期）。
 *
 * 设计原则：
 * 1. 使用应用 `cacheDir` 下的私有目录存储缓存文件，进程崩溃不影响下次读取
 * 2. 每个缓存键仅一个数据文件（.bin），文件首13字节为ASCII过期时间戳
 * 3. 读取时依据头部时间判断过期；若文件损坏或头部非法则直接删除
 * 4. 接口保持精简：字符串与字节两类常用读写 + 主动清理/删除
 * 5. 不引入复杂策略（LRU/容量限制），坚持简单可预期；若未来需要可在不破坏接口前提下扩展
 */
object CacheManager {
    /** 缓存目录名 */
    private const val DIR_NAME = "kv_cache"

    /** 数据文件后缀（新格式：首13字节写入过期时间戳ASCII） */
    private const val SUFFIX_DATA = ".bin"


    /** 永不过期标记：对外仍使用 -1；内部新格式写入固定13位最大时间 */
    const val NO_EXPIRE: Long = -1L

    /** 新格式头部长度（ASCII 13位毫秒时间戳） */
    private const val HEADER_LEN = 13

    /** 新格式中“永不过期”的写入值：13位9填充，便于简单字符串比较与解析 */
    private const val HEADER_FOREVER = "9999999999999"

    /**
     * 将字符串写入缓存。
     * @param key 业务键，内部会以 md5(key) 映射到文件名
     * @param value 字符串内容
     * @param durationMs 过期时长（毫秒）。传入 [NO_EXPIRE] 表示永不过期
     */
    fun putString(key: String, value: String, durationMs: Long) {
        putBytes(key, value.toByteArray(Charsets.UTF_8), durationMs)
    }

    /**
     * 从缓存读取字符串。
     * @param key 业务键
     * @return 字符串，若不存在或已过期返回 null
     */
    fun getString(key: String): String? {
        return getBytes(key)?.toString(Charsets.UTF_8)
    }

    /**
     * 将字节数组写入缓存。
     * @param key 业务键
     * @param bytes 数据
     * @param durationMs 过期时长（毫秒）。传入 [NO_EXPIRE] 表示永不过期
     */
    @Synchronized
    fun putBytes(key: String, bytes: ByteArray, durationMs: Long) {
        maybeCleanup()
        val context = autoApp
        val keyHash = SystemUtils.md5(key)
        val dir = ensureDir(context)
        val dataFile = File(dir, "$keyHash$SUFFIX_DATA")

        try {
            // 计算过期时间，并以新格式写入：首13字节为ASCII过期时间戳
            val now = System.currentTimeMillis()
            val expireAt = if (durationMs == NO_EXPIRE) Long.MAX_VALUE else now + durationMs
            writeDataWithHeader(dataFile, bytes, expireAt)
        } catch (e: Exception) {
            Logger.e("写入缓存失败: ${dataFile.name}", e)
        }
    }

    /**
     * 从缓存读取字节数组。
     * 读取时会自动进行过期校验，过期则删除并返回 null。
     * @param key 业务键
     */
    @Synchronized
    fun getBytes(key: String): ByteArray? {
        maybeCleanup()
        val context = autoApp
        val keyHash = SystemUtils.md5(key)
        val dir = ensureDir(context)
        val dataFile = File(dir, "$keyHash$SUFFIX_DATA")

        if (!dataFile.exists()) {
            // 数据文件缺失
            return null
        }

        // 读取新格式头部
        val headerExpireAt = readHeaderExpireAt(dataFile)
        if (headerExpireAt == null) {
            // 头部非法/文件损坏
            safeDelete(dataFile)
            return null
        }
        val now = System.currentTimeMillis()
        if (now >= headerExpireAt) {
            safeDelete(dataFile)
            return null
        }
        return try {
            readDataPayload(dataFile)
        } catch (e: IOException) {
            Logger.e("读取缓存失败: ${dataFile.name}", e)
            null
        }
    }

    /**
     * 删除指定键的缓存。
     * @return 是否删除了任何文件
     */
    @Synchronized
    fun remove(key: String): Boolean {
        val context = autoApp
        val keyHash = SystemUtils.md5(key)
        val dir = ensureDir(context)
        val dataFile = File(dir, "$keyHash$SUFFIX_DATA")
        var removed = false
        if (dataFile.exists()) removed = dataFile.delete() || removed
        return removed
    }

    /**
     * 清空所有缓存文件。
     */
    @Synchronized
    fun clear() {
        val dir = ensureDir(autoApp)
        dir.listFiles()?.forEach { file ->
            safeDelete(file)
        }
    }

    /**
     * 主动清理过期的缓存文件。
     * @return 被删除的文件数量（包含数据与元数据文件）
     */
    @Synchronized
    fun cleanup(): Int {
        val dir = ensureDir(autoApp)
        val files = dir.listFiles() ?: return 0
        var deleteCount = 0

        // 仅处理 .bin：无头部或已过期则清除
        files.filter { it.isFile && it.name.endsWith(SUFFIX_DATA) }.forEach { dataFile ->
            val headerExpireAt = readHeaderExpireAt(dataFile)
            val shouldDelete =
                headerExpireAt == null || System.currentTimeMillis() >= headerExpireAt
            if (shouldDelete) {
                if (safeDelete(dataFile)) deleteCount++
            }
        }
        return deleteCount
    }

    /**
     * 判断键是否已过期（不存在视为过期）。
     */
    @Synchronized
    fun isExpired(key: String): Boolean {
        val context = autoApp
        val keyHash = SystemUtils.md5(key)
        val dir = ensureDir(context)
        val dataFile = File(dir, "$keyHash$SUFFIX_DATA")
        if (!dataFile.exists()) return true

        val headerExpireAt = readHeaderExpireAt(dataFile) ?: return true
        return System.currentTimeMillis() >= headerExpireAt
    }

    /**
     * 永不过期的便捷写入（字符串）。
     */
    fun putStringForever(key: String, value: String) = putString(key, value, NO_EXPIRE)

    /**
     * 永不过期的便捷写入（字节）。
     */
    fun putBytesForever(key: String, bytes: ByteArray) = putBytes(key, bytes, NO_EXPIRE)

    // ----------------------------- 内部工具方法 ----------------------------- //

    /**
     * 确保缓存目录存在
     */
    private fun ensureDir(context: Context): File {
        val dir = File(context.cacheDir, DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 安全删除文件，异常时仅记录日志
     */
    private fun safeDelete(file: File): Boolean {
        return try {
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            Logger.e("删除缓存文件失败: ${file.name}", e)
            false
        }
    }

    /**
     * 概率清理：在读写操作时以小概率触发一次清理，避免频繁遍历文件。
     * 当前概率：1/100。保持简单，足以在常用路径下逐步清除过期项。
     */
    private fun maybeCleanup() {
        if (Random.nextInt(100) == 0) {
            try {
                val removed = cleanup()
                if (removed > 0) Logger.d("概率清理完成，删除文件数：$removed")
            } catch (e: Exception) {
                Logger.e("概率清理失败", e)
            }
        }
    }

    // ----------------------------- 新格式（首13字节头）实现 ----------------------------- //

    /**
     * 将数据按新格式写入：首13字节为ASCII时间戳，之后为业务数据。
     * 写入采用同目录临时文件 + rename 以尽量保证原子性。
     */
    private fun writeDataWithHeader(target: File, payload: ByteArray, expireAtMillis: Long) {
        val dir = target.parentFile ?: throw IOException("无有效父目录: ${target.path}")
        if (!dir.exists()) dir.mkdirs()
        val header = when (expireAtMillis) {
            Long.MAX_VALUE -> HEADER_FOREVER
            else -> format13Digits(expireAtMillis)
        }
        val tmp = File(dir, target.name + ".tmp")
        FileOutputStream(tmp).use { fos ->
            fos.write(header.toByteArray(Charsets.US_ASCII))
            fos.write(payload)
            try {
                fos.fd.sync()
            } catch (_: Exception) {
                // 某些设备可能不支持，忽略
            }
        }
        if (target.exists()) {
            if (!tmp.renameTo(target)) {
                // 尝试删除旧文件后再重命名
                safeDelete(target)
                if (!tmp.renameTo(target)) {
                    // 失败则抛出异常
                    throw IOException("重命名缓存文件失败: ${target.name}")
                }
            }
        } else {
            if (!tmp.renameTo(target)) {
                throw IOException("创建缓存文件失败: ${target.name}")
            }
        }
    }

    /** 读取新格式头部的过期时间；若不是新格式或头部非法，返回 null */
    private fun readHeaderExpireAt(file: File): Long? {
        if (!file.exists() || file.length() < HEADER_LEN) return null
        return try {
            val bytes = ByteArray(HEADER_LEN)
            FileInputStream(file).use { fis ->
                val read = fis.read(bytes)
                if (read != HEADER_LEN) return null
            }
            val header = String(bytes, Charsets.US_ASCII)
            if (header == HEADER_FOREVER) return Long.MAX_VALUE
            if (header.all { it.isDigit() }) header.toLongOrNull() else null
        } catch (_: Exception) {
            null
        }
    }

    /** 读取新格式数据部分（去除头部13字节） */
    private fun readDataPayload(file: File): ByteArray {
        val total = file.length().toInt()
        if (total <= HEADER_LEN) return ByteArray(0)
        val payload = ByteArray(total - HEADER_LEN)
        FileInputStream(file).use { fis ->
            // 跳过头部
            val skipped = fis.skip(HEADER_LEN.toLong())
            if (skipped < HEADER_LEN) throw IOException("跳过头部失败: ${file.name}")
            var offset = 0
            while (offset < payload.size) {
                val read = fis.read(payload, offset, payload.size - offset)
                if (read <= 0) break
                offset += read
            }
            if (offset != payload.size) throw IOException("读取数据不完整: ${file.name}")
        }
        return payload
    }

    /** 将 long 格式化为固定13位的ASCII数字串（左侧补0） */
    private fun format13Digits(value: Long): String {
        val safe =
            if (value < 0) 0L else if (value > 9_999_999_999_999L) 9_999_999_999_999L else value
        return String.format(Locale.US, "%013d", safe)
    }


}