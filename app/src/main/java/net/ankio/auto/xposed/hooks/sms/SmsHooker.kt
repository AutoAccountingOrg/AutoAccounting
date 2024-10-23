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

package net.ankio.auto.xposed.hooks.sms

import android.app.Application
import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.hooks.sms.hooks.SmsIntentHooker
import net.ankio.dex.model.Clazz

class SmsHooker: HookerManifest() {
    override val packageName: String
        get() = "com.android.phone"
    override val appName: String
        get() = "短信"

    override fun hookLoadPackage(application: Application?, classLoader: ClassLoader) {

    }

    override var partHookers: MutableList<PartHooker>
        get() = mutableListOf(
            SmsIntentHooker()
        )
        set(value) {}
    override var rules: MutableList<Clazz>
        get() = mutableListOf()
        set(value) {}
}