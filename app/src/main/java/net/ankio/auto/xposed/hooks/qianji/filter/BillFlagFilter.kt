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
 * 钱迹宿主 `BillFlagFilter` 的轻量包装（Xposed 反射委托）。
 * - 完全转发宿主，零逻辑复制；
 * - 与其他过滤器保持统一风格。
 */
class BillFlagFilter private constructor(private var filterObj: Any?) {

    companion object : HookerClazz() {
        /** 宿主类名 */
        private const val CLAZZ = "com.mutangtech.qianji.filter.filters.BillFlagFilter"

        /** 精确类名规则 */
        override var rule = Clazz(name = this::class.java.name, nameRule = CLAZZ)

        /** 指定 flag 构造 */
        fun withFlag(flag: Int): BillFlagFilter =
            fromObject(XposedHelpers.newInstance(clazz(), flag))

        /** 包装已有宿主对象 */
        fun fromObject(obj: Any): BillFlagFilter {
            if (obj::class.java.name != CLAZZ) {
                throw IllegalArgumentException("${obj::class.java.name} must be instance of $CLAZZ")
            }
            return BillFlagFilter(obj)
        }
    }

    /** 返回底层宿主对象 */
    fun toObject(): Any = filterObj!!

    /** 读取 flag 值 */
    fun getFlag(): Int = XposedHelpers.callMethod(filterObj, "getFlag") as Int

    /** 键名固定为 "bill_flag" */
    fun getKey(): String = XposedHelpers.callMethod(filterObj, "getKey") as String

    /** 值：字符串化的 flag */
    fun value(): String = XposedHelpers.callMethod(filterObj, "getValue") as String
}