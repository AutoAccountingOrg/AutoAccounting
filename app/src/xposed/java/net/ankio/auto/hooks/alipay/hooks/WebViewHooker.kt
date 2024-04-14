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
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.api.Hooker
import net.ankio.auto.api.PartHooker
import net.ankio.auto.constant.DataType
import net.ankio.auto.utils.AppUtils
import org.json.JSONObject

class WebViewHooker(hooker: Hooker) : PartHooker(hooker) {
    override val hookName: String
        get() = "支付宝WebView页面"

    private val PREFIX = "javascript:(function(){if(typeof AlipayJSBridge === 'object'){AlipayJSBridge._invokeJS("

    override fun onInit(
        classLoader: ClassLoader,
        context: Context,
    ) {
        hooker.hookUtils.scope.launch {
            val webview = loadClass("com.alipay.mywebview.sdk.WebView")
            val callback = loadClass("com.alipay.mywebview.sdk.ValueCallback")
            /**
             * javascript:(function(){if(typeof AlipayJSBridge === 'object'){AlipayJSBridge._invokeJS("{\"clientId\":\"17116867375600.4874238795104955\",\"func\":\"rpc\",\"keepCallback\":false,\"msgType\":\"callback\",\"param\":{\"ariverRpcTraceId\":\"client`ZU4sULl9g50DAKQFbhdkTPuoOw8Ujwd_5560142\",\"buttons\":[],\"code\":0,\"degradeCardFromMrchorder\":false,\"extension\":{\"bizInNo\":\"20240326315575111721\",\"bizType\":\"MINITRANS\",\"gmtBizCreateTime\":\"1711393834000\",\"mdata\":\"{\\\"conbiz_biztype\\\":\\\"MINITRANS\\\",\\\"conbiz_bizinno\\\":\\\"20240326315575111721\\\",\\\"conbiz_bizsubtype\\\":\\\"8041\\\"}\"},\"fields\":[{\"locationPage\":\"main\",\"templateId\":\"BLDetailTitle\",\"type\":\"birdNest\",\"value\":\"{\\\"goto\\\":\\\"alipays://platformapi/startapp?appId=20000032&isOpenedFund=true\\\",\\\"icon\\\":\\\"https://gw.alipayobjects.com/mdn/rms_b03cca/afts/img/A*YczITbg-JhMAAAAAAAAAAAAAARQnAQ\\\",\\\"content\\\":\\\"长城基金管理有限公司\\\"}\"},{\"locationPage\":\"main\",\"templateId\":\"BLDetailPrice\",\"type\":\"birdNest\",\"value\":\"{\\\"amount\\\":\\\"0.01\\\",\\\"status\\\":\\\"交易成功\\\"}\"},{\"locationPage\":\"main\",\"templateId\":\"BLDetailCommon\",\"type\":\"birdNest\",\"value\":\"{\\\"data\\\":[{\\\"content\\\":\\\"2024-03-26 03:10:34\\\",\\\"inOut\\\":0,\\\"needVerify\\\":false}],\\\"title\\\":\\\"创建时间\\\"}\"},{\"locationPage\":\"main\",\"templateId\":\"BLH5ProductInfo\",\"type\":\"birdNest\",\"value\":\"{\\\"data\\\":[{\\\"content\\\":\\\"余额宝-2024.03.25-收益发放\\\",\\\"goUrlTitle\\\":\\\"查看收益明细\\\",\\\"goto\\\":\\\"alipays://platformapi/startapp?appClearTop=false&appId=68687356&startMultApp=YES&url=%2Fwww%2Fmy.html%3Ffrom%3DtotalProfit%26enableWK%3DYES\\\",\\\"inOut\\\":0,\\\"needVerify\\\":false,\\\"seed\\\":\\\"billDetailProductInfo\\\"}],\\\"title\\\":\\\"商品说明\\\"}\"},{\"locationPage\":\"main\",\"templateId\":\"BLDetailCommon\",\"type\":\"birdNest\",\"value\":\"{\\\"data\\\":[{\\\"content\\\":\\\"长城基金管理有限公司\\\",\\\"inOut\\\":0,\\\"needVerify\\\":false}],\\\"title\\\":\\\"对方账户\\\"}\"},{\"locationPage\":\"main\",\"templateId\":\"BLDetailCommon\",\"type\":\"birdNest\",\"value\":\"{\\\"data\\\":[{\\\"content\\\":\\\"20240326315575111721\\\",\\\"inOut\\\":0,\\\"needVerify\\\":false}],\\\"title\\\":\\\"订单号\\\"}\"},{\"locationPage\":\"main\",\"templateId\":\"BLDetailBlankArea\",\"type\":\"birdNest\"},{\"locationPage\":\"main\",\"templateId\":\"BLNewCategory\",\"type\":\"birdNest\",\"value\":\"{\\\"content\\\":\\\"投资理财\\\",\\\"inOut\\\":0,\\\"needVerify\\\":false,\\\"seed\\\":\\\"billLifeCategory\\\",\\\"title\\\":\\\"账单分类\\\"}\"},{\"locationPage\":\"main\",\"templateId\":\"BLTagAndEssay\",\"type\":\"birdNest\",\"value\":\"{\\\"data\\\":[{\\\"content\\\":\\\"\\\",\\\"goto\\\":\\\"alipays://platformapi/startapp?appId=66666698&url=%2Fwww%2Findex.html%3FgmtBizCreate%3D1711393834000%26bizInNo%3D20240326315575111721%26bizType%3DMINITRANS\\\",\\\"inOut\\\":0,\\\"needVerify\\\":false,\\\"seed\\\":\\\"billTagAndEssay\\\",\\\"title\\\":\\\"标签和备注\\\"}]}\"},{\"locationPage\":\"main\",\"templateId\":\"BLStatManage\",\"type\":\"birdNest\",\"value\":\"{\\\"content\\\":\\\"1\\\",\\\"inOut\\\":0,\\\"needVerify\\\":false,\\\"seed\\\":\\\"countswitch\\\",\\\"title\\\":\\\"计入收支\\\"}\"},{\"locationPage\":\"main\",\"templateId\":\"BLDetailLink\",\"type\":\"birdNest\",\"value\":\"{\\\"data\\\":[{\\\"goto\\\":\\\"alipays://platformapi/startapp?appId=77700256&query=scene%3dmbill_tips%26bizNo%3d20240326315575111721\\\",\\\"inOut\\\":0,\\\"infoLevel\\\":\\\"3\\\",\\\"needVerify\\\":false,\\\"seed\\\":\\\"selfHelp\\\",\\\"title\\\":\\\"对此订单有疑问\\\",\\\"titleLogo\\\":\\\"https://mdn.alipayobjects.com/huamei_6cknvu/afts/img/A*JprOQZFp2u8AAAAAAAAAAAAADoWDAQ/original\\\"}]}\"}],\"logisticsModel\":{\"nodes\":[]},\"needDisplayCardStyle\":true,\"needDisplayConsumeRate\":true,\"refundModel\":{\"fundBatch\":[]},\"showAuthPage\":false,\"succ\":true,\"switchNewVersion\":true}}")}})();
             */

            withContext(Dispatchers.Main) {
                XposedHelpers.findAndHookMethod(
                    webview,
                    "evaluateJavascript",
                    String::class.java,
                    callback,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam?) {
                            super.afterHookedMethod(param)
                            val js = param?.args?.get(0) as String

                            //      if(!js.trim().startsWith(PREFIX))return
                            //   if(!js.contains("\\\"func\\\":\\\"pause\\\""))return
                            //      if(!js.contains("func") && !js.contains("rpc") && !js.contains("param"))return
                            logD("支付宝WebView页面hook数据:$js")
                            hooker.hookUtils.scope.launch {
                                val json = getJSON(js)
                                if (json !== null) {
                                    val func = json.getString("func")
                                    if (func.equals("rpc")) {
                                        val alipayParam = json.getJSONObject("param")
                                        // 分析支付宝数据
                                        log("分析支付宝数据:$alipayParam")
                                        analyzeData(DataType.App.ordinal, alipayParam.toString())
                                    }
                                }
                            }
                        }
                    },
                )
            }
        }
    }

    suspend fun getJSON(js: String): JSONObject? {
        var code = js.trim().removePrefix(PREFIX).removeSuffix(")}})();")
        code = "print(JSON.stringify(JSON.parse($code)))"
        return runCatching { JSONObject(AppUtils.getService().js(code)) }.getOrNull()
    }
}
