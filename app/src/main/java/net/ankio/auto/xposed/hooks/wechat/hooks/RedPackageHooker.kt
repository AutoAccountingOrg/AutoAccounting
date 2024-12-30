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

import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.DataUtils
import org.ezbook.server.constant.DataType
import org.json.JSONObject


class RedPackageHooker: PartHooker() {
    override fun hook() {
        /**
         * (agent) [981950] Arguments com.tencent.mm.plugin.luckymoney.model.n5.onGYNetEnd((none), ok, {"retcode":0,"retmsg":"ok","sendId":"1000039801202409266295087127052","amount":1,"recNum":1,"recAmount":1,"totalNum":1,"totalAmount":1,"hasWriteAnswer":0,"hbType":0,"isSender":0,"receiveStatus":2,"hbStatus":4,"statusMess":"","wishing":"恭喜发财，大吉大利","receiveId":"100003980100040926629508712adTitle":"","canShare":0,"operationHeader":[],"record":[{"receiveAmount":1,"receiveTime":"1727339015","answer":"","receiveId":"1000039801000409266295087127052","state":1,"receiveOpenId":"wxid_8mrswdkrtbqm22","userName":"wxid_8mrswdkrtbqm22"}],"watermark":"","jumpChange":1,"changeWording":"已存入零钱，可直接消费","sendUserName":"wxid_6x4zob0gltzw22","changeUrl":"weixin:\/\/wxpay\/changee_info":{"guide_flag":0,"guide_wording":"","left_button_wording":"","right_button_wording":"","upload_credit_url":""},"SystemMsgContext":"<img src=\"SystemMessages_HongbaoIcon.png\"\/>  你领取了d_6x4zob0gltzw22$的<_wc_custom_link_ color=\"#FD9931\" href=\"weixin:\/\/weixinhongbao\/opendetail?sendid=1000039801202409266295087127052&sign=ed6d64b2502301cf5b179454e80cc149d4863ee732d3dff06ad7fd49e4755d1f26fd6d6001581fab898c5932023f9f2915f6733c04a333cc786fbbd1933396752d0d678788cdda7fcc1d21f780c18893&ver=6\">红包<\/_wc_custom_link_>","sessionUserName":"wxid_6x4zob0gltzw22","jumpChangype":1,"changeIconUrl":"","expression_md5":"","expression_type":0,"showYearExpression":1,"showOpenNormalExpression":1,"enableAnswerByExpression":1,"enableAnswerBySelfie":0,"enable_set_status":false})
         *
         *
         *
         */
        val clazz = AppRuntime.manifest.clazz("luckymoney.model")
        //  public void onGYNetEnd(int v, String s, JSONObject jSONObject0) {
        Hooker.before(clazz,
            "onGYNetEnd",
            Int::class.javaPrimitiveType!!,
            String::class.java,
            JSONObject::class.java
        ){ param ->
            val json = param.args[2] as JSONObject
            AppRuntime.manifest.logD("hooked red package: $json")
            json.put(ChatUserHooker.CHAT_USER, ChatUserHooker.get(json.getString("sendUserName")))
            AppRuntime.manifest.analysisData(DataType.DATA,json.toString())
        }
    }
}