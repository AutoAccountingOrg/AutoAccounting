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
 * 钱迹宿主 `MoneyFilter` 的轻量包装（Xposed 反射驱动）。
 * - 仅做方法转发，不改变逻辑，保持与宿主完全一致。
 * - 与 `BookFilter`、`DataFilter`、`TypesFilter` 风格统一。
 */
class MoneyFilter private constructor(private var filterObj: Any?) {

    companion object : HookerClazz() {
        /** 宿主类名 */
        private const val CLAZZ = "com.mutangtech.qianji.filter.filters.MoneyFilter"

        /** 精确类名规则 */
        override var rule = Clazz(name = this::class.java.name, nameRule = CLAZZ)

        /** 新建空区间（min/max=0） */
        fun newInstance(): MoneyFilter = fromObject(XposedHelpers.newInstance(clazz()))

        /** 新建指定金额区间 */
        fun withRange(min: Double, max: Double): MoneyFilter =
            fromObject(XposedHelpers.newInstance(clazz(), min, max))

        /** 包装已有宿主对象 */
        fun fromObject(obj: Any): MoneyFilter {
            if (obj::class.java.name != CLAZZ) {
                throw IllegalArgumentException("${obj::class.java.name} must be instance of $CLAZZ")
            }
            return MoneyFilter(obj)
        }
    }

    /** 返回底层宿主对象 */
    fun toObject(): Any = filterObj!!

    /** 键名固定为 "moneyrange" */
    fun getKey(): String = XposedHelpers.callMethod(filterObj, "getKey") as String

    /** 最小值 */
    fun getMin(): Double = XposedHelpers.callMethod(filterObj, "getMin") as Double

    /** 最大值 */
    fun getMax(): Double = XposedHelpers.callMethod(filterObj, "getMax") as Double

    /** 值序列化："min,max" */
    fun value(): String = XposedHelpers.callMethod(filterObj, "getValue") as String

    /** 设置最小值（链式） */
    fun setMin(min: Double): MoneyFilter = apply {
        XposedHelpers.callMethod(filterObj, "setMin", min)
    }

    /** 设置最大值（链式） */
    fun setMax(max: Double): MoneyFilter = apply {
        XposedHelpers.callMethod(filterObj, "setMax", max)
    }
}