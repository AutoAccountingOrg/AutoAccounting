/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.app.Activity
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.SmsMessage
import android.provider.Telephony
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.JsonObject
import net.ankio.auto.App
import net.ankio.auto.autoApp
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.http.api.JsAPI
import net.ankio.auto.service.api.IService
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.constant.DataType


class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // 验证Intent的action，确保只处理SMS接收广播，防止恶意应用发送伪造Intent
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        if (PrefManager.workMode != WorkMode.Ocr) return
        val bundle: Bundle? = intent.extras
        if (bundle != null) {
            val pdus = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    bundle.getSerializable("pdus", Array::class.java) as Array<*>
                }

                else -> {
                    @Suppress("DEPRECATION")
                    bundle.getSerializable("pdus") as Array<*>
                }
            }

            var sender = ""
            var messageBody = ""
            for (pdu in pdus) {
                val smsMessage =
                    SmsMessage.createFromPdu(pdu as ByteArray?, bundle.getString("format"))
                sender = smsMessage.displayOriginatingAddress
                messageBody += smsMessage.messageBody
            }
            if (PrefManager.dataFilter.all { !messageBody.contains(it) }) {
                Logger.d("数据信息不在识别关键字里面，忽略")
                return
            }
            val json = JsonObject().apply {
                addProperty("sender", sender)
                addProperty("body", messageBody)
                addProperty("t", System.currentTimeMillis())
            }

            App.launch {
                val billResult =
                    JsAPI.analysis(DataType.DATA, Gson().toJson(json), "com.android.phone")
                Logger.d("识别结果：${billResult?.billInfoModel}")
            }
        }
    }

    companion object : IService {
        override fun hasPermission(): Boolean {
            return ContextCompat.checkSelfPermission(
                autoApp,
                Manifest.permission.RECEIVE_SMS
            ) == PackageManager.PERMISSION_GRANTED
        }

        override fun startPermissionActivity(context: Context) {
            if (context is Activity) {
                ActivityCompat.requestPermissions(
                    context,
                    arrayOf(
                        Manifest.permission.RECEIVE_SMS,
                    ),
                    100
                )
            } else {
                // 否则再退回到应用详情页，让用户手动开启
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }

    }
}