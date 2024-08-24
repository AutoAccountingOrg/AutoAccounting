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

package net.ankio.auto.hooks.alipay.hooks

import android.app.Application
import android.content.Context
import android.webkit.ValueCallback
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ankio.auto.constant.DataType
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.api.PartHooker
import net.ankio.auto.utils.AppUtils

class WebViewHooker : PartHooker {

    companion object {
        const val WAIT_TIME = 200L
        const val MAX_CHECK_COUNT = 500
    }

    override fun hook(hookerManifest: HookerManifest, application: Application?,classLoader: ClassLoader)  {
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
                    if (!url.contains("tradeNo="))return
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
                                if (result.isNullOrEmpty() || result.equals("{}" ) || result.equals("null")) {
                                    return@ValueCallback
                                }

                                needWait = false
                                hookerManifest.logD("支付宝WebView页面hook成功，获取到数据：$result")
                                hookerManifest.analysisData(DataType.App, result)
                            }

                        AppUtils.getScope().launch {
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

        XposedHelpers.findAndHookMethod(
            webView, // 目标类名
            "addJavascriptInterface", // 目标方法名
            Object::class.java,
            String::class.java, // 方法参数类型
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    // 在方法执行前拦截
                    val obj = param.args[0]; // 第一个参数是Object obj
                    val str = param.args[1] as String; // 第二个参数是String str
                    // 可以在这里修改参数或者进行其他操作
                    hookerManifest.logD("加载obj: $obj  加载名称：$str")
                }
            },
        )
    }
}
