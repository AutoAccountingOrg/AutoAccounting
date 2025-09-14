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

import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.utils.MessageUtils
import net.ankio.auto.xposed.hooks.qianji.activity.AddBillIntentAct
import net.ankio.auto.xposed.hooks.qianji.activity.MainActivity
import net.ankio.auto.xposed.hooks.qianji.activity.MainDrawerLayout
import net.ankio.auto.xposed.hooks.qianji.filter.AssetsFilter
import net.ankio.auto.xposed.hooks.qianji.filter.BillFlagFilter
import net.ankio.auto.xposed.hooks.qianji.filter.BookFilter
import net.ankio.auto.xposed.hooks.qianji.filter.DataFilter
import net.ankio.auto.xposed.hooks.qianji.filter.ImageFilter
import net.ankio.auto.xposed.hooks.qianji.filter.MoneyFilter
import net.ankio.auto.xposed.hooks.qianji.filter.PlatformFilter
import net.ankio.auto.xposed.hooks.qianji.filter.SortFilter
import net.ankio.auto.xposed.hooks.qianji.filter.TagsFilter
import net.ankio.auto.xposed.hooks.qianji.filter.TypesFilter
import net.ankio.auto.xposed.hooks.qianji.helper.AssetDbHelper
import net.ankio.auto.xposed.hooks.qianji.helper.BillDbHelper
import net.ankio.auto.xposed.hooks.qianji.hooks.AutoHooker
import net.ankio.auto.xposed.hooks.qianji.hooks.SideBarHooker
import net.ankio.auto.xposed.hooks.qianji.impl.AssetPreviewPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.impl.BaseSubmitAssetPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.impl.BookManagerImpl
import net.ankio.auto.xposed.hooks.qianji.impl.BxPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.impl.CateInitPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.impl.GetCategoryListInterface
import net.ankio.auto.xposed.hooks.qianji.impl.RefundPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.impl.SearchPresenterImpl
import net.ankio.auto.xposed.hooks.qianji.models.QjAssetAccountModel
import net.ankio.auto.xposed.hooks.qianji.models.AutoTaskLogModel
import net.ankio.auto.xposed.hooks.qianji.models.BillExtraModel
import net.ankio.auto.xposed.hooks.qianji.models.QjBillModel
import net.ankio.auto.xposed.hooks.qianji.models.QjBookModel
import net.ankio.auto.xposed.hooks.qianji.models.QjCategoryModel
import net.ankio.auto.xposed.hooks.qianji.models.LoanInfoModel
import net.ankio.auto.xposed.hooks.qianji.models.QjTagModel
import net.ankio.auto.xposed.hooks.qianji.models.UserModel
import net.ankio.auto.xposed.hooks.qianji.utils.BroadcastUtils
import net.ankio.auto.xposed.hooks.qianji.utils.TimeRecordUtils
import net.ankio.dex.model.Clazz
import net.ankio.dex.model.ClazzField
import net.ankio.dex.model.ClazzMethod

class QianjiHooker : HookerManifest() {
    override val packageName: String
        get() = "com.mutangtech.qianji"
    override val appName: String
        get() = "钱迹"
    override var minVersion: Long = 951

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
            // models
            AutoTaskLogModel.rule,
            LoanInfoModel.rule,
            QjAssetAccountModel.rule,
            QjBillModel.rule,
            BillExtraModel.rule,
            QjBookModel.rule,
            QjCategoryModel.rule,
            QjTagModel.rule,
            UserModel.rule,

            // Activity
            MainActivity.rule,
            MainDrawerLayout.rule,
            AddBillIntentAct.rule,
            //impl
            AssetPreviewPresenterImpl.rule,
            BookManagerImpl.rule,
            BxPresenterImpl.rule,
            CateInitPresenterImpl.rule,
            RefundPresenterImpl.rule,
            SearchPresenterImpl.rule,
            // interface
            GetCategoryListInterface.rule,



            ///////////////////////////Timeout//////////////////////////////////////
            TimeRecordUtils.rule,
            BroadcastUtils.rule,
            ///////////////////////////AssetInsert//////////////////////////////////////
            AssetDbHelper.rule,
            BillDbHelper.rule,
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

            // Filters
            AssetsFilter.rule,
            BillFlagFilter.rule,
            BookFilter.rule,
            DataFilter.rule,
            ImageFilter.rule,
            MoneyFilter.rule,
            PlatformFilter.rule,
            SortFilter.rule,
            TagsFilter.rule,
            TypesFilter.rule,

            BaseSubmitAssetPresenterImpl.rule
        )
        set(value) {}


}