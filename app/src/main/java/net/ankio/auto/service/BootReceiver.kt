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
package net.ankio.auto.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.Logger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import kotlin.system.exitProcess

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // 启动服务
            val cacheDir = AppUtils.getApplication().externalCacheDir
            val shell = "sh ${cacheDir?.absolutePath}/start.sh"
            try {
                val process = Runtime.getRuntime().exec("su")
                val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
                val bufferedWriter = OutputStreamWriter(process.outputStream)

                Logger.i("Executing shell command: $shell")

                // 写入命令
                bufferedWriter.write(shell)
                bufferedWriter.flush()
                bufferedWriter.close()

                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    Logger.i(line ?: "")
                }
                process.waitFor()
                bufferedReader.close()
            } catch (e: Exception) {
                e.printStackTrace()
                Logger.e("Error executing shell command", e)
            } finally {
                exitProcess(0)
            }
        }
    }
}
