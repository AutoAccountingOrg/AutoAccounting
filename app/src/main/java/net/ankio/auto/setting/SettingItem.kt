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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.ankio.auto.constant.ItemType
import net.ankio.auto.ui.api.BaseActivity

data class SettingItem(
    @StringRes val title: Int, // 标题
    val regex: String? = null, // 关联表达式
    val key: String? = null, // key，存储专用
    @DrawableRes val icon: Int? = null, // 图标
    @StringRes val subTitle: Int? = null, // 副标题
    val type: ItemType = ItemType.TITLE, // 类型
    val selectList: HashMap<String, Any>? = null, // 如果为switch类型，需要提供选项
    val link: String? = null, // 如果为link类型，需要提供链接
    val default: Any? = null, // 默认值
    val onGetKeyValue: (() -> Any?)? = null,
    val onItemClick: ((value: Any, activity: BaseActivity) -> Unit)? = null, // 点击事件
    val onSavedValue: ((value: Any, activity: BaseActivity) -> Unit)? = null, // 保存事件
)
