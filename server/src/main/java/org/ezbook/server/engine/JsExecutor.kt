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
import org.ezbook.server.log.ServerLog
import org.ezbook.server.tools.runCatchingExceptCancel
import java.io.Closeable

/**
 * 轻量级 JS 执行器，集中管理 QuickJS 生命周期，避免重复创建 Runtime/Context
 */
class JsExecutor : Closeable {

    private val quickJs by lazy { QuickJS.Builder().build() }

    suspend fun run(code: String, data: String = ""): String =
        runCatchingExceptCancel {
            ServerLog.d("执行JS：$code")
            // 每次执行创建独立的 Runtime/Context，避免跨脚本污染
            quickJs.createJSRuntime().use { rt ->
                rt.createJSContext().use { ctx ->
                    val output = StringBuilder()

                    // 注入简单的 print 收集器，便于 JS 调试输出回传
                    ctx.globalObject.setProperty("print", ctx.createJSFunction { _, args ->
                        args.joinToString(" ") { it.cast(JSString::class.java).string }
                            .also { output.append(it) }
                        ctx.createJSUndefined()
                    })

                    // 注入数据上下文，供规则或脚本读取
                    if (data.isNotEmpty()) {
                        ctx.globalObject.setProperty("data", ctx.createJSString(data))
                    }

                    // 执行脚本：优先返回 evaluate 结果，否则回退到 print 收集
                    val evalResult: String? = ctx.evaluate(code, "bill.js", String::class.java)
                    val result = if (output.isNotEmpty()) output.toString() else (evalResult ?: "")
                    result
                }
            }
        }.getOrElse {
            // 失败时打印详细错误，包含异常与堆栈，便于定位问题；返回空串让上层决定回退策略
            ServerLog.e("JS 执行失败：${it.message}", it)
            it.message ?: ""
        }

    override fun close() {}
}

