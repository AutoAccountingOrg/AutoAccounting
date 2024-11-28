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

package net.ankio.auto.xposed.hooks.common

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.google.gson.Gson
import kotlinx.coroutines.delay
import net.ankio.auto.App
import net.ankio.auto.xposed.core.App.Companion.TAG
import net.ankio.auto.xposed.core.logger.Logger
import org.ezbook.server.db.model.BillInfoModel

object UnLockScreen {
    fun init(context: Context){
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_USER_PRESENT)

        val receiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_USER_PRESENT) {
                    // 用户解锁了设备
                    Logger.logD(TAG,"User unlocked the device and entered the home screen.")
                    App.launch {
                        val list = BillInfoModel.edit()
                        Logger.logD(TAG,"BillInfoModel.edit()：$list")
                        list.forEach { billInfoModel ->
                            delay(1000)
                            val panelIntent = Intent()
                            panelIntent.putExtra("billInfo", Gson().toJson(billInfoModel))
                            panelIntent.putExtra("id", billInfoModel.id)
                            panelIntent.putExtra("showWaitTip", true)
                            panelIntent.putExtra("from","JsRoute")
                            panelIntent.setComponent(
                                ComponentName(
                                    "net.ankio.auto.xposed",
                                    "net.ankio.auto.ui.activity.FloatingWindowTriggerActivity"
                                )
                            )
                            panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            panelIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                            panelIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                            panelIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                            Logger.logD(TAG,"Calling auto server：$intent")
                            try {
                                context.startActivity(panelIntent)
                            }catch (t:Throwable){
                                Logger.logD(TAG,"Failed to start auto server：$t")
                            }
                        }
                    }
                }
            }
        }

        context.registerReceiver(receiver, filter)
    }
}