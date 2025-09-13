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

package net.ankio.auto.xposed.hooks.qianji.helper

import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.core.api.HookerClazz
import net.ankio.auto.xposed.hooks.qianji.models.QjAssetAccountModel
import net.ankio.dex.model.Clazz
import net.ankio.dex.model.ClazzMethod

class AssetDbHelper(private val obj: Any) {
    companion object : HookerClazz() {
        override var rule = Clazz(
            name = this::class.java.name,
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
        )

        fun newInstance(): AssetDbHelper = AssetDbHelper(XposedHelpers.newInstance(clazz()))
    }

    fun toObject() = obj

    fun insertOrReplace(assetAccountModel: QjAssetAccountModel) {
        XposedHelpers.callMethod(obj, "insertOrReplace", assetAccountModel.assetObj)
    }
}