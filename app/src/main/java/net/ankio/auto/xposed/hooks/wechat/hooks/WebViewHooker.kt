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

package net.ankio.auto.xposed.hooks.wechat.hooks

import com.google.gson.Gson
import com.google.gson.JsonObject
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AppRuntime
import org.ezbook.server.tools.MD5HashTable
import org.ezbook.server.constant.DataType

class WebViewHooker:PartHooker() {
    // javascript:WeixinJSBridge._handleMessageFromWeixin({"__json_message":{"__msg_type":"callback","__callback_id":"1049","__params":{"err_msg":"nativeWXPayCgiTunnel:ok","respbuf":"{\"ret_code\":0,\"ret_msg\":\"ok\",\"header\":{\"logo_url\":\"http:\\\/\\\/wx.qlogo.cn\\\/mmhead\\\/NhPIuUBkmFSFZWsibdTFblLMT2KsKNu7qAsJk6mV6icREfic6nMOU7bgBYSQ3cmCnbl6lCbvyhQ4Zk\\\/64\",\"nickname\":\"转账-来自微信支付\",\"fee\":\"+0.01\"},\"preview\":[{\"separate\":\"solid\",\"value\":[],\"sequence\":[]},{\"label\":{\"name\":\"当前状态\",\"actionsheet\":[],\"sub_text\":[]},\"value\:\"已存入零钱\",\"actionsheet\":[],\"sub_text\":[]}],\"sequence\":[]},{\"label\":{\"name\":\"转账说明\",\"actionsheet\":[],\"sub_text\":[]},\"value\":[{\"name\":\"微信转账\",\"actionsheet\":[],\"sub_text\":[]}],[]},{\"label\":{\"name\":\"转账时间\",\"actionsheet\":[],\"sub_text\":[]},\"value\":[{\"name\":\"1728568091\",\"actionsheet\":[],\"is_timestamp\":true,\"sub_text\":[]}],\"sequence\":[]},{\"label\":{\"name\":\"收时间\",\"actionsheet\":[],\"sub_text\":[]},\"value\":[{\"name\":\"1728569608\",\"actionsheet\":[],\"is_timestamp\":true,\"sub_text\":[]}],\"sequence\":[]},{\"label\":{\"name\":\"转账单号\",\"actionsheet\":[],\"st\":[]},\"value\":[{\"name\":\"1000050001202410100726623484903\",\"actionsheet\":[],\"sub_text\":[]}],\"sequence\":[]}],\"entrances\":[{\"name\":\"定位到聊天位置\",\"event\":{\"name\":\"openWCPaySpecificView\",\\":[{\"key\":\"appId\",\"value\":\"wx57849631bb367f52\"},{\"key\":\"nonceStr\",\"value\":\"iImn80YEPCJIpW9M1QvC6Btu6p7M2gY1\"},{\"key\":\"package\",\"value\":\"openview=open_wcpay_c2c_message_view&bizId=1000050001202410100726623484903&bizType=1&username=wxid_6x4zob0gltzw22&createTime=1728569607\"},{\"key\":\"timeStamp\",\"value\":\"1733669561\"},{\"key\":\"signType\",\"value\":\"SHA1\"},{\"key\":\"paySign\",\"value\":\"63bed7423ceb6c4eb0b69c85cb967f6a620cb5fb\"}],\"need_list\":[]},\"actionsheet\":[],\"sub_text\":[]},{\"name\":\"申请转账电子凭证\",\"link_btn\":true,\"event\":{\"name\":\"openUrlWithExtraWebview\",\"params\":[{\"openType\",\"value\":\"1\"},{\"key\":\"url\",\"value\":\"https:\\\/\\\/payapp.wechatpay.cn\\\/transferbillcertificate\\\/jumphomepage?outtradeno=1000050001202410100726623484903&amount=1&scene=0#wechat_pay&wechat_redirect\"}],\"need_list\":[]},\"actionsheet\":[],\"domain\":12,\"sub_text\":[]},{\"name\":\"查看往来转账\",\"link_btn\":true,\"event\":{\"name\":\"openUrlWithExtraWebview\",\"params\":[{\"key\":\"openType\",\\":\"1\"},{\"key\":\"url\",\"value\":\"https:\\\/\\\/payapp.wechatpay.cn\\\/transfertochange\\\/transfercontactsrecords\\\/jumphomepage?outtradeno=1000050001202410100726623484903&peer_openid=oX2-vjixUZt7r2PyY7tjOf_bKf_A&scene=0#wechat_pay&wechat_redirect\"}],\"need_list\":[]},\"actionsheet\":[],\"domain\":15,\"sub_text\":[]}],\"footer\":[],\"v2flag\":1,\"scene\":0,\"banner\":{\"content\":\"\"},\"v3_flag\":1,\"service_module\":[{\"area_id\":2,\"area_name\":\"账单服务\",\"area_type\":2,\"services\":[{\"service_id\":7,\"name\":\"定位到聊天位置\",\"jump_type\":2,\"event\":{\"name\":\"openWCPaySpecificView\",\"params\":[{\"key\":\value\":\"wx57849631bb367f52\"},{\"key\":\"nonceStr\",\"value\":\"iImn80YEPCJIpW9M1QvC6Btu6p7M2gY1\"},{\"key\":\"package\",\"value\":\"openview=open_wcpay_c2c_message_view&bizId=1000050001202410100726623484903&bizType=1&username=wxid_6x4zob0gltzw22&createTime=1728569607\"},{\"key\":\"timeStamp\",\"value\":\"1733669561\"},{\"key\":\"signType\",\"value\":\"SHA1\"},{\"key\":\"paySign\",\"value\":\"63bed7423ceb6c4eb0b69c85cb967f6a620cb5fb\"}],\"need_list\":[]},\"actionsheet\":[],\"link_btn\":false,\"red_dot\":{\"show_red_dot\":false}},{\"service_id\":8,\"name\":\"申请转账电子凭证\",\"jump_type\":2,\"event\":{\"name\":\"openUrlWitbview\",\"params\":[{\"key\":\"openType\",\"value\":\"1\"},{\"key\":\"url\",\"value\":\"https:\\\/\\\/payapp.wechatpay.cn\\\/transferbillcertificate\\\/jumphomepage?outtradeno=1000050001202410100726623484903&amount=1&scene=0#wechat_pay&wechat_redirect\"}],\"need_list\":[]},\"actionsheet\":[],\"link_btn\":false,\"red_dot\":{\"show_red_dot\":false}},{\"service_id\":9,\"name\":\"查看往来转账\",\"jump_type\":2,\"event\":{\":\"openUrlWithExtraWebview\",\"params\":[{\"key\":\"openType\",\"value\":\"1\"},{\"key\":\"url\",\"value\":\"https:\\\/\\\/payapp.wechatpay.cn\\\/transfertochange\\\/transfercontactsrecords\\\/jumphomepage?outtradeno=1000050001202410100726623484903&peer_openid=oX2-vjixUZt7r2PyY7tjOf_bKf_A&scene=0#wechat_pay&wechat_redirect\"}],\"need_list\":[]},\"actionsheet\":[],\"link_btn\":false,\"red_dot\":{\"show_red_dot\":false}}]}]}"}},"__sha_key":"8a1c6c62a9cee71134fa0fc85739b6ea6fcc16fb"})
   //  Called com.tencent.xweb.WebView.evaluateJavascript(java.lang.String, android.webkit.ValueCallback)
    //(agent) [610089] Arguments com.tencent.xweb.WebView.evaluateJavascript(j
    //
    private val md5HashTable = MD5HashTable()
    override fun hook() {
       Hooker.after("com.tencent.xweb.WebView", "evaluateJavascript", String::class.java, android.webkit.ValueCallback::class.java) {
           val json = it.args[0] as String
           if (json.contains("nativeWXPayCgiTunnel:ok")) {
              // val index = json.indexOf("javascript:WeixinJSBridge._handleMessageFromWeixin({")
               val jsonStr = json.substring("javascript:WeixinJSBridge._handleMessageFromWeixin(".length, json.length - 1)
               //AppRuntime.manifest.logD("Wechat WebViewHooker hook： $jsonStr")
              kotlin.runCatching {
                  val jsonObject = Gson().fromJson(jsonStr, JsonObject::class.java)
                  val params = jsonObject.getAsJsonObject("__json_message").getAsJsonObject("__params")
                  val respbuf = params.get("respbuf").asString
                  var __json = Gson().fromJson(respbuf, JsonObject::class.java)
                  //删除entrances节点
                  __json.remove("entrances")
                  __json.remove("service_module")
                  val md5 = MD5HashTable.md5(__json.toString())
                  AppRuntime.manifest.logD("")
                  if (md5HashTable.contains(md5)){
                      AppRuntime.manifest.logD("Wechat WebViewHooker exist： $__json")
                     return@runCatching
                  }
                  md5HashTable.put(md5)
                  AppRuntime.manifest.logD("Wechat WebViewHooker hook： $__json")
                  analysisData(DataType.DATA, __json.toString())
              }.onFailure { e ->
                    AppRuntime.manifest.logE(e)
              }
           }
       }
    }
}