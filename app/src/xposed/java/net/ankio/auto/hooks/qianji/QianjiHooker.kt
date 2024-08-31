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

package net.ankio.auto.hooks.qianji

import android.app.Application
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.api.PartHooker
import net.ankio.auto.hooks.qianji.hooks.SideBarHooker
import net.ankio.dex.model.Clazz
import net.ankio.dex.model.ClazzField
import net.ankio.dex.model.ClazzMethod

class QianjiHooker:HookerManifest() {
    override val packageName: String
        get() = "com.mutangtech.qianji"
    override val appName: String
        get() = "钱迹"
    override var minVersion: Int = 951
    override fun hookLoadPackage(application: Application?, classLoader: ClassLoader) {

    }

    override var partHookers: MutableList<PartHooker>
        get() = mutableListOf(
            SideBarHooker(),
        )
        set(value) {}
    override var rules: MutableList<Clazz>
        get() = mutableListOf(
            ////////////////////////////////BookManager//////////////////////////////////////
            Clazz(
                name = "BookManager",
                nameRule = "^\\w{0,2}\\..+",
                type = "class",
                methods =
                listOf(
                    ClazzMethod(
                        name = "isFakeDefaultBook",
                        returnType = "boolean",
                        parameters =
                        listOf(
                            ClazzField(
                                type = "com.mutangtech.qianji.data.model.Book",
                            ),
                        ),
                    ),
                    ClazzMethod(
                        name = "getAllBooks",
                        returnType = "java.util.List",
                    ),
                ),
            ),

            ///////////////////////////onGetCategoryList//////////////////////////////////////
            Clazz(
                type = "interface",
                name = "onGetCategoryList",
                nameRule = "^\\w{0,2}\\..+",
                methods =
                listOf(
                    ClazzMethod(
                        name = "onGetCategoryList",
                        returnType = "void",
                        parameters =
                        listOf(
                            ClazzField(
                                type = "java.util.List",
                            ),
                            ClazzField(
                                type = "java.util.List",
                            ),
                            ClazzField(
                                type = "boolean",
                            ),
                        ),
                    ),
                ),
            ),
        )
        set(value) {}



}