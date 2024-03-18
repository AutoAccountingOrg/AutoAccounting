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

/**
 * 自动记账要求不同分类类型的分类不允许出现重复，即 支出或者收入 分类中，不能出现两个一样的分类，无论是一级分类还是二级分类
 */
data class CategoryModel(
    var name:String= "",//分类名称
    var icon:String = "",//分类图标，url或者base64
    var type:Int = 0,//分类类型，0：支出，1：收入
    var sort:Int = 0,//排序
    var id:String = "0",//分类id
    var parent:String = "-1",//父分类id，如果是一级分类，则为-1
){
    override fun toString(): String {
        return "CategoryModel(name='$name', icon='$icon', type=$type, sort=$sort, id='$id', parent='$parent')"
    }
}