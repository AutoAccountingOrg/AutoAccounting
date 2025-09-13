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

package net.ankio.auto.xposed.hooks.qianji.filter

import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.core.api.HookerClazz
import net.ankio.dex.model.Clazz

/**
 * 钱迹宿主 `TypesFilter` 的轻量包装（Xposed 反射驱动）。
 * - 仅做方法转发，不改变逻辑。
 * - 与 `BookFilter`、`DataFilter` 风格一致，Kotlin 简洁 API。
 */
class TypesFilter private constructor(private var filterObj: Any?) {

    companion object : HookerClazz() {
        /** 宿主类名 */
        private const val CLAZZ = "com.mutangtech.qianji.filter.filters.TypesFilter"

        /** 精确类名规则 */
        override var rule = Clazz(name = this::class.java.name, nameRule = CLAZZ)

        /** 新建一个空类型过滤器 */
        fun newInstance(): TypesFilter = fromObject(XposedHelpers.newInstance(clazz()))

        /** 新建并包含一个初始类型 */
        fun withType(type: Int): TypesFilter =
            fromObject(XposedHelpers.newInstance(clazz(), type))

        /** 包装已有宿主对象 */
        fun fromObject(obj: Any): TypesFilter {
            if (obj::class.java.name != CLAZZ) {
                throw IllegalArgumentException("${obj::class.java.name} must be instance of $CLAZZ")
            }
            return TypesFilter(obj)
        }
    }

    /** 返回底层宿主对象 */
    fun toObject(): Any = filterObj!!

    /** 添加类型（-1 表示全部） */
    fun add(type: Int) {
        XposedHelpers.callMethod(filterObj, "add", type)
    }

    /** 是否包含某类型 */
    fun contains(type: Int): Boolean =
        XposedHelpers.callMethod(filterObj, "contains", type) as Boolean

    /** 键名固定为 "types" */
    fun getKey(): String = XposedHelpers.callMethod(filterObj, "getKey") as String

    /** 当处于单选时返回首个类型，否则返回 -1 */
    fun getType(): Int = XposedHelpers.callMethod(filterObj, "getType") as Int

    /** 获取类型列表（原始 Integer 列表） */
    @Suppress("UNCHECKED_CAST")
    fun getTypes(): ArrayList<Int> =
        XposedHelpers.callMethod(filterObj, "getTypes") as ArrayList<Int>

    /** 值序列化（逗号分隔） */
    fun value(): String = XposedHelpers.callMethod(filterObj, "getValue") as String

    /** 设置类型列表（原样转发） */
    fun setTypes(types: ArrayList<Int>) {
        XposedHelpers.callMethod(filterObj, "setTypes", types)
    }

    /** 单选切换：选中则只保留该类型，否则恢复为空 -> -1 */
    fun singleType(type: Int) {
        XposedHelpers.callMethod(filterObj, "singleType", type)
    }

    /** 切换某类型的 on/off（-1 表示清空并置为全选） */
    fun toggle(type: Int) {
        XposedHelpers.callMethod(filterObj, "toggle", type)
    }
}