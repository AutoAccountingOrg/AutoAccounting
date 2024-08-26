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

package net.ankio.auto.hooks.android.hooks

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.core.App
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.api.PartHooker
import org.ezbook.server.Server


/**
 *
 */
class KeepActiveHooker:PartHooker {

    override fun hook(
        hookerManifest: HookerManifest,
        application: Application?,
        classLoader: ClassLoader
    ) {
      App.scope.launch {
          delay(60*1000) // 一分钟后再检查自动记账服务是否启动，因为开机需要一段时间才能注入进程
          checkServerIsStart(hookerManifest,application!!)
      }
    }

    private suspend fun checkServerIsStart(hookerManifest: HookerManifest,application: Application) = withContext(Dispatchers.IO){
      while (true){
          try {
              if (Server.request("/") == null){
                  hookerManifest.logD("KeepActiveHooker checkServerIsStart server is not start")
                  //自动记账服务停止运行了，执行系统命令
                 // Runtime.getRuntime().exec("am start -n com.android.keychain/.KeyChainActivity")
                  val intent = Intent()
                  intent.setComponent(
                      ComponentName(
                          "com.android.keychain",
                          "com.android.keychain.KeyChainActivity"
                      )
                  )
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                  application.startActivity(intent)
              }
          } catch (e: Exception) {
              e.printStackTrace()
              hookerManifest.logD("KeepActiveHooker checkServerIsStart error: ${e.message}")
              hookerManifest.logE(e)
          }
          delay(5000) //检测间隔为5秒
      }
    }


}