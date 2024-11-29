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

import android.app.Application
import android.webkit.ValueCallback
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.ThreadUtils
import org.ezbook.server.constant.DataType

class WebViewHooker : PartHooker() {

    companion object {
        const val WAIT_TIME = 100L
        const val MAX_CHECK_COUNT = 600
    }

    override fun hook() {

        val webView = Hooker.loader("com.alipay.mobile.nebulacore.web.H5WebView")

        Hooker.after(
            webView,
            "evaluateJavascript",
            String::class.java,
            ValueCallback::class.java,
        ){ param ->
            val script = param.args[0] as String
            val obj = param.thisObject
            val urlObj = XposedHelpers.callMethod(obj, "getUrl") ?: return@after
            val url = urlObj as String
            if (!url.contains("tradeNo=")) return@after
            if (script.contains("AlipayJSBridge.call(\"setStartParam\"")) {
                val js =
                    """
                            javascript:(function(){
                                 window.ankioResults = {};
                                let alipayCall = AlipayJSBridge.call;
                                AlipayJSBridge.call = function(a,b,c){
                                    if(typeof c === 'function'){
                                         alipayCall(a,b,function(d){
                                            if(d.ariverRpcTraceId!==undefined){
                                                window.ankioResults = d;
                                            }
                                            c(d);
                                         });
                                        
                                    }else{
                                        alipayCall(a,b,c);
                                    }
                                };
                            })();
                            """.trimIndent().replace("\n", "")
                XposedHelpers.callMethod(
                    obj,
                    "loadUrl",
                    js,
                )

                var needWait = true
                var count = MAX_CHECK_COUNT

                val resultCallback =
                    ValueCallback<String> { result ->
                        if (result.isNullOrEmpty() || result.equals("{}") || result.equals("null")) {
                            return@ValueCallback
                        }

                        needWait = false
                        AppRuntime.manifest.logD("Hooked Alipay Bill List Data：$result")
                        AppRuntime.manifest.analysisData(DataType.DATA, result)
                    }

                ThreadUtils.launch {
                    withContext(Dispatchers.IO) {
                        while (needWait && count > 0) {
                            count--
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
