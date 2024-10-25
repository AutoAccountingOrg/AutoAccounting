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
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.delay
import net.ankio.auto.xposed.core.App
import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.utils.ThreadUtils
import org.ezbook.server.constant.DataType

class WebViewHooker : PartHooker() {

    companion object {
        const val WAIT_TIME = 200L
        const val MAX_CHECK_COUNT = 500
    }

    override fun hook(
        hookerManifest: HookerManifest,
        application: Application?,
        classLoader: ClassLoader
    ) {
        val webViewClazz = " com.alipay.mobile.nebulacore.web.H5WebView"

        val webView = XposedHelpers.findClass(webViewClazz, classLoader)

        XposedHelpers.findAndHookMethod(
            webView,
            "evaluateJavascript",
            String::class.java,
            ValueCallback::class.java,
            object :
                XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val script = param.args[0] as String
                    val obj = param.thisObject

                    val urlObj = XposedHelpers.callMethod(obj, "getUrl") ?: return
                    val url = urlObj as String
                    if (!url.contains("tradeNo=")) return
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
                                hookerManifest.logD("Hooked Alipay Bill List Data：$result")
                                hookerManifest.analysisData(DataType.DATA, result)
                            }

                        ThreadUtils.launch {
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
            },
        )

    }
}
