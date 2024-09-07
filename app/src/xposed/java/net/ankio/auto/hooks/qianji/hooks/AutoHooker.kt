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

package net.ankio.auto.hooks.qianji.hooks

import android.app.Application
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.api.PartHooker
import net.ankio.dex.model.ClazzField
import net.ankio.dex.model.ClazzMethod

class AutoHooker:PartHooker() {
    lateinit var  addBillIntentAct: Class<*>
    override val methodsRule: MutableList<Triple<String, String, ClazzMethod>>
        get() = mutableListOf(
            Triple("com.mutangtech.qianji.bill.auto.AddBillIntentAct","InsertAutoTask",
                ClazzMethod(
                    parameters =
                    listOf(
                        ClazzField(
                            type = "java.lang.String",
                        ),
                        ClazzField(
                            type = "com.mutangtech.qianji.data.model.AutoTaskLog",
                        ),
                    ),
                    regex = "^\\w{2}$",
                    modifiers = "private static final",
                ),
            )
        )
    override fun hook(
        hookerManifest: HookerManifest,
        application: Application?,
        classLoader: ClassLoader
    ) {

        addBillIntentAct = classLoader.loadClass("com.mutangtech.qianji.bill.auto.AddBillIntentAct")

        hookTimeout(hookerManifest)

        hookTaskLog(hookerManifest,classLoader)
    }

    private fun hookTaskLog(hookerManifest: HookerManifest,classLoader: ClassLoader){

    }

    /*
    * hookTimeout
     */
    private fun hookTimeout(hookerManifest: HookerManifest){

        XposedHelpers.setStaticLongField(addBillIntentAct, "FREQUENCY_LIMIT_TIME", 0L)

        hookerManifest.logD("hookTimeout =${XposedHelpers.getStaticLongField(addBillIntentAct, "FREQUENCY_LIMIT_TIME")}")
    }


}