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

package net.ankio.auto.utils.server

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.ankio.auto.events.AutoServerConnectedEvent
import net.ankio.auto.events.AutoServiceErrorEvent
import net.ankio.auto.exceptions.AutoServiceException
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.Logger
import net.ankio.auto.utils.event.EventBus
import net.ankio.auto.utils.server.model.SettingModel
import net.ankio.common.config.AccountingConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class AutoServer {
    companion object {
        const val PORT = 52045
        const val HOST = "ws://127.0.0.1"
    }

    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null

    init {
        client =
            OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build()
    }

    private var ws: WebSocket? = null
    private val callbacks: HashMap<String, (json: JSONObject) -> Unit> = HashMap()

    suspend fun sendMsg(
        type: String,
        data: Any?,
    ): Any? =
        suspendCancellableCoroutine { continuation ->
            if (ws == null) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }
            val json = JSONObject()
            val id = UUID.randomUUID().toString()
            json.put("id", id)
            json.put("type", type)
            json.put("data", data)
            callbacks[id] = { it: JSONObject ->
                continuation.resume(it.get("data"))
            }
            ws!!.send(Gson().toJson(json))
            // 等待返回
        }

    fun connect() {
        val request =
            Request.Builder()
                .url("$HOST:$PORT/")
                .build()

        val listener: WebSocketListener =
            object : WebSocketListener() {
                override fun onOpen(
                    webSocket: WebSocket,
                    response: Response,
                ) {
                    ws = webSocket
                    EventBus.post(AutoServerConnectedEvent())
                }

                override fun onMessage(
                    webSocket: WebSocket,
                    text: String,
                ) {
                    AppUtils.getScope().launch {
                        runCatching {
                            val json = Gson().fromJson(text, JSONObject::class.java)
                            val type = json.getString("type")
                            if (type == "auth") {
                                val token = getToken()
                                if (token.isNotEmpty()) {
                                    ws!!.send(
                                        Gson().toJson(
                                            JSONObject().apply {
                                                put("type", "auth")
                                                put("data", token)
                                                put("id", "")
                                            },
                                        ),
                                    )
                                } else {
                                    ws!!.close(1000, "Token not found")
                                    Logger.i("Token not found")
                                }
                                return@runCatching
                            }
                            val id = json.getString("id")
                            callbacks[id]?.invoke(json)
                            callbacks.remove(id)
                        }.onFailure {
                            it.printStackTrace()
                        }
                    }
                }

                override fun onMessage(
                    webSocket: WebSocket,
                    bytes: ByteString,
                ) {
                }

                override fun onClosing(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String,
                ) {
                    webSocket.close(1000, null)
                    ws = null
                    println("WebSocket closing: $code / $reason")
                }

                override fun onClosed(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String,
                ) {
                    println("WebSocket closed: $code / $reason")

                    EventBus.post(AutoServiceErrorEvent(AutoServiceException("WebSocket closed: $code / $reason")))
                }

                override fun onFailure(
                    webSocket: WebSocket,
                    t: Throwable,
                    response: Response?,
                ) {
                    System.err.println("WebSocket error: " + t.message)
                    Logger.e("WebSocket error: " + t.message, t)
                    EventBus.post(AutoServiceErrorEvent(AutoServiceException(t.message ?: "WebSocket error")))
                }
            }

        webSocket = client!!.newWebSocket(request, listener)
    }

    suspend fun config(): AccountingConfig {
        val json = SettingModel.get(AppUtils.getApplication().packageName, "config")
        return runCatching { Gson().fromJson(json, AccountingConfig::class.java) }.getOrNull() ?: AccountingConfig()
    }

    suspend fun copyAssets() =
        withContext(Dispatchers.IO) {
            val context = AppUtils.getApplication()
            val cacheDir = context.externalCacheDir!!.absolutePath + File.separator + "shell"
            val copyFiles = arrayListOf("version.txt", "starter.sh", "apps.txt")
            // 检查cpu架构
            val cpu = System.getProperty("os.arch")!!
            val androidCpu =
                when {
                    cpu.contains("arm") -> "armeabi-v7a"
                    cpu.contains("aarch64") -> "arm64-v8a"
                    cpu.contains("i386") || cpu.contains("i686") -> "x86"
                    cpu.contains("x86_64") -> "x86_64"
                    else -> "arm64-v8a"
                }
            copyFiles.add("$androidCpu/auto_accounting_starter")
            copyFiles.forEach {
                // 从assets复制到cacheDir
                runCatching {
                    context.assets.open("shell" + File.separator + it).use { input ->
                        val file = File(cacheDir, it)
                        if (!file.exists()) {
                            file.parentFile?.mkdirs()
                        } else {
                            file.delete()
                        }
                        file.createNewFile()
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                }.onFailure {
                    Logger.e("复制文件失败", it)
                }
            }
        }

    fun getToken(): String {
        val file = File(AppUtils.getApplication().externalCacheDir!!.absolutePath + "/token.txt")
        if (file.exists()) {
            return file.readText()
        }
        return ""
    }
}
