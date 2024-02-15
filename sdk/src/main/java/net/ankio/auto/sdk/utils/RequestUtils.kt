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
import java.net.HttpURLConnection
import java.net.URL


object RequestUtils {


    suspend fun post(
        url: String,
        query: HashMap<String, String>? = null,
        data: String? = null,
        headers: HashMap<String, String> = HashMap(),
    ) :PostResult = withContext(Dispatchers.IO){
        var requestUrl = url
        if (!query.isNullOrEmpty()) {
            requestUrl += query.entries.joinToString("&", prefix = "?") { "${it.key}=${it.value}" }
        }
        val urlObj = URL(requestUrl)
        val con =  urlObj.openConnection() as HttpURLConnection
        con.requestMethod = "POST" //设置请求方法POST
        con.connectTimeout = 30000
        con.readTimeout = 30000
        con.doOutput = true
        con.doInput = true
        headers.forEach { (key, value) ->
            con.setRequestProperty(key, value)
        }
        val outputStream = con.outputStream
        if(!data.isNullOrEmpty()){
            outputStream.write(data.toByteArray())
            outputStream.close()
        }

      try {
          val responseCode = con.responseCode
          val response = con.inputStream.use { it.readBytes() }
          PostResult(response, responseCode == HttpURLConnection.HTTP_OK)
      } catch (e: Exception) {
          Logger.i("请求异常：${e.message}",e)
          PostResult(null, false)
      }
    }

}
