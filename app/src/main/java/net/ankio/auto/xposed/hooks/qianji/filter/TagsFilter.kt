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
import net.ankio.auto.xposed.hooks.qianji.models.QjTagModel
import net.ankio.dex.model.Clazz

/**
 * 钱迹宿主 `TagsFilter` 的轻量包装（Xposed 反射委托）。
 * - 不复制逻辑，直接转发宿主；
 * - 接收/返回 `QjTagModel` 包装对象，便于统一风格。
 */
class TagsFilter private constructor(private var filterObj: Any?) {

    companion object : HookerClazz() {
        /** 宿主类名 */
        private const val CLAZZ = "com.mutangtech.qianji.filter.filters.TagsFilter"

        /** 精确类名规则 */
        override var rule = Clazz(name = this::class.java.name, nameRule = CLAZZ)

        /** 从宿主 `List<Tag>` 构造 */
        fun fromTagObjects(tags: List<Any>): TagsFilter =
            fromObject(XposedHelpers.newInstance(clazz(), tags))

        /** 包装已有宿主对象 */
        fun fromObject(obj: Any): TagsFilter {
            if (obj::class.java.name != CLAZZ) {
                throw IllegalArgumentException("${obj::class.java.name} must be instance of $CLAZZ")
            }
            return TagsFilter(obj)
        }
    }

    /** 返回底层宿主对象 */
    fun toObject(): Any = filterObj!!

    /** 键名固定为 "tags" */
    fun getKey(): String = XposedHelpers.callMethod(filterObj, "getKey") as String

    /** 获取标签列表（包装返回） */
    fun getTagList(): List<QjTagModel> =
        (XposedHelpers.callMethod(
            filterObj,
            "getTagList"
        ) as List<*>).mapNotNull { it?.let { QjTagModel.fromObject(it) } }

    /** 值序列化：以逗号拼接的 tagId 列表 */
    fun value(): String = XposedHelpers.callMethod(filterObj, "getValue") as String

    /** 标签 ID 集合（字符串） */
    fun ids(): Set<String> =
        (XposedHelpers.callMethod(filterObj, "ids") as Set<*>).mapNotNull { it?.toString() }.toSet()
}