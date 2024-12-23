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

package net.ankio.auto.xposed

import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.hooks.alipay.AliPayHooker
import net.ankio.auto.xposed.hooks.auto.AutoHooker
import net.ankio.auto.xposed.hooks.qianji.QianjiHooker
import net.ankio.auto.xposed.hooks.wechat.WechatHooker
import net.ankio.auto.xposed.hooks.wechat.WechatPushHooker


object Apps {

    fun get(): MutableList<HookerManifest> {
        return mutableListOf(

           // AndroidHooker(), // Android
            AutoHooker(), // Auto
            ////////////////////////////
            // 记账App hook
            ////////////////////////////
            QianjiHooker(),
            ////////////////////////////
            // 哪些App可能发送记账账单？
            ////////////////////////////
            WechatHooker(), // Wechat
            WechatPushHooker(), // Server
            AliPayHooker(), // AliPay
           // SmsHooker() // Sms
            ////////////////////////////
        )
    }
}