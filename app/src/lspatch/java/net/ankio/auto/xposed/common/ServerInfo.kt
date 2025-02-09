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

package net.ankio.auto.xposed.common

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.exceptions.ServiceCheckException
import net.ankio.auto.storage.Logger
import net.ankio.lspatch.services.NotificationService
import net.ankio.lspatch.services.SmsReceiver
import org.ezbook.server.Server


object ServerInfo {

    suspend fun isServerStart(context: Context) = withContext(Dispatchers.IO) {
        checkServer(context)
        SmsReceiver.checkPermission(context)
        NotificationService.checkPermission()
    }

    private suspend fun checkServer(context: Context) = withContext(Dispatchers.IO) {
        val maxAttempts = 3  // 最大重试次数
        val delayBetweenAttempts = 500L  // 每次重试间隔3秒

        repeat(maxAttempts) { attempt ->
            try {
                val data = Server.request("/", "")
                if (data != null) {
                    val json = Gson().fromJson(data, JsonObject::class.java)
                    if (json.get("data").asString != BuildConfig.VERSION_NAME) {
                        throw ServiceCheckException(
                            context.getString(R.string.server_error_version_title),
                            context.getString(
                                R.string.server_error_version,
                                json.get("data").asString,
                                BuildConfig.VERSION_NAME
                            ),
                            context.getString(R.string.server_error_btn),
                            action = { activity ->

                            }
                        )
                    }
                    return@withContext  // 成功检查，直接返回
                }
            } catch (e: ServiceCheckException) {
                throw e  // 版本不匹配异常直接抛出
            } catch (e: Exception) {
                if (attempt == maxAttempts - 1) {
                    Logger.e("服务检查失败", e)
                }
            }

            if (attempt < maxAttempts - 1) {
                delay(delayBetweenAttempts)
            }
        }

        // 所有重试都失败后抛出异常
        throw ServiceCheckException(
            context.getString(R.string.server_error_title),
            context.getString(R.string.server_error),
            context.getString(R.string.server_error_btn),
            action = { activity ->

            }
        )
    }

}