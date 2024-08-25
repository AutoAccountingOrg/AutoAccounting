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

package net.ankio.auto.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import net.ankio.auto.App

/**
 * 本地广播助手类
 */
class LocalBroadcastHelper {


    /**
     * 发送应用内广播
     *
     * @param action 广播的动作（唯一标识符）
     * @param extras 附加的数据
     */
    fun sendBroadcast(action: String, extras: Bundle? = null) {
        val intent = Intent(action).apply {
            extras?.let { putExtras(it) }
        }
        App.app.sendBroadcast(intent)
    }

    /**
     * 注册广播接收器
     *
     * @param action 广播的动作（唯一标识符）
     * @param receiver 广播接收器
     */
    fun registerReceiver(action: String, receiver: BroadcastReceiver) {
        val filter = IntentFilter(action)
        App.app.registerReceiver(receiver, filter)
    }

    /**
     * 注销广播接收器
     *
     * @param receiver 广播接收器
     */
    fun unregisterReceiver(receiver: BroadcastReceiver) {
        App.app.unregisterReceiver(receiver)
    }

    /**
     * 简化的广播接收器
     */
    abstract class SimpleBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                onReceive(it.action, it.extras)
            }
        }

        abstract fun onReceive(action: String?, extras: Bundle?)
    }
}
