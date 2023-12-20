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

package net.ankio.auto.hooks.auto

import android.content.Context
import net.ankio.auto.api.Hooker
import net.ankio.auto.api.PartHooker
import net.ankio.auto.hooks.auto.hooks.ActiveHooker


class AutoHooker: Hooker(){
    override val packPageName: String = "net.ankio.auto.xposed"
    override val appName: String = "自动记账"
    override val needHelpFindApplication: Boolean = true
    override var partHookers: MutableList<PartHooker> = arrayListOf(
        ActiveHooker(this)
    )

    override fun hookLoadPackage(classLoader: ClassLoader?, context: Context?) {
        hookUtils.logD(appName,"欢迎使用自动记账，该日志表示 $appName 已被hook")
    }
}
