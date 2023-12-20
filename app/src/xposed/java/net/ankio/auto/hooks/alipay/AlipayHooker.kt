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

package net.ankio.auto.hooks.alipay

import android.content.Context
import net.ankio.auto.api.Hooker
import net.ankio.auto.api.PartHooker
import net.ankio.auto.hooks.alipay.hooks.MessageBoxHooker
import net.ankio.auto.hooks.alipay.hooks.RedPackageHooker
import net.ankio.auto.hooks.alipay.hooks.SettingUIHooker

class AlipayHooker:Hooker() {
    override val packPageName: String = "com.eg.android.AlipayGphone"
    override val appName: String = "支付宝"
    override val needHelpFindApplication: Boolean = true
    override var partHookers: MutableList<PartHooker> = arrayListOf(
        SettingUIHooker(this),//支付宝设置
        MessageBoxHooker(this),//支付消息盒子
        RedPackageHooker(this),//支付宝红包
    )

    override fun hookLoadPackage(classLoader: ClassLoader?, context: Context?) {
    }
}