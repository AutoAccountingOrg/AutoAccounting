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

package net.ankio.auto.xposed.hooks.sms.hooks

import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import com.google.gson.Gson
import com.google.gson.JsonObject
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.DataUtils
import net.ankio.auto.xposed.core.utils.ThreadUtils
import net.ankio.auto.xposed.hooks.sms.utils.SmsMessageUtils
import org.ezbook.server.constant.DataType
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.SettingModel
import java.text.Normalizer


class SmsIntentHooker: PartHooker() {
   // private val hashTable = MD5HashTable()
    private val TELEPHONY_PACKAGE: String = "com.android.internal.telephony"
    private val SMS_HANDLER_CLASS: String = "$TELEPHONY_PACKAGE.InboundSmsHandler"
    override fun hook() {
        val inboundSmsHandlerClass = Hooker.loader(SMS_HANDLER_CLASS)

        Hooker.allMethodsEqBefore(
            inboundSmsHandlerClass,
            "dispatchIntent",
        ){ param ->
            val intent = param.args[0] as Intent
            val action = intent.action

            if (Telephony.Sms.Intents.SMS_DELIVER_ACTION != action) {
                return@allMethodsEqBefore null
            }
            val smsMessageParts: Array<SmsMessage> = SmsMessageUtils.fromIntent(intent)
            var sender: String = smsMessageParts[0].displayOriginatingAddress
            var body: String = SmsMessageUtils.getMessageBody(smsMessageParts)
            sender = Normalizer.normalize(sender, Normalizer.Form.NFC)
            body = Normalizer.normalize(body, Normalizer.Form.NFC)
            val json = JsonObject().apply {
                addProperty("sender",sender)
                addProperty("body",body)
                addProperty("t",System.currentTimeMillis())
            }

            ThreadUtils.launch {
                val filter = SettingModel.get(Setting.SMS_FILTER, DefaultData.SMS_FILTER).split(",")

                if (filter.all { !body.contains(it) }) {
                    Logger.d("all filter not contains: $body, $filter")
                    return@launch
                }

                AppRuntime.manifest.analysisData(DataType.DATA, Gson().toJson(json))
            }
            return@allMethodsEqBefore null
        }

    }
}