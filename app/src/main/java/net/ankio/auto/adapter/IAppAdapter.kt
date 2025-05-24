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

package net.ankio.auto.adapter

import net.ankio.auto.constant.BookFeatures
import org.ezbook.server.db.model.BillInfoModel

interface IAppAdapter {
    //包名
    val pkg: String

    //官网链接
    val link: String

    //图标链接
    val icon: String

    //描述
    val desc: String

    //应用描述
    val name: String

    //是否支持同步资产，你可以在这里做资产初始化的
    fun supportSyncAssets(): Boolean

    //支持的功能，部分记账软件可能接口不支持
    fun features(): List<BookFeatures>

    //从目标App同步资产等数据
    fun syncAssets()

    //将账单同步到目标APP
    fun syncBill(billInfoModel: BillInfoModel)
}