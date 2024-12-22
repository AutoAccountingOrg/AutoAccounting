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

package net.ankio.auto.xposed.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.exceptions.ServiceCheckException
import net.ankio.lspatch.services.NotificationService
import net.ankio.lspatch.services.SmsReceiver
import org.ezbook.server.Server


object ServerInfo {

    suspend fun isServerStart(context: Context) = withContext(Dispatchers.IO) {
        checkServer(context)
        SmsReceiver.checkPermission(context)
        NotificationService.checkPermission()
    }

    private suspend fun checkServer(context: Context) {
        val data = Server.request("/");
        if (data === null){
            throw ServiceCheckException(
                context.getString(R.string.server_error_title),
                context.getString(R.string.server_error),
                context.getString(R.string.server_error_btn),
                        action = { activity ->
                //跳转微信设置页面
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", "com.tencent.mm", null)
                intent.setData(uri)
                activity.startActivity(intent)
            })
        }else{
            val json = Gson().fromJson(data,JsonObject::class.java)
            if (json.get("data").asString != BuildConfig.VERSION_NAME){
                throw ServiceCheckException(
                    context.getString(R.string.server_error_version_title),
                    context.getString(R.string.server_error_version),
                    context.getString(R.string.server_error_btn)
                    ,
                    action = { activity ->
                    //跳转微信设置页面
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", "com.tencent.mm", null)
                    intent.setData(uri)
                    activity.startActivity(intent)
                })
            }
        }
    }

}