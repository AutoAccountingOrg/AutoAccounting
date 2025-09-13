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
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.hooks.sms.utils.SmsMessageUtils
import org.ezbook.server.constant.DataType
import java.text.Normalizer


/**
 * 短信接收链路 Hook。
 *
 * - 拦截 `InboundSmsHandler.dispatchIntent`，仅处理 `SMS_DELIVER_ACTION`。
 * - 从 `Intent` 提取短信分片并组装完整消息，做 NFC 规范化，输出 JSON。
 * - 保持无副作用与最小侵入，不改变宿主行为（返回 null）。
 */
class SmsIntentHooker : PartHooker() {
    /** Android Telephony 内部包名（目标 Hook 类所在包） */
    private val TELEPHONY_PACKAGE: String = "com.android.internal.telephony"

    /** 目标处理类全名：`InboundSmsHandler` */
    private val SMS_HANDLER_CLASS: String = "$TELEPHONY_PACKAGE.InboundSmsHandler"

    /**
     * 安装 Hook：对 `InboundSmsHandler.dispatchIntent` 进行前置拦截。
     */
    override fun hook() {
        val inboundSmsHandlerClass = Hooker.loader(SMS_HANDLER_CLASS)

        Hooker.allMethodsEqBefore(
            inboundSmsHandlerClass,
            "dispatchIntent",
        ) { param, method ->
            val intent = param.args[0] as Intent
            val action = intent.action

            if (Telephony.Sms.Intents.SMS_DELIVER_ACTION != action) {
                return@allMethodsEqBefore null
            }
            val smsMessageParts: Array<SmsMessage> = SmsMessageUtils.fromIntent(intent)
            // 短信分片为空则忽略
            if (smsMessageParts.isEmpty()) return@allMethodsEqBefore null
            // 原始号码与正文
            val senderRaw: String = smsMessageParts[0].displayOriginatingAddress
            val bodyRaw: String = SmsMessageUtils.getMessageBody(smsMessageParts)
            // NFC 规范化，消除全/半角与组合字符差异
            val sender = Normalizer.normalize(senderRaw, Normalizer.Form.NFC)
            val body = Normalizer.normalize(bodyRaw, Normalizer.Form.NFC)
            // 构建输出 JSON
            val json = JsonObject().apply {
                addProperty("sender", sender)
                addProperty("body", body)
                addProperty("t", System.currentTimeMillis())
            }

            analysisData(DataType.DATA, Gson().toJson(json))
            return@allMethodsEqBefore null
        }

    }
}