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

package net.ankio.auto.hooks.sms.utils

import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage


object SmsMessageUtils {
    private const val SMS_CHARACTER_LIMIT = 160

    fun fromIntent(intent: Intent?): Array<SmsMessage> {
        return Telephony.Sms.Intents.getMessagesFromIntent(intent)
    }

    fun getMessageBody(messageParts: Array<SmsMessage>): String {
        if (messageParts.size == 1) {
            return messageParts[0].displayMessageBody
        } else {
            val sb = StringBuilder(SMS_CHARACTER_LIMIT * messageParts.size)
            for (messagePart in messageParts) {
                sb.append(messagePart.displayMessageBody)
            }
            return sb.toString()
        }
    }
}