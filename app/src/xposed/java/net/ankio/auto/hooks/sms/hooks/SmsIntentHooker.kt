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

package net.ankio.auto.hooks.sms.hooks

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import com.google.gson.Gson
import com.google.gson.JsonObject
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.api.PartHooker
import net.ankio.auto.core.utils.MD5HashTable
import net.ankio.auto.hooks.sms.utils.SmsMessageUtils
import org.ezbook.server.constant.DataType
import org.json.JSONObject
import java.lang.reflect.Method
import java.text.Normalizer


class SmsIntentHooker:PartHooker() {
   // private val hashTable = MD5HashTable()
    private val TELEPHONY_PACKAGE: String = "com.android.internal.telephony"
    private val SMS_HANDLER_CLASS: String = "$TELEPHONY_PACKAGE.InboundSmsHandler"
    override fun hook(
        hookerManifest: HookerManifest,
        application: Application?,
        classLoader: ClassLoader
    ) {

        // 实际上这是一个通用的方式，不再使用精确匹配来找到对应的 Method，而使用模糊搜索的方式
        // 但是之前分 API 匹配的逻辑在以往 Android 版本的系统之中已经验证通过，故而保留原有逻辑
        val inboundSmsHandlerClass = XposedHelpers.findClass(SMS_HANDLER_CLASS, classLoader)
        if (inboundSmsHandlerClass == null) {
            hookerManifest.logE(Throwable("Class $SMS_HANDLER_CLASS not found"))
            return
        }

        val methods = inboundSmsHandlerClass.declaredMethods
        var exactMethod: Method? = null
        val DISPATCH_INTENT = "dispatchIntent"
        for (method in methods) {
            val methodName = method.name
            if (DISPATCH_INTENT == methodName) {
                exactMethod = method
            }
        }


        if (exactMethod == null) {
            hookerManifest.logE(Throwable("Method $SMS_HANDLER_CLASS for Class $DISPATCH_INTENT cannot found"))
            return
        }
        XposedBridge.hookMethod(exactMethod, object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun beforeHookedMethod(param: MethodHookParam) {
                val intent = param.args[0] as Intent
                val action = intent.action

                if (Telephony.Sms.Intents.SMS_DELIVER_ACTION != action) {
                    return
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

                hookerManifest.analysisData(DataType.DATA, Gson().toJson(json))
                return
            }
        })
    }
}