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

package net.ankio.auto.setting

import android.app.Activity
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.ankio.auto.setting.types.ItemType

data class SettingItem(
    @StringRes val title: Int,//标题
    val id :String? = null,//id
    val idLink:String? = null,//和某个ID进行关联，只有boolean类型才能关联
    val idLinkBoolean:Boolean = false,//关联的boolean类型的值，为false隐藏对应idLink否则就是为true的时候隐藏
    val key: String? = null, //key，存储专用
    @DrawableRes val icon: Int? = null, //图标
    @StringRes val subTitle: Int? = null,//副标题
    val type: ItemType = ItemType.TITLE,//类型
    val selectList: HashMap<String, Any>? = null,//如果为switch类型，需要提供选项
    val link: String? = null,//如果为link类型，需要提供链接
    val default:Any? = null,//默认值
    val onGetKeyValue:(()->Any)? = null,
    val onItemClick: ((value: Any,activity:Activity)-> Unit)? = null,   //点击事件
    val onSavedValue: ((value: Any,activity:Activity) -> Unit)? = null//保存事件
)
