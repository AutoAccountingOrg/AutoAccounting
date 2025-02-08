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

import org.ezbook.server.Server
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.SettingModel
import kotlinx.coroutines.delay
import net.ankio.auto.storage.Logger

object ActiveInfo {
   suspend fun getFramework(): String {
       return SettingModel.get(Setting.KEY_FRAMEWORK, "Unknown Framework")
           .ifEmpty { "LSPatch Framework" }
    }

   suspend fun isModuleActive(): Boolean {
       val maxAttempts = 3  // 最大重试次数
       val delayBetweenAttempts = 500L  // 每次重试间隔3秒

       repeat(maxAttempts) { attempt ->
           try {
               val response = Server.request("/")
               if (response != null) return true
           } catch (e: Exception) {
               if (attempt == maxAttempts - 1) {
                   Logger.e("模块激活检查失败", e)
               }
           }

           if (attempt < maxAttempts - 1) {
               delay(delayBetweenAttempts)
           }
       }

       return false
    }
}