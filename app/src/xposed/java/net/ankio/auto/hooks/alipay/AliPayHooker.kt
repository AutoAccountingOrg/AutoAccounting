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

import android.app.Application
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.api.PartHooker
import net.ankio.auto.hooks.alipay.hooks.MessageBoxHooker
import net.ankio.auto.hooks.alipay.hooks.RedPackageHooker
import net.ankio.auto.hooks.alipay.hooks.WebViewHooker
import net.ankio.dex.model.Clazz

class AliPayHooker : HookerManifest() {
    override val packageName: String
        get() = "com.eg.android.AlipayGphone"
    override val appName: String = "支付宝"
    override fun hookLoadPackage(application: Application?, classLoader: ClassLoader) {

    }

    override var applicationName: String = "com.alipay.mobile.quinox.LauncherApplication"
    override var partHookers: MutableList<PartHooker> = mutableListOf(
        MessageBoxHooker(),//支付消息盒子
        RedPackageHooker(),//支付宝红包
        WebViewHooker(),//支付宝webview
    )
    override var rules: MutableList<Clazz>
        get() = mutableListOf()
        set(value) {}


}