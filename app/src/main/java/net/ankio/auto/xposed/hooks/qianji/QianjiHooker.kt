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

package net.ankio.auto.xposed.hooks.qianji

import android.content.Intent
import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.hooks.qianji.hooks.AutoHooker
import net.ankio.auto.xposed.hooks.qianji.hooks.SideBarHooker
import net.ankio.dex.model.Clazz
import net.ankio.dex.model.ClazzField
import net.ankio.dex.model.ClazzMethod

class QianjiHooker : HookerManifest() {
    override val packageName: String
        get() = "com.mutangtech.qianji"
    override val appName: String
        get() = "钱迹"
    override var minVersion: Int = 951

    override var applicationName = "com.mutangtech.qianji.app.CoreApp"
    override fun hookLoadPackage() {

    }

    override var partHookers: MutableList<PartHooker>
        get() = mutableListOf(
            SideBarHooker(),
            AutoHooker(),
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

            ///////////////////////////UserManager//////////////////////////////////////
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
            ),
            ///////////////////////////Timeout//////////////////////////////////////
            Clazz(
                name = "TimeoutApp",
                nameRule = "^\\w{0,2}\\..+",
                type = "class",
                methods =
                listOf(
                    ClazzMethod(
                        name = "timeoutApp",
                        returnType = "boolean",
                    ),
                    ClazzMethod(
                        name = "timeoutUser",
                        returnType = "boolean",
                    ),
                    ClazzMethod(
                        name = "setTimeOutApp",
                        returnType = "boolean",
                    ),
                ),
            ),
            ///////////////////////////AssetInsert//////////////////////////////////////
            Clazz(
                name = "AssetDbHelper",//com.mutangtech.qianji.data.db.dbhelper
                nameRule = "com.mutangtech.qianji.data.db.\\w+.\\w+",
                type = "class",
                methods =
                listOf(
                    ClazzMethod(
                        name = "insertOrReplace",
                        returnType = "boolean",
                    ),
                    ClazzMethod(
                        name = "saveLoanList",
                        returnType = "boolean",
                    ),
                    ClazzMethod(
                        name = "updateOrders",
                        returnType = "boolean",
                    ),
                ),
            ),
            Clazz(
                name = "BillDbHelper",
                nameRule = "com.mutangtech.qianji.data.db.dbhelper.\\w+",
                type = "class",
                methods =
                listOf(
                    ClazzMethod(
                        name = "saveOrUpdateBill",
                        returnType = "boolean",
                    ),
                    ClazzMethod(
                        name = "saveSyncedResult",
                        returnType = "void",
                    ),
                ),
            ),
            //////////////////////钱迹BillTools////////////////////////////////////////
            Clazz(
                name = "BillTools",
                nameRule = "^\\w{0,2}\\..+",
                type = "class",
                methods =
                listOf(
                    ClazzMethod(
                        name = "deleteBook",
                        returnType = "void",
                    ),
                    ClazzMethod(
                        name = "getUnPushCount",
                    ),
                ),
            ),
            //////////////////////钱迹RequestInterface////////////////////////////////////////
            Clazz(
                name = "RequestInterface",
                nameRule = "^\\w{0,2}\\..+",
                type = "class",
                methods =
                listOf(
                    ClazzMethod(
                        name = "onExecuteRequest",
                        returnType = "void",
                    ),
                    ClazzMethod(
                        name = "onFinish",
                    ),
                    ClazzMethod(
                        name = "onToastMsg",
                    ),
                    ClazzMethod(
                        name = "onError",
                    ),
                ),
            ),
            Clazz(
                name = "AssetsInterface",
                nameRule = "com.mutangtech.qianji.network.api.asset.\\w+",
                type = "class",
                methods =
                listOf(
                    ClazzMethod(
                        name = "getBindBill",
                    ),
                    ClazzMethod(
                        name = "setBindBill",
                    ),
                ),
            ),
            Clazz(
                name = "AddBillIntentAct",
                nameRule = "com.mutangtech.qianji.bill.auto.AddBillIntentAct",
                type = "class",
                methods =
                listOf(
                    ClazzMethod(
                        findName = "InsertAutoTask",
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
                    ),
                ),

            ),

        )
        set(value) {}


}