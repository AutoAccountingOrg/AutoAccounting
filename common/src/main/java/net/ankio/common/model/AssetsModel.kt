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

package net.ankio.common.model

import net.ankio.common.constant.AssetType
import net.ankio.common.constant.Currency

data class AssetsModel(
    var name: String = "", //账户名
    /**
     * 这里的图标是url链接或存储的base64图片
     */
    var icon: String = "", //图标
    var sort: Int = 0, //排序
    var type: AssetType = AssetType.CASH, //账户类型
    var extra:String = "",//额外信息
    var currency: Currency = Currency.CNY //货币类型
){
    override fun toString(): String {
        return "AssetsModel(name='$name', icon='$icon', sort=$sort)"
    }
}