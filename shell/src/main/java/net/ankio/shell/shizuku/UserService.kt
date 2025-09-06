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

package net.ankio.shell.shizuku

import androidx.annotation.Keep
import net.ankio.shell.IUserService
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess


class UserService @Keep constructor() : IUserService.Stub() {
    override fun destroy() {
        exitProcess(0)
    }

    override fun execCommand(command: String?): String {
        if (command.isNullOrBlank()) return ""
        val output = StringBuilder()

        try {
            // 用 ProcessBuilder，通过 sh -c 来解析复合命令，并合并 stderr 到 stdout
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start()

            // 边读边等，不要先 waitFor 再读
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.appendLine(line)
                }
            }

            // 等到子进程结束
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return output.toString()
    }


}