/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

package net.ankio.auto.xposed.hooks.qq

import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.dex.model.Clazz

class QQHooker : HookerManifest() {
    override val packageName: String
        get() = "com.tencent.mobileqq"
    override val appName: String
        get() = "QQ"

    override var applicationName: String
        get() = "com.tencent.mobileqq.qfix.QFixApplication"
        set(value) {}

    override fun hookLoadPackage() {

    }

    override var partHookers: MutableList<PartHooker>
        get() = mutableListOf()
        set(value) {}
    override var rules: MutableList<Clazz>
        get() = mutableListOf()
        set(value) {}

}