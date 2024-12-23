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

import kotlinx.coroutines.delay
import net.ankio.auto.App
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import org.ezbook.server.Server.Companion.request
import org.ezbook.server.constant.DataType
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting

object Analyze {
    fun start(type: DataType, data: String, appPackage: String){

        val filter = ConfigUtils.getString(Setting.SMS_FILTER, DefaultData.SMS_FILTER).split(",")

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
                   retryCount++
                   val delaySeconds = (1L shl (retryCount - 1)) * 10  // 10, 20, 40, 80, 160...
                   Logger.d("Analysis attempt $retryCount failed, retrying in $delaySeconds seconds...")
                   delay(delaySeconds * 1000L)
               }
           }

           if (result != null) {
               Logger.d("Analysis Result: $result")
           } else {
               Logger.d("Analysis failed after 20 attempts")
           }
       }
    }
}