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

package net.ankio.auto.utils

import android.content.Intent
import net.ankio.auto.storage.Logger

/**
 * 调试工具类
 */
object DebugUtils {

    /**
     * 打印 Intent 的详细信息
     */
    fun printIntent(intent: Intent) {
        val sp = StringBuilder()
        sp.appendLine("Action: ${intent.action}")
        sp.appendLine("Data: ${intent.data}")
        sp.appendLine("Categories: ${intent.categories}")
        sp.appendLine("Type: ${intent.type}")
        sp.appendLine("Flags: ${intent.flags}")

        // 输出所有 extras（键值对）
        intent.extras?.let { extras ->
            sp.appendLine("Extras:")
            for (key in extras.keySet()) {
                @Suppress("DEPRECATION")
                sp.appendLine("  $key: ${extras.get(key)}")
            }
        } ?: run {
            sp.appendLine("No extras")
        }

        Logger.d(sp.toString())
    }
}
