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

package net.ankio.auto.hooks.setting

import android.content.Context
import net.ankio.auto.api.Hooker
import net.ankio.auto.api.PartHooker
import net.ankio.auto.hooks.auto.hooks.ActiveHooker

class SettingHooker: Hooker() {
    override val packPageName: String
        get() = "com.android.settings"
    override val appName: String
        get() = "Android Settings"

    override var partHookers: MutableList<PartHooker> = arrayListOf(
        ServerHooker(this)
    )

    override fun hookLoadPackage(classLoader: ClassLoader, context: Context) {

    }


}