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

package net.ankio.auto.http

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.BuildConfig
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.PrefManager
import java.io.File
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class LicenseNetwork {
    private val client by lazy {
        RequestsUtils()
    }

    init {
        if (PrefManager.localID.isEmpty()) {
            PrefManager.localID = UUID.randomUUID().toString()
        }
        client.addHeader("X-Device-Board", Build.BOARD)
        client.addHeader("X-Device-Hardware", Build.HARDWARE)
        client.addHeader("X-Device-Model", Build.MODEL)
        client.addHeader("X-Device-Manufacturer", Build.MANUFACTURER)
        client.addHeader("X-Device-Local-Id", PrefManager.localID)
    }


    fun hmacSha256(data: String, key: String): String {
        val algorithm = "HmacSHA256"
        val mac = Mac.getInstance(algorithm)
        val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), algorithm)
        mac.init(secretKeySpec)
        val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        // 转成十六进制字符串
        return hash.joinToString("") { "%02x".format(it) }
    }

    // 生成签名（返回signature和timestamp）
    fun generateSignature(postData: Map<String, String>): Pair<String, String> {
        // 1. 当前时间戳（秒）
        val timestamp = (System.currentTimeMillis() / 1000).toString()

        // 2. 合并数据
        val data = mutableMapOf<String, String>()
        data["device"] = PrefManager.localID
        data.putAll(postData)

        // 3. 字典序排序
        val sortedData = data.toSortedMap()

        // 4. 拼接字符串
        val signString =
            sortedData.entries.joinToString("&") { "${it.key}=${it.value}" } + "&timestamp=$timestamp"

        // 5. 生成签名
        val signature = hmacSha256(signString, PrefManager.localID)

        return Pair(signature, timestamp)
    }

    private val url =
        if (BuildConfig.DEBUG) "https://license.ankio.icu" else "https://license.ez-book.org"

    suspend fun post(
        path: String,
        postData: HashMap<String, String>,
    ): String = withContext(Dispatchers.IO) {
        val uri = "$url/${path.trimStart('/')}"
        val (signature, timestamp) = generateSignature(postData)
        client.addHeader("X-Token", PrefManager.token)
        client.addHeader("X-Signature", signature)
        client.addHeader("X-Timestamp", timestamp)

        try {
            return@withContext client.form(uri, postData).second
        } catch (e: Exception) {
            Logger.e("请求失败：$e", e)
            return@withContext ""
        }
    }


    suspend fun get(
        path: String,
        queryData: HashMap<String, String> = hashMapOf(),
    ): String = withContext(Dispatchers.IO) {
        val uri = "$url/${path.trimStart('/')}"
        val (signature, timestamp) = generateSignature(queryData)
        client.addHeader("X-Token", PrefManager.token)
        client.addHeader("X-Signature", signature)
        client.addHeader("X-Timestamp", timestamp)

        try {
            return@withContext client.get(uri, queryData).second
        } catch (e: Exception) {
            Logger.e("请求失败：$e", e)
            return@withContext ""
        }
    }


    suspend fun download(
        path: String,
        file: File,
        queryData: HashMap<String, String> = hashMapOf(),

        ): Boolean = withContext(Dispatchers.IO) {
        val uri =
            "$url/${path.trimStart('/')}?" + queryData.entries.joinToString("&") { "${it.key}=${it.value}" }
        val (signature, timestamp) = generateSignature(queryData)
        client.addHeader("X-Token", PrefManager.token)
        client.addHeader("X-Signature", signature)
        client.addHeader("X-Timestamp", timestamp)
        try {
            return@withContext client.download(uri, file)
        } catch (e: Exception) {
            Logger.e("请求失败：$e", e)
            return@withContext false
        }
    }


}