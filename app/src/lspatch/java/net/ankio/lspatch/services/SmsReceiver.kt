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

package net.ankio.lspatch.services

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.JsonObject
import net.ankio.auto.R
import net.ankio.auto.exceptions.ServiceCheckException
import net.ankio.lspatch.js.Analyze
import org.ezbook.server.constant.DataType


class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val bundle: Bundle? = intent!!.extras
        if (bundle != null) {
            val pdus =
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) bundle.getParcelableArray(
                    "pdus",
                    ByteArray::class.java
                ) else bundle.getParcelableArray("pdus")
            if (pdus == null) {
                Log.e("SmsReceiver", "SmsReceiver: pdus is null")
                return
            }

            var sender = ""
            var messageBody = ""
            for (pdu in pdus) {
                val smsMessage = SmsMessage.createFromPdu(pdu as ByteArray?, bundle.getString("format"))
                sender = smsMessage.displayOriginatingAddress
                messageBody += smsMessage.messageBody
            }
            val json = JsonObject().apply {
                addProperty("sender", sender)
                addProperty("body", messageBody)
                addProperty("t", System.currentTimeMillis())
            }
            Analyze.start(DataType.DATA, Gson().toJson(json), "com.android.phone")
        }
    }
    companion object{
        fun checkPermission(context: Context){
            val result = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS)
            if (result != PackageManager.PERMISSION_GRANTED) {
                throw ServiceCheckException(
                    context.getString(R.string.permission_not_granted_sms),
                    context.getString(R.string.permission_not_granted_sms_desc),
                    context.getString(R.string.permission_not_granted_sms_btn)
                ){ activity ->
                    val SMS_PERMISSION_CODE = 100
                    ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.RECEIVE_SMS), SMS_PERMISSION_CODE);
                }
            }
        }
    }
}