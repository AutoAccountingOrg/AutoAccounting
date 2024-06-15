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
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    private val callbacks: HashMap<String, (json: JsonObject) -> Unit> = HashMap()
    private var times = 0
    suspend fun reconnect() = withContext(Dispatchers.Main){
        ws = null
        Logger.i("重连自动记账服务...$times")
        withContext(Dispatchers.IO){
            delay(1000L)
            times++
            withContext(Dispatchers.Main){
                connect(true)
            }
        }



    }
    suspend fun sendMsg(
        type: String,
        data: Any?,
    ): Any? =
        suspendCancellableCoroutine { continuation ->
            if (ws == null) {
                //  Logger.d("WebSocket未连接")
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }
            val json = JsonObject()
            val id = UUID.randomUUID().toString()
            json.addProperty("id", id)
            json.addProperty("type", type)
            json.add("data", Gson().toJsonTree(data))
            callbacks[id] = { it: JsonObject ->
                continuation.resume(it.get("data"))
            }
            val msg = Gson().toJson(json)
            Logger.d("发送消息：$msg")
            ws!!.send(msg)
            // 等待返回
        }

    fun connect(reconnect:Boolean = false) {
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
                }

                override fun onMessage(
                    webSocket: WebSocket,
                    text: String,
                ) {
                    Logger.d("收到服务端消息：$text")
                    AppUtils.getScope().launch {
                        runCatching {
                            val json = Gson().fromJson(text, JsonObject::class.java)
                            val type = json.get("type").asString
                            if (type == "auth") {
                                val token = getToken()
                                if (token.isNotEmpty()) {
                                    val msg =
                                        JsonObject().apply {
                                            addProperty("type", "auth")
                                            addProperty("data", token.trim())
                                            addProperty("id", "")
                                        }.toString()

                                    Logger.i("授权响应：$msg")
                                    webSocket.send(
                                        msg,
                                    )
                                } else {
                                    webSocket.close(1000, "Token not found")
                                    Logger.i("Token not found")
                                }
                                // ws = webSocket
                                return@runCatching
                            } else if (type == "auth/success") {
                                Logger.d("服务链接成功")
                                ws = webSocket
                                EventBus.post(AutoServerConnectedEvent())
                            }
                            val id = json.get("id").asString
                            callbacks[id]?.invoke(json)
                            callbacks.remove(id)
                        }.onFailure {
                            it.printStackTrace()
                            webSocket.close(1000, it.message)
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
                    ws = null
                    webSocket.close(1000, null)

                    println("WebSocket closing: $code / $reason")
                }

                override fun onClosed(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String,
                ) {
                    ws = null
                    println("WebSocket closed: $code / $reason")
                    if(reconnect){
                        AppUtils.getScope().launch {
                            reconnect()
                        }
                    }else{
                        EventBus.post(AutoServiceErrorEvent(AutoServiceException( reason)))
                    }

                }

                override fun onFailure(
                    webSocket: WebSocket,
                    t: Throwable,
                    response: Response?,
                ) {
                    Logger.e("WebSocket error: " + t.message, t)
                    if(reconnect){
                        AppUtils.getScope().launch {
                            reconnect()
                        }
                    }else{
                        EventBus.post(AutoServiceErrorEvent(AutoServiceException(t.message ?: "WebSocket error")))
                    }


                }
            }

        webSocket = client!!.newWebSocket(request, listener)
    }

    suspend fun config(): AccountingConfig {
        val json = SettingModel.get("server", "config")
        Logger.i("Config: $json")
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
        val file = File(AppUtils.getApplication().externalCacheDir!!.absolutePath + "/../token.txt")
        Logger.i("Token file path: ${file.absolutePath}")
        if (file.exists()) {
            val token = file.readText().trim()
            Logger.i("Token: $token")
            return token
        }
        return ""
    }

    fun isConnected(): Boolean {
        return ws != null
    }
}
