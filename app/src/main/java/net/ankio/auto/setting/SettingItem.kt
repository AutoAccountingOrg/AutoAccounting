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
 import net.ankio.auto.ui.api.BaseActivity
 
 /**
  * 设置项数据类，用于定义设置界面中的各个配置项
  */
 sealed class SettingItem(
     @StringRes open val title: Int,
     open val regex: String? = null,
 ) {
     /**
      * 开关类型的设置项
      */
     data class Switch(
         @StringRes override val title: Int,
         val key: String? = null,
         @DrawableRes val icon: Int? = null,
         @StringRes val subTitle: Int? = null,
         val default: Boolean = false,
         val onGetKeyValue: (() -> Boolean)? = null,
         val onItemClick: ((value: Boolean, activity: BaseActivity) -> Unit)? = null,
         val onSavedValue: ((value: Boolean, activity: BaseActivity) -> Unit)? = null,
         override val regex: String? = null,
     ) : SettingItem(title,regex)
 
     /**
      * 文本类型的设置项
      */
     data class Text(
         @StringRes override val title: Int,
         @DrawableRes val icon: Int? = null,
         @StringRes val subTitle: Int? = null,
         val link: String? = null,
         val onItemClick: ((activity: BaseActivity) -> Unit)? = null,
         val onGetKeyValue: (() -> String?)? = null,
         override val regex: String? = null,
     ) : SettingItem(title,regex)

     data class Card(
            @StringRes override val title: Int,
            override val regex: String? = null,
     ) : SettingItem(title,regex)

     /**
      * 输入框类型的设置项
      */
     data class Input(
         @StringRes override val title: Int,
         val key: String,
         @DrawableRes val icon: Int? = null,
         @StringRes val subTitle: Int? = null,
         val default: Any = "",
         val isPassword: Boolean = false,
         val onGetKeyValue: (() -> Any)? = null,
         val onItemClick: ((value: Any, activity: BaseActivity) -> Unit)? = null,
         val onSavedValue: ((value: Any, activity: BaseActivity) -> Unit)? = null,
         override val regex: String? = null,
     ) : SettingItem(title,regex)
 
     /**
      * 选择类型的设置项
      */
     data class Select(
         @StringRes override val title: Int,
         val key: String? = null,
         val selectList: HashMap<String, Any>,
         @DrawableRes val icon: Int? = null,
         @StringRes val subTitle: Int? = null,
         val default: Any = "",
         val onGetKeyValue: (() -> Any)? = null,
         val onSavedValue: ((value: Any, activity: BaseActivity) -> Unit)? = null,
         override val regex: String? = null,
     ) : SettingItem(title,regex)
 
     /**
      * 颜色选择类型的设置项
      */
     data class Color(
         @StringRes override val title: Int,
         @DrawableRes val icon: Int? = null,
         @StringRes val subTitle: Int? = null,
         val onGetKeyValue: (() -> Int),
         val onItemClick: ((value: Int, activity: BaseActivity) -> Unit)? = null,
         val onSavedValue: ((value: Int, activity: BaseActivity) -> Unit)? = null,
         override val regex: String? = null,
     ) : SettingItem(title,regex)
 
     /**
      * 标题类型的设置项
      */
     data class Title(
         @StringRes override val title: Int,
         override val regex: String? = null,
     ) : SettingItem(title,regex)

 }