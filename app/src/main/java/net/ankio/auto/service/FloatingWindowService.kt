/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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

import android.app.Service.START_NOT_STICKY
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.intent.FloatingIntent
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.utils.ToastUtils
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.BillInfoModel

class FloatingWindowService(val context: Context) {



    private lateinit var floatingQueue: FloatingQueue

    var bills = Channel<BillInfoModel>(capacity = 1, BufferOverflow.DROP_LATEST)

    fun onCreate() {
        floatingQueue = FloatingQueue { intent, floatingQueue ->
            //单独处理
            FloatingWindowManager(this, intent, floatingQueue).process()
        }
    }


    fun onStartCommand(
        intent: Intent,
        flags: Int,
        startId: Int,
    ): Int {
        Logger.d("FloatingWindowService onStartCommand")
        val parent = FloatingIntent.parse(intent).parent
        if (parent != null) {
            if (ConfigUtils.getBoolean(
                    Setting.SHOW_DUPLICATED_POPUP,
                    DefaultData.SHOW_DUPLICATED_POPUP
                )
            ) {
                //说明是重复账单
                ToastUtils.info(context.getString(R.string.repeat_bill))
            }
            App.launch(Dispatchers.Main) {
                bills.send(parent)
            }
            Logger.d("Repeat Bill, Parent: $parent")
            return START_NOT_STICKY
        }
        floatingQueue.send(intent)
        return START_NOT_STICKY
    }


    fun onDestroy() {
        floatingQueue.shutdown()
        runCatching { !bills.isClosedForSend && bills.close() }
    }
}
