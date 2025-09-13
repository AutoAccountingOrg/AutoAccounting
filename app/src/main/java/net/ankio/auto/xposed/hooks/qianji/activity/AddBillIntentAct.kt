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

package net.ankio.auto.xposed.hooks.qianji.activity

import android.content.Intent
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.core.api.HookerClazz
import net.ankio.auto.xposed.hooks.qianji.models.AutoTaskLogModel
import net.ankio.dex.model.Clazz
import net.ankio.dex.model.ClazzField
import net.ankio.dex.model.ClazzMethod

class AddBillIntentAct(private val obj: Any) {
    companion object : HookerClazz() {
        const val CLAZZ = "com.mutangtech.qianji.bill.auto.AddBillIntentAct"
        override var rule = Clazz(
            name = this::class.java.name,
            nameRule = CLAZZ,
            type = "class",
            methods =
                listOf(
                    ClazzMethod(
                        findName = "insertAutoTask",
                        parameters =
                            listOf(
                                ClazzField(
                                    type = "java.lang.String",
                                ),
                                ClazzField(
                                    type = AutoTaskLogModel.CLAZZ,
                                ),
                            ),
                        regex = "^\\w{2}$",
                    ),
                    ClazzMethod(
                        findName = "doIntent",
                        parameters =
                            listOf(
                                ClazzField(
                                    type = Intent::class.java.name,
                                ),
                            ),
                        regex = "^\\w{2}$",
                        strings = listOf(
                            "intent-data:",
                            "auto_task_last_time",
                            "getString(...)",
                        ),
                    )
                )
        )

        fun fromObj(obj: Any): AddBillIntentAct {
            if (obj::class.java.name != CLAZZ) error("${obj::class.java.name} is not $CLAZZ")
            return AddBillIntentAct(obj)
        }

        fun doIntent() = method("doIntent")

        fun insertAutoTask() = method("insertAutoTask")
    }

    fun finishAffinity() {
        XposedHelpers.callMethod(obj, "finishAffinity")
    }
}