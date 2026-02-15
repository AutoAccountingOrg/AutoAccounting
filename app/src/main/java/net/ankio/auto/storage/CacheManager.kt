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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * 文件缓存管理器，支持过期时间。
 * 存储于 cacheDir/kv_cache，每 key 一个 .bin 文件，首 13 字节为 ASCII 过期时间戳。
 */
object CacheManager {
    private const val DIR_NAME = "kv_cache"
    private const val SUFFIX = ".bin"
    private const val HEADER_LEN = 13
    private const val FOREVER = "9999999999999"

    /** 永不过期 */
    const val NO_EXPIRE: Long = -1L

    fun putString(key: String, value: String, durationMs: Long) {
        putBytes(key, value.toByteArray(Charsets.UTF_8), durationMs)
    }

    fun getString(key: String): String? =
        getBytes(key)?.let { String(it, Charsets.UTF_8) }

    @Synchronized
    fun putBytes(key: String, bytes: ByteArray, durationMs: Long) {
        val file = fileForKey(key)
        val expireAt =
            if (durationMs == NO_EXPIRE) Long.MAX_VALUE else System.currentTimeMillis() + durationMs
        val header = if (expireAt == Long.MAX_VALUE) FOREVER else "%013d".format(expireAt)
        writeAtomic(file) {
            it.write(header.toByteArray(Charsets.US_ASCII))
            it.write(bytes)
        }
    }

    @Synchronized
    fun getBytes(key: String): ByteArray? {
        val file = fileForKey(key)
        if (!file.exists()) return null
        val expireAt = readExpireAt(file) ?: run {
            file.delete()
            return null
        }
        if (System.currentTimeMillis() >= expireAt) {
            file.delete()
            return null
        }
        return try {
            FileInputStream(file).use { fis ->
                fis.skip(HEADER_LEN.toLong())
                fis.readBytes()
            }
        } catch (e: IOException) {
            Logger.e("读取缓存失败: ${file.name}", e)
            null
        }
    }

    @Synchronized
    fun remove(key: String): Boolean = fileForKey(key).let { it.exists() && it.delete() }

    @Synchronized
    fun clear() {
        dir().listFiles()?.forEach { it.delete() }
    }

    /** 清理过期文件 */
    @Synchronized
    fun cleanup(): Int {
        val files = dir().listFiles() ?: return 0
        var n = 0
        val now = System.currentTimeMillis()
        for (f in files) {
            if (f.isFile && f.name.endsWith(SUFFIX)) {
                val expireAt = readExpireAt(f)
                if (expireAt == null || now >= expireAt) {
                    if (f.delete()) n++
                }
            }
        }
        return n
    }

    fun putStringForever(key: String, value: String) = putString(key, value, NO_EXPIRE)
    fun putBytesForever(key: String, bytes: ByteArray) = putBytes(key, bytes, NO_EXPIRE)

    private fun dir(): File =
        File(autoApp.cacheDir, DIR_NAME).also { if (!it.exists()) it.mkdirs() }

    private fun fileForKey(key: String): File = File(dir(), "${SystemUtils.md5(key)}$SUFFIX")

    private fun readExpireAt(file: File): Long? {
        if (!file.exists() || file.length() < HEADER_LEN) return null
        return try {
            val header = ByteArray(HEADER_LEN)
            FileInputStream(file).use { fis ->
                if (fis.read(header) != HEADER_LEN) return null
            }
            val s = String(header, Charsets.US_ASCII)
            when {
                s == FOREVER -> Long.MAX_VALUE
                s.all { it.isDigit() } -> s.toLongOrNull()
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun writeAtomic(target: File, block: (FileOutputStream) -> Unit) {
        val parent = target.parentFile ?: throw IOException("无父目录")
        if (!parent.exists()) parent.mkdirs()
        val tmp = File(parent, "${target.name}.tmp")
        FileOutputStream(tmp).use { block(it) }
        if (target.exists()) target.delete()
        if (!tmp.renameTo(target)) throw IOException("重命名失败: ${target.name}")
    }
}
