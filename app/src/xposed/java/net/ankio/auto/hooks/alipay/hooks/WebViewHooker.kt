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

import android.content.Context
import android.webkit.ValueCallback
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ankio.auto.api.Hooker
import net.ankio.auto.api.PartHooker
import net.ankio.auto.constant.DataType

class WebViewHooker(hooker: Hooker) : PartHooker(hooker) {
    override val hookName: String
        get() = "支付宝WebView页面"

    override fun onInit(
        classLoader: ClassLoader,
        context: Context,
    ) {
        val webViewClazz = " com.alipay.mobile.nebulacore.web.H5WebView"

        val webView = XposedHelpers.findClass(webViewClazz, classLoader)

        XposedHelpers.findAndHookMethod(
            webView,
            "loadUrl",
            String::class.java,
            object :
                XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    if (param != null) {
                        hooker.hookUtils.scope.launch {
                            if (isDebug())
                                {
                                    runOnUiThread {
                                        XposedHelpers.callMethod(param.thisObject, "setWebContentsDebuggingEnabled", true)
                                    }
                                }
                        }
                    }
                }

                override fun afterHookedMethod(param: MethodHookParam) {
                    val url = param.args[0] as String
                    logD("load url $url")
                    val h5WebView = param.thisObject
                    if (url.startsWith("https://66666676.h5app.alipay.com/www/index.html?tradeNo=")) {
                        val number = extractTradeNo(url)
                        val type = extractBizType(url)
                        val stringBuilder = StringBuilder()
                        stringBuilder.append("javascript:(function(){")
                        stringBuilder.append("window.ankioResults = '';")
                        stringBuilder.append("let queryJSON = {")
                        stringBuilder.append("appMode: \"normal\",")
                        stringBuilder.append("tradeNoType: undefined,")
                        stringBuilder.append("bizType: \"$type\",")
                        stringBuilder.append("tradeNo: \"$number\",")
                        stringBuilder.append("gmtCreate: undefined,")
                        stringBuilder.append("useCardStyle: false,")
                        stringBuilder.append("queryOrder: false,")
                        stringBuilder.append("version: \"v1\"")
                        stringBuilder.append("};")
                        stringBuilder.append("let query = {")
                        stringBuilder.append("headers: undefined,")
                        stringBuilder.append("operationType : \"alipay.mobile.bill.QuerySingleBillDetailForH5\",")
                        stringBuilder.append("requestData: [queryJSON]};")
                        stringBuilder.append("AlipayJSBridge.call(\"rpc\",query,function(result){")
                        stringBuilder.append("window.ankioResults = result;")
                        stringBuilder.append("});")
                        stringBuilder.append("})();")
                        hooker.hookUtils.writeData("waitExecDetailInfo", stringBuilder.toString())
                    } else if (url.contains("AlipayJSBridge.call(\"setStartParam\"")) {
                        val waitExecDetailInfo = hooker.hookUtils.readData("waitExecDetailInfo")
                        if (waitExecDetailInfo == "")return
                        hooker.hookUtils.writeData("waitExecDetailInfo", "")
                        XposedHelpers.callMethod(h5WebView, "loadUrl", waitExecDetailInfo)
                        var needWait = true
                        var count = 500

                        val resultCallback =
                            ValueCallback<String> { result ->
                                logD("支付宝WebView获取到数据：$result")
                                if (result.isNullOrEmpty() || result.equals("\"\"")) {
                                    return@ValueCallback
                                }

                                needWait = false
                                logD("支付宝WebView页面hook成功，获取到数据：$result")
                                analyzeData(DataType.App.ordinal, result)
                            }

                        hooker.hookUtils.scope.launch {
                            while (needWait && count > 0) {
                                count--
                                XposedHelpers.callMethod(
                                    h5WebView,
                                    "evaluateJavascript",
                                    "javascript:window.ankioResults",
                                    resultCallback,
                                )
                                delay(200L)
                            }
                        }
                    }
                }
            },
        )
    }

    fun extractTradeNo(url: String): String? {
        val regex = Regex("""tradeNo=([^&]+)""")
        val matchResult = regex.find(url)
        return matchResult?.groupValues?.get(1)
    }

    // bizType
    fun extractBizType(url: String): String? {
        val regex = Regex("""bizType=([^&]+)""")
        val matchResult = regex.find(url)
        return matchResult?.groupValues?.get(1)
    }
}
