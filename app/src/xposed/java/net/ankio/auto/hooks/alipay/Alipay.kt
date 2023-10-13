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
import de.robv.android.xposed.XposedBridge
import net.ankio.auto.api.Hooker
import net.ankio.auto.api.PartHooker
import net.ankio.auto.hooks.alipay.hooks.SettingUIHooker
import net.ankio.auto.hooks.android.AccountingService

class Alipay:Hooker() {
    override val packPageName: String = "com.eg.android.AlipayGphone"
    override val appName: String = "支付宝"
    override val needHelpFindApplication: Boolean = true
    override var partHookers: MutableList<PartHooker> = arrayListOf(
        SettingUIHooker(this)
    )

    override fun hookLoadPackage(classLoader: ClassLoader?, context: Context?) {

    }
}