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
import net.ankio.auto.xposed.hooks.qianji.models.QjBookModel
import net.ankio.dex.model.Clazz

/**
 * 钱迹宿主 `BookFilter` 的轻量包装（Xposed 反射驱动）。
 *
 * 设计目标：
 * - 不复刻逻辑，全部委托给宿主同名类的方法（Never break userspace）。
 * - 提供 Kotlin 风格 API，并与现有 `QjBookModel` 协同工作。
 * - 保持最小实现：只暴露当前项目需要的行为。
 */
class BookFilter private constructor(private var filterObj: Any?) {

    companion object : HookerClazz() {
        /** 宿主类完整类名（精确匹配） */
        private const val CLAZZ = "com.mutangtech.qianji.filter.filters.BookFilter"

        /** Xposed 规则：精确类名 */
        override var rule = Clazz(name = this::class.java.name, nameRule = CLAZZ)

        /**
         * 创建一个新的宿主 `BookFilter` 实例并包装。
         */
        fun newInstance(): BookFilter = fromObject(XposedHelpers.newInstance(clazz()))

        /**
         * 将已有宿主对象包装为 `BookFilter`。
         * @throws IllegalArgumentException 当对象并非目标宿主类时抛出
         */
        fun fromObject(obj: Any): BookFilter {
            if (obj::class.java.name != CLAZZ) {
                throw IllegalArgumentException("${obj::class.java.name} must be instance of $CLAZZ")
            }
            return BookFilter(obj)
        }

        /**
         * 调用宿主的静态 `copy(BookFilter)` 返回拷贝实例。
         */
        fun copy(of: BookFilter): BookFilter =
            fromObject(XposedHelpers.callStaticMethod(clazz(), "copy", of.toObject())!!)

        /**
         * 调用宿主的静态 `valueOf(Book)` 生成仅含单一账本的过滤器。
         */
        fun valueOf(book: QjBookModel): BookFilter =
            fromObject(
                XposedHelpers.callStaticMethod(
                    clazz(),
                    "valueOf",
                    book.toObject(),
                )!!
            )
    }

    /** 返回底层宿主对象 */
    fun toObject(): Any = filterObj!!

    /** 添加一本账本（包装对象） */
    fun add(book: QjBookModel) {
        XposedHelpers.callMethod(filterObj, "add", book.toObject())
    }

    /** 添加一本账本（原始宿主对象） */
    fun addRaw(bookObj: Any?) {
        if (bookObj == null) return
        XposedHelpers.callMethod(filterObj, "add", bookObj)
    }

    /** 批量添加（包装对象列表） */
    fun addAll(books: List<QjBookModel>) {
        XposedHelpers.callMethod(filterObj, "addAll", books.map { it.toObject() })
    }

    /** 清空 */
    fun clear() {
        XposedHelpers.callMethod(filterObj, "clear")
    }

    /** 是否包含 */
    fun contains(book: QjBookModel): Boolean =
        XposedHelpers.callMethod(filterObj, "contains", book.toObject()) as Boolean

    /** 数量 */
    fun count(): Int = XposedHelpers.callMethod(filterObj, "count") as Int

    /** 首项（包装返回） */
    fun first(): QjBookModel =
        QjBookModel.fromObject(XposedHelpers.callMethod(filterObj, "first")!!)

    /** 账本 ID 列表 */
    fun getBookIds(): List<Long> =
        (XposedHelpers.callMethod(filterObj, "getBookIds") as List<*>).mapNotNull {
            when (it) {
                is Long -> it
                is Int -> it.toLong()
                is String -> it.toLongOrNull()
                else -> null
            }
        }

    /** 宿主范围配置对象（保留原始对象） */
    fun getBookRangeConfig(): Any? = XposedHelpers.callMethod(filterObj, "getBookRangeConfig")

    /** 账本列表（包装返回） */
    fun getBooks(): List<QjBookModel> =
        (XposedHelpers.callMethod(filterObj, "getBooks") as List<*>).mapNotNull {
            it?.let { QjBookModel.fromObject(it) }
        }

    /** 首个账本 ID，不存在时由宿主返回 -1 */
    fun getFirstId(): Long = XposedHelpers.callMethod(filterObj, "getFirstId") as Long

    /** 键名（宿主固定为 "books"） */
    fun getKey(): String = XposedHelpers.callMethod(filterObj, "getKey") as String

    /** 字符串值（以逗号拼接的账本 ID 列表） */
    fun value(): String = XposedHelpers.callMethod(filterObj, "getValue") as String

    /** 是否为空 */
    fun isEmpty(): Boolean = XposedHelpers.callMethod(filterObj, "isEmpty") as Boolean

    /** 是否仅一个元素 */
    fun isSingle(): Boolean = XposedHelpers.callMethod(filterObj, "isSingle") as Boolean

    /** 移除一本账本 */
    fun remove(book: QjBookModel) {
        XposedHelpers.callMethod(filterObj, "remove", book.toObject())
    }

    /** 设置为仅包含一本账本 */
    fun set(book: QjBookModel) {
        XposedHelpers.callMethod(filterObj, "set", book.toObject())
    }
}