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
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.delay
import net.ankio.auto.App
import net.ankio.auto.xposed.core.App.Companion.TAG
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.http.api.BillAPI
import org.ezbook.server.intent.BillInfoIntent
import io.github.oshai.kotlinlogging.KotlinLogging

object UnLockScreen {

    private val logger = KotlinLogging.logger(this::class.java.name)
    suspend fun launchUnEditedBills(){
        delay(3000)
        val list = BillAPI.edit()
        logger.debug { "BillAPI.edit()：$list" }
        list.forEach { billInfoModel ->
            delay(1000)
            val floatIntent =
                BillInfoIntent(billInfoModel, "JsRoute", null)
            val panelIntent = floatIntent.toIntent()
            try {
                AppRuntime.application!!.baseContext.startActivity(panelIntent)
            } catch (t: Throwable) {
                logger.debug { "Failed to start auto server：$t" }
            }
        }
    }
    fun init() {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_USER_PRESENT)

        val receiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val pendingResult = goAsync()
                App.launch {
                    try {
                        if (intent.action == Intent.ACTION_USER_PRESENT) {
                            logger.debug {
                                "User unlocked the device and entered the home screen."
                            }
                            launchUnEditedBills()
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }


        AppRuntime.application!!.baseContext.registerReceiver(receiver, filter)
    }
}