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

package net.ankio.auto.xposed.hooks.alipay.hooks

import android.app.Application
import com.google.gson.Gson
import com.google.gson.JsonArray
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AppRuntime
import org.ezbook.server.constant.DataType

class MessageBoxHooker : PartHooker() {
    override fun hook() {
        val msgboxInfoServiceImpl = Hooker.loader("com.alipay.android.phone.messageboxstatic.biz.sync.d")
        val syncMessage = Hooker.loader("com.alipay.mobile.rome.longlinkservice.syncmodel.SyncMessage")
        Hooker.before(
            msgboxInfoServiceImpl,
            "onReceiveMessage",
            syncMessage,
        ) { param ->
            val syncMessageObject = param.args[0]
            val result = XposedHelpers.callMethod(syncMessageObject, "getData") as String
            // 收到的是数组，拆分
            Gson().fromJson(result, JsonArray::class.java).forEach { jsonObject ->

                val jsonArray =
                    JsonArray().apply {
                        add(jsonObject)
                    }

                AppRuntime.manifest.logD("Hooked Alipay Message Box：$jsonArray")
                // 调用分析服务进行数据分析
                AppRuntime.manifest.analysisData(DataType.DATA, Gson().toJson(jsonArray))
            }
        }
    }
}
