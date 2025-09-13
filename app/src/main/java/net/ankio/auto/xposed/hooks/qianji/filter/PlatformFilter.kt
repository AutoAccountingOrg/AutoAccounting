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
 * 钱迹宿主 `PlatformFilter` 的轻量包装（Xposed 反射委托）。
 * - 不改变逻辑；
 * - 与其他过滤器一致的 API 风格。
 */
class PlatformFilter private constructor(private var filterObj: Any?) {

    companion object : HookerClazz() {
        /** 宿主类名 */
        private const val CLAZZ = "com.mutangtech.qianji.filter.filters.PlatformFilter"

        /** 精确类名规则 */
        override var rule = Clazz(name = this::class.java.name, nameRule = CLAZZ)

        /** 新建默认（-1 表示全部） */
        fun newInstance(): PlatformFilter = fromObject(XposedHelpers.newInstance(clazz()))

        /** 指定平台构造 */
        fun withValue(source: Int): PlatformFilter =
            fromObject(XposedHelpers.newInstance(clazz(), source))

        /** 包装已有宿主对象 */
        fun fromObject(obj: Any): PlatformFilter {
            if (obj::class.java.name != CLAZZ) {
                throw IllegalArgumentException("${obj::class.java.name} must be instance of $CLAZZ")
            }
            return PlatformFilter(obj)
        }
    }

    /** 返回底层宿主对象 */
    fun toObject(): Any = filterObj!!

    /** 键名固定为 "source" */
    fun getKey(): String = XposedHelpers.callMethod(filterObj, "getKey") as String

    /** 值（平台 ID，-1 表示全部） */
    fun value(): Int = XposedHelpers.callMethod(filterObj, "getValue") as Int
}