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

package net.ankio.auto.xposed.hooks.wechat.models

import net.ankio.auto.xposed.core.api.HookerClazz
import net.ankio.dex.model.Clazz
import net.ankio.dex.model.ClazzField
import net.ankio.dex.model.ClazzMethod

class LuckMoneyModel {
    companion object : HookerClazz() {
        override var rule = Clazz(
            type = "class",
            name = this::class.java.name,
            nameRule = "com.tencent.mm.plugin.luckymoney.model.\\w+",
            methods = arrayListOf(
                ClazzMethod(
                    name = "constructor",
                    parameters = arrayListOf(
                        // public n5(int v, int v1, String s, String s1, String s2, String s3, String s4, String s5, String s6, String s7) {
                        //
                        ClazzField(
                            type = "int"
                        ),
                        ClazzField(
                            type = "int"
                        ),
                        ClazzField(
                            type = "java.lang.String"
                        ),
                        ClazzField(
                            type = "java.lang.String"
                        ),
                        ClazzField(
                            type = "java.lang.String"
                        ),
                        ClazzField(
                            type = "java.lang.String"
                        ),
                        ClazzField(
                            type = "java.lang.String"
                        ),
                        ClazzField(
                            type = "java.lang.String"
                        ),
                        ClazzField(
                            type = "java.lang.String"
                        ),
                        ClazzField(
                            type = "java.lang.String"
                        ),
                    )
                ),

                ClazzMethod(
                    name = "onGYNetEnd",
                    returnType = "void",
                    parameters = arrayListOf(
                        ClazzField(
                            type = "int"
                        ),
                        ClazzField(
                            type = "java.lang.String"
                        ),
                        ClazzField(
                            type = "org.json.JSONObject"
                        ),
                    )

                )
            )
        )
    }
}