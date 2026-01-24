/*
 * Copyright (C) 2026 ankio(ankio@ankio.net)
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

package org.ezbook.server.log

import kotlinx.coroutines.runBlocking
import org.ezbook.server.Server
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.LogModel
import org.ezbook.server.tools.SettingUtils

object ServerLog : BaseLogger() {
    override fun isDebugMode(): Boolean = runBlocking {
        SettingUtils.debugMode()
    }
    override fun logcatFormater(
        priority: LogLevel, file: String, line: Int, msg: String, tr: Throwable?
    ): String {
        var prefix = "[ 自动记账S ]"
        if (line != -1) prefix = "$prefix($file:$line) "

        return prefix + "$msg\n${tr?.stackTrace?.joinToString("\n") ?: ""}".trimEnd()
    }

    override fun logModelFormater(
        priority: LogLevel, className: String, file: String, line: Int, msg: String, tr: Throwable?
    ): LogModel = LogModel(
        level = priority,
        app = "AutoServer",
        // location 统一为 "类名(File.kt:行号)"
        location = className + if (line != -1) "($file:$line)" else "",
        message = "$msg\n${tr?.stackTrace?.joinToString("\n") ?: ""}".trimEnd()
    )

    override fun onLogModel(model: LogModel) {
        Server.withIO {
            Db.get().logDao().insert(model)
        }
    }
}