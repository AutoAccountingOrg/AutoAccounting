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
 * 钱迹宿主 `SortFilter` 的轻量包装（Xposed 反射委托）。
 * - 不复制任何逻辑，保持与宿主一致；
 * - 与其他过滤器类保持统一风格与命名。
 */
class SortFilter private constructor(private var filterObj: Any?) {

    companion object : HookerClazz() {
        /** 宿主类名 */
        private const val CLAZZ = "com.mutangtech.qianji.filter.filters.SortFilter"

        /** 精确类名规则 */
        override var rule = Clazz(name = this::class.java.name, nameRule = CLAZZ)

        // 常量（与宿主一致）
        const val TIME = 0
        const val AMOUNT = 1

        /** 新建实例（类型+是否升序） */
        fun newInstance(type: Int, asc: Boolean = true): SortFilter =
            fromObject(XposedHelpers.newInstance(clazz(), type, asc))

        /** 新建时间倒序（宿主工厂：newTimeDesc(false|true)） */
        fun newTimeDesc(asc: Boolean = false): SortFilter =
            fromObject(
                XposedHelpers.newInstance(
                    clazz(),
                    0,
                    asc,
                ) ?: XposedHelpers.newInstance(clazz(), TIME, asc)
            )

        /** 包装已有宿主对象 */
        fun fromObject(obj: Any): SortFilter {
            if (obj::class.java.name != CLAZZ) {
                throw IllegalArgumentException("${obj::class.java.name} must be instance of $CLAZZ")
            }
            return SortFilter(obj)
        }
    }

    /** 返回底层宿主对象 */
    fun toObject(): Any = filterObj!!

    /** 是否升序 */
    fun getAscSort(): Boolean = XposedHelpers.callMethod(filterObj, "getAscSort") as Boolean

    /** 键名固定为 "sort" */
    fun getKey(): String = XposedHelpers.callMethod(filterObj, "getKey") as String

    /** 类型：TIME/AMOUNT */
    fun getType(): Int = XposedHelpers.callMethod(filterObj, "getType") as Int

    /** 值序列化："type,asc(0/1)" */
    fun value(): String = XposedHelpers.callMethod(filterObj, "getValue") as String

    /** 是否升序（同 getAscSort） */
    fun isAscSort(): Boolean = XposedHelpers.callMethod(filterObj, "isAscSort") as Boolean

    /** 解析字符串到内部状态（转发宿主 parse） */
    fun parse(s: String) {
        XposedHelpers.callMethod(filterObj, "parse", s)
    }

    /** 设置升序 */
    fun setAscSort(asc: Boolean): SortFilter = apply {
        XposedHelpers.callMethod(filterObj, "setAscSort", asc)
    }

    /** 设置类型 */
    fun setType(type: Int): SortFilter = apply {
        XposedHelpers.callMethod(filterObj, "setType", type)
    }
}