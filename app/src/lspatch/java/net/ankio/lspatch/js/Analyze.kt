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

package net.ankio.lspatch.js

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.delay
import net.ankio.auto.App
import net.ankio.auto.intent.WakeupIntent
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.storage.Logger
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.BillUtils
import org.ezbook.server.Server.Companion.request
import org.ezbook.server.constant.DataType
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import org.ezbook.server.models.BillResultModel

object Analyze {
    fun start(type: DataType, data: String, appPackage: String){
        AppRuntime.application = App.app
        val filter = PrefManager.smsFilter.split(",")

        if (filter.all { !data.contains(it) }) {
            Logger.d("all filter not contains: $data, $filter")
            return
        }


        App.launch {
           var retryCount = 0
           var result: String? = null

           while (result == null && retryCount < 10) {
               result = request("js/analysis?type=${type.name}&app=$appPackage&fromAppData=false", data)

               if (result == null) {
                   WakeupIntent().toIntent().let {
                       runCatching {
                           AppRuntime.application!!.startActivity(it)
                       }.onFailure {
                           Logger.e("拉起自动记账失败", it)
                       }
                   }
                   retryCount++
                   val delaySeconds = (1L shl (retryCount - 1)) * 10  // 10, 20, 40, 80, 160...
                   Logger.d("Analysis attempt $retryCount failed, retrying in $delaySeconds seconds...")
                   delay(delaySeconds * 1000L)
               }
           }

           if (result != null) {

               BillUtils.handle(result)

               Logger.d("Analysis Result: $result")
           } else {
               Logger.d("Analysis failed after 20 attempts")
           }
       }
    }
}