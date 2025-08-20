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

package org.ezbook.server.engine

import com.shiqi.quickjs.JSString
import com.shiqi.quickjs.QuickJS
import org.ezbook.server.Server
import java.io.Closeable

/**
 * 轻量级 JS 执行器，集中管理 QuickJS 生命周期，避免重复创建 Runtime/Context
 */
class JsExecutor : Closeable {

    private val quickJs by lazy { QuickJS.Builder().build() }

    suspend fun run(code: String, data: String = ""): String = try {
        quickJs.createJSRuntime().use { rt ->
            rt.createJSContext().use { ctx ->
                val output = StringBuilder()
                ctx.globalObject.setProperty("print", ctx.createJSFunction { _, args ->
                    args.joinToString(" ") { it.cast(JSString::class.java).string }
                        .also { output.append(it) }
                    ctx.createJSUndefined()
                })
                if (data.isNotEmpty()) ctx.globalObject.setProperty(
                    "data",
                    ctx.createJSString(data)
                )
                ctx.evaluate(code, "bill.js", String::class.java)
                output.takeIf { it.isNotEmpty() }?.toString() ?: ""
            }
        }
    } catch (t: Throwable) {
        Server.log(t)
        ""
    }

    override fun close() {}
}

