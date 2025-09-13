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
 * 钱迹宿主 `ImageFilter` 的轻量包装（Xposed 反射驱动）。
 * - 完全委托宿主实现，不复制逻辑；
 * - 与 `BookFilter`、`DataFilter`、`TypesFilter`、`MoneyFilter` 保持一致风格。
 */
class ImageFilter private constructor(private var filterObj: Any?) {

    companion object : HookerClazz() {
        /** 宿主类名 */
        private const val CLAZZ = "com.mutangtech.qianji.filter.filters.ImageFilter"

        /** 精确类名规则 */
        override var rule = Clazz(name = this::class.java.name, nameRule = CLAZZ)

        // 宿主常量别名
        const val ALL = -1
        const val HAS = 1
        const val NONE = 0

        /** 新建默认过滤器（等价宿主无参构造，默认 ALL） */
        fun newInstance(): ImageFilter = fromObject(XposedHelpers.newInstance(clazz()))

        /** 新建指定值过滤器（-1: ALL，0: NONE，1: HAS） */
        fun withValue(value: Int): ImageFilter =
            fromObject(XposedHelpers.newInstance(clazz(), value))

        /** 包装已有宿主对象 */
        fun fromObject(obj: Any): ImageFilter {
            if (obj::class.java.name != CLAZZ) {
                throw IllegalArgumentException("${obj::class.java.name} must be instance of $CLAZZ")
            }
            return ImageFilter(obj)
        }
    }

    /** 返回底层宿主对象 */
    fun toObject(): Any = filterObj!!

    /** 键名固定为 "images" */
    fun getKey(): String = XposedHelpers.callMethod(filterObj, "getKey") as String

    /** 值（-1: ALL，0: NONE，1: HAS） */
    fun value(): Int = XposedHelpers.callMethod(filterObj, "getValue") as Int
}