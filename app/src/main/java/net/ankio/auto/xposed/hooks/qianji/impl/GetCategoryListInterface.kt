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

package net.ankio.auto.xposed.hooks.qianji.impl

import net.ankio.auto.xposed.core.api.HookerClazz
import net.ankio.dex.model.Clazz
import net.ankio.dex.model.ClazzField
import net.ankio.dex.model.ClazzMethod

class GetCategoryListInterface {
    companion object : HookerClazz() {
        override var rule = Clazz(
            name = this::class.java.name,
            nameRule = "^\\w{0,2}\\..+",
            type = "interface",
            methods = listOf(
                ClazzMethod(
                    name = "onGetCategoryList",
                    returnType = "void",
                    parameters = listOf(
                        ClazzField(type = "java.util.List"),
                        ClazzField(type = "java.util.List"),
                        ClazzField(type = "boolean"),
                    ),
                ),
            ),
        )

    }
}