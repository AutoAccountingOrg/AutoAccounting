/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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

package net.ankio.auto.hooks.wechat.hooks

import android.content.Context
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.api.Hooker
import net.ankio.auto.api.PartHooker

class PayToolsHooker(hooker: Hooker) : PartHooker(hooker){
    override val hookName: String
        get() = "支付方式Hook"

    /**
     * 更底层的hook(未试验，frida调用失败）
     * com.tencent.kinda.framework.module.impl.WXPCommReqResp.getUri
     *  var response = Java.use("com.tencent.kinda.framework.module.impl.WXPCommReqResp");
     *             response.getUri.implementation = function (){
     *                 var uri =   this.getUri();
     *                 var data = this.getWXPReqData();
     *                 console.log("之前",uri,data)
     *                 var mockMgr = this.m_mockMgr.value;
     *                 var loaded_methods = mockMgr.class.getDeclaredMethods();
     *                 console.log(mockMgr,loaded_methods)
     *                 var json = mockMgr.get().requestDataToJson(data)
     *                   console.log("之后",json)
     *                 return uri;
     *             };
     */
    override fun onInit(classLoader: ClassLoader, context: Context) {
      XposedHelpers.findAndHookMethod(
          "com.tencent.kinda.framework.widget.base.MMKRichText",
          classLoader,
          "appendText",
            String::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val text = param.args[0] as String
                    when{
                        text.contains("卡(") || text.contains("零钱通(")  || text.contains("零钱(") -> {
                            logD("支付方式Hook: $text")
                            hooker.hookUtils.writeData("cachedPayTools",text)
                        }
                        text.contains("￥") || text.contains("$")  -> {
                            logD("支付金额Hook: $text")
                            hooker.hookUtils.writeData("cachedPayMoney",text)
                        }
                        text.contains("转账") || text.contains("红包")  -> {
                            logD("支付对象hook: $text")
                            hooker.hookUtils.writeData("cachedPayShop",text)
                        }
                    }
                }
            }
      )
    }
}