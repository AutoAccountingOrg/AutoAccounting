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
           /* Clazz(
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
           Clazz(
                type = "interface",
                name = "onGetAssetsFromApi", // loadAssets()
                methods =
                listOf(
                    ClazzMethod(
                        name = "onGetAssetsFromApi",
                        returnType = "void",
                    ),
                    ClazzMethod(
                        name = "onGetAssetsFromDB",
                        returnType = "void",
                    ),
                ),
            ),
            Clazz(
                type="class",
                name = "AssetContainer", // onGetAssetsFromApi(boolean,f8.f)
                methods =
                listOf(
                    ClazzMethod(
                        name = "getAsset",
                    ),
                    ClazzMethod(
                        name = "getCurrent",
                        returnType = "int",
                    ),
                    ClazzMethod(
                        name = "getEnabled",
                        returnType = "boolean",
                    ),
                ),
            ),
            Clazz(
                type="class",
                name = "AssetPreviewPresenterImplParam1", // u7.h，用于初始化 AssetPreviewPresenterImpl assetPreviewPresenterImpl = new AssetPreviewPresenterImpl(this, this.f15789r0);
                methods =
                listOf(
                    ClazzMethod(
                        name = "onGetAssetsFromApi",
                    ),
                    ClazzMethod(
                        name = "onGetAssetsFromDB",
                    ),
                ),
            ),
            Clazz(
                type = "interface",
                name = "onGetBaoXiaoList",
                nameRule = "^\\w{0,2}\\..+",
                methods =
                listOf(
                    ClazzMethod(
                        name = "onGetList",
                        returnType = "void",
                        parameters =
                        listOf(
                            ClazzField(
                                type = "java.util.List",
                            ),
                        ),
                    ),
                    ClazzMethod(
                        name = "onBaoXiaoFinished",
                        returnType = "void",
                        parameters =
                        listOf(
                            ClazzField(
                                type = "boolean",
                            ),
                        ),
                    ),
                ),
            ),
            Clazz(
                type = "enum",
                name = "filterEnum",
                nameRule = "^\\w{0,2}\\..+",
                fields =
                arrayListOf(
                    ClazzField(
                        name = "ALL",
                    ),
                    ClazzField(
                        name = "HAS",
                    ),
                    ClazzField(
                        name = "NOT",
                    ),
                ),
            ),
            Clazz(
                name = "UserManager",
                nameRule = "^\\w{0,2}\\..+",
                type = "class",
                methods =
                listOf(
                    ClazzMethod(
                        name = "isLogin",
                        returnType = "boolean",
                    ),
                    ClazzMethod(
                        name = "isVip",
                        returnType = "boolean",
                    ),
                    ClazzMethod(
                        name = "isSuperVIP",
                        returnType = "boolean",
                    ),
                ),
            )*/
        )
        set(value) {}



}