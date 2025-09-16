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

package net.ankio.auto.xposed.hooks.alipay.hooks

import android.webkit.ValueCallback
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.CoroutineUtils
import org.ezbook.server.constant.DataType

class WebViewHooker : PartHooker() {

    companion object {
        const val WAIT_TIME = 100L
        const val MAX_WAIT_MS = 120_000L
    }

    override fun hook() {

        val webView = Hooker.loader("com.alipay.mobile.nebulacore.web.H5WebView")

        Hooker.after(
            webView,
            "evaluateJavascript",
            String::class.java,
            ValueCallback::class.java,
        ) { param ->
            val script = param.args[0] as String
            val obj = param.thisObject
            val urlObj = XposedHelpers.callMethod(obj, "getUrl") ?: return@after
            val url = urlObj as String
            if (!url.contains("tradeNo=")) return@after
            XposedBridge.log(script)
            if (script.contains("(function(){window.ALIPAYVIEWAPPEARED=1})()")) {
                // 一次性、幂等式注入，避免重复覆盖与页面抖动
                val inject = (
                    """
                    (function(){
                        if (window.__ankioHooked) return;
                        if (typeof AlipayJSBridge === 'undefined' || typeof AlipayJSBridge.call !== 'function') return;
                       
                        window.__ankioHooked = true;
                        window.ankioResults = {};
                        var alipayCall = AlipayJSBridge.call;
                        AlipayJSBridge.call = function(a,b,c){
                            if (typeof c === 'function'){
                                alipayCall(a,b,function(d){
                                    if (d && d.ariverRpcTraceId !== undefined){
                                        window.ankioResults = d;
                                    }
                                    c(d);
                                });
                            } else {
                                alipayCall(a,b,c);
                            }
                        };
                    })();
                    """.trimIndent()
                        )
                XposedHelpers.callMethod(
                    obj,
                    "evaluateJavascript",
                    inject,
                    null,
                )

                var needWait = true

                val resultCallback =
                    ValueCallback<String> { result ->
                        if (result.isNullOrEmpty() || result == "{}" || result == "null") {
                            return@ValueCallback
                        }

                        needWait = false
                        d("Hooked Alipay Bill List Data：$result")
                        analysisData(DataType.DATA, result)
                    }
                // TODO 这里原本计划直接注入JSBridge,但是一直注入失败（没有反应），所以使用一个懒办法
                CoroutineUtils.withIO {
                    withTimeout(MAX_WAIT_MS) {
                        while (needWait) {
                            XposedHelpers.callMethod(
                                obj,
                                "evaluateJavascript",
                                "javascript:window.ankioResults",
                                resultCallback,
                            )
                            delay(WAIT_TIME)
                        }
                    }
                }
            }
        }
    }
}
