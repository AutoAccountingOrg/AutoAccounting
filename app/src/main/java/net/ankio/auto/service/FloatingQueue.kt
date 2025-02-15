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

package net.ankio.auto.service

import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import net.ankio.auto.App
import net.ankio.auto.storage.Logger
import net.ankio.auto.intent.FloatingIntent

/**
 * 自动记账悬浮窗队列类。
 *
 * 该类负责管理悬浮窗Intent的队列，确保每个Intent只被处理一次。通过回调函数处理队列中的Intent，并在处理完成后移除对应的Intent。
 *
 * 主要功能包括：
 * - 发送新的悬浮窗Intent到队列中。
 * - 开始处理队列中的Intent。
 * - 停止处理队列中的Intent。
 * - 关闭队列并清理资源。
 *
 * @param callback 用于处理队列中Intent的回调函数。回调函数接收两个参数：当前处理的Intent和当前的FloatingQueue实例。
 */
class FloatingQueue(private val callback: (FloatingIntent, FloatingQueue) -> Unit) {
    private val channel = Channel<FloatingIntent>(Channel.BUFFERED)
    // 用于追踪当前队列中的Intent
    private val stopChannel = Channel<Unit>(capacity = 1, BufferOverflow.DROP_LATEST)
    init {
        startProcessing()
    }
    /**
     * 停止处理队列中的Intent
     */
    suspend fun processStop(){
       runCatching {
           stopChannel.send(Unit)
       }
    }
    /**
     * 发送新的悬浮窗Intent到队列中
     *
     * @param rawIntent 原始Intent
     */
    fun send(intent: FloatingIntent) {
        App.launch(Dispatchers.Main) {
            try {
                channel.send(intent)
            } catch (e: Exception) {
                // 发送失败时需要移除key
                Logger.e("发送悬浮窗Intent失败", e)
            }
        }
    }
    
    /**
     * 开始处理队列消息
     */
    private fun startProcessing() {
        App.launch(Dispatchers.Main) {
            try {
                for (intent in channel) {
                    try {
                        callback(intent,this@FloatingQueue)
                        stopChannel.receive()
                    } finally {
                    }
                }
            } catch (e: Exception) {
                Logger.e("FloatingQueue处理异常", e)
            }
        }
    }

    private var isProcessing = true
    /**
     * 关闭队列
     */
    fun shutdown() {
        if (!isProcessing) {
            return
        }
        isProcessing = false
        runCatching { channel.close() }
        runCatching { stopChannel.close() }
    }
}