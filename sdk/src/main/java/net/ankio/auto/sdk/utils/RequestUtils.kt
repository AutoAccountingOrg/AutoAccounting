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

package net.ankio.auto.sdk.utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
data class PostResult(val byteArray: ByteArray?, val isSuccess: Boolean)

object RequestUtils {

    suspend fun post(
        url: String,
        query: HashMap<String, String>? = null,
        data: String? = null,
        headers: HashMap<String, String> = HashMap(),
    ): PostResult{
        try {
            var requestUrl = url
            if (!query.isNullOrEmpty()) {
                requestUrl += query.entries.joinToString("&", prefix = "?") { "${it.key}=${it.value}" }
            }

            val urlObj = URL(requestUrl)
            val conn = withContext(Dispatchers.IO) {
                urlObj.openConnection()
            } as HttpURLConnection
            conn.requestMethod = "POST"

            headers.forEach { (key, value) ->
                conn.setRequestProperty(key, value)
            }

            conn.doOutput = true

            DataOutputStream(conn.outputStream).use { dos ->
                dos.writeBytes(data ?: "")
                dos.flush()
            }

            val responseCode = conn.responseCode
            val response = conn.inputStream.use { it.readBytes() }
            return PostResult(response, responseCode == HttpURLConnection.HTTP_OK)
        } catch (e: Exception) {
           Logger.i("请求异常：${e.message}")
           return PostResult(null, false)
        }
    }
}
