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

package net.ankio.auto.xposed.hooks.qianji.models

import android.content.Context
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AppRuntime

class Book {
    private var bookObj: Any? = null

    companion object {
        const val DEFAULT_BOOK_ID = -1L
        private const val DEMO_BOOK_ID = 1L
        private const val TYPE_DEFAULT = -1
        const val VISIBLE_ALL = -1
        const val VISIBLE_NOT = 0
        const val VISIBLE_YES = 1
        val CLAZZ = "com.mutangtech.qianji.data.model.Book"
        val bookClazz = Hooker.loader(CLAZZ)

        fun fromObject(obj: Any): Book {
            AppRuntime.log("obj::class.java.name = ${obj::class.java.name}")
            if (obj::class.java.name != CLAZZ) {
                throw IllegalArgumentException("obj is not a Book object")
            }

            val book = Book()
            book.bookObj = obj
            return book
        }

        /**
         * 获取默认账本
         */
        fun defaultBook(context: Context?): Book {
            return XposedHelpers.callStaticMethod(
                bookClazz,
                "defaultBook",
                context
            )!!.let { fromObject(it) }
        }

        /**
         * 生成演示数据
         */
        fun generateDemoData(context: Context): List<Book> {
            return (XposedHelpers.callStaticMethod(
                bookClazz,
                "generateDemoData",
                context
            ) as List<*>).map { fromObject(it!!) }
        }

        /**
         * 获取类型名称
         */
        @Deprecated("已弃用")
        fun getTypeName(type: Int): String {
            return XposedHelpers.callStaticMethod(
                bookClazz,
                "getTypeName",
                type
            ) as String
        }
    }

    fun toObject(): Any = bookObj!!

    /**
     * 复制账本信息
     */
    fun copy(book: Book?) {
        XposedHelpers.callMethod(
            bookObj,
            "copy",
            book?.toObject()
        )
    }

    /**
     * 获取账本ID
     */
    fun getBookId(): Long {
        return XposedHelpers.callMethod(
            bookObj,
            "getBookId"
        ) as Long
    }


    /**
     * 获取封面
     */
    fun getCover(): String {
        return XposedHelpers.callMethod(
            bookObj,
            "getCover"
        ) as String
    }

    /**
     * 切换可见性
     */
    fun toggleVisible() {
        XposedHelpers.callMethod(
            bookObj,
            "toggleVisible"
        )
    }

    /**
     * 设置可见性
     */
    fun setVisible(visible: Boolean) {
        XposedHelpers.callMethod(
            bookObj,
            "setVisible",
            visible
        )
    }

    /**
     * 获取创建时间
     */
    fun getCreatetimeInSec(): Long {
        return XposedHelpers.callMethod(
            bookObj,
            "getCreatetimeInSec"
        ) as Long
    }

    /**
     * 获取过期状态
     */
    fun getExpired(): Int {
        return XposedHelpers.callMethod(
            bookObj,
            "getExpired"
        ) as Int
    }

    /**
     * 获取成员数量
     */
    fun getMemberCount(): Int {
        return XposedHelpers.callMethod(
            bookObj,
            "getMemberCount"
        ) as Int
    }

    /**
     * 获取成员ID
     */
    fun getMemberId(): String? {
        return XposedHelpers.callMethod(
            bookObj,
            "getMemberId"
        ) as String?
    }


    /**
     * 获取名称
     */
    fun getName(): String {
        return XposedHelpers.callMethod(
            bookObj,
            "getName"
        ) as String
    }

    /**
     * 获取范围
     */
    fun getRange(): String? {
        return XposedHelpers.callMethod(
            bookObj,
            "getRange"
        ) as String?
    }

    /**
     * 获取排序值
     */
    fun getSort(): Int {
        return XposedHelpers.callMethod(
            bookObj,
            "getSort"
        ) as Int
    }

    /**
     * 获取类型
     */
    fun getType(): Int {
        return XposedHelpers.callMethod(
            bookObj,
            "getType"
        ) as Int
    }

    /**
     * 获取类型名称
     */
    fun getTypename(): String? {
        return XposedHelpers.callMethod(
            bookObj,
            "getTypename"
        ) as String?
    }

    /**
     * 获取更新时间
     */
    fun getUpdateTimeInSec(): Long {
        return XposedHelpers.callMethod(
            bookObj,
            "getUpdateTimeInSec"
        ) as Long
    }

    /**
     * 获取用户ID
     */
    fun getUserid(): String? {
        return XposedHelpers.callMethod(
            bookObj,
            "getUserid"
        ) as String?
    }

    /**
     * 获取可见性
     */
    fun getVisible(): Int {
        return XposedHelpers.callMethod(
            bookObj,
            "getVisible"
        ) as Int
    }

    /**
     * 获取ID
     */
    fun get_id(): Long? {
        return XposedHelpers.callMethod(
            bookObj,
            "get_id"
        ) as Long?
    }

    /**
     * 是否为默认账本
     */
    fun isDefaultBook(): Boolean {
        return XposedHelpers.callMethod(
            bookObj,
            "isDefaultBook"
        ) as Boolean
    }

    /**
     * 是否为演示账本
     */
    fun isDemo(): Boolean {
        return XposedHelpers.callMethod(
            bookObj,
            "isDemo"
        ) as Boolean
    }

    /**
     * 作为成员是否过期
     */
    fun isExpiredAsMember(): Boolean {
        return XposedHelpers.callMethod(
            bookObj,
            "isExpiredAsMember"
        ) as Boolean
    }

    /**
     * 作为所有者是否过期
     */
    fun isExpiredAsOwner(): Boolean {
        return XposedHelpers.callMethod(
            bookObj,
            "isExpiredAsOwner"
        ) as Boolean
    }

    /**
     * 是否为成员
     */
    fun isMember(): Boolean {
        return XposedHelpers.callMethod(
            bookObj,
            "isMember"
        ) as Boolean
    }

    /**
     * 是否为所有者
     */
    fun isOwner(): Boolean {
        return XposedHelpers.callMethod(
            bookObj,
            "isOwner"
        ) as Boolean
    }

    /**
     * 是否可见
     */
    fun isVisible(): Boolean {
        return XposedHelpers.callMethod(
            bookObj,
            "isVisible"
        ) as Boolean
    }

    /**
     * 设置账本ID
     */
    fun setBookId(id: Long?) {
        XposedHelpers.callMethod(
            bookObj,
            "setBookId",
            id
        )
    }


    /**
     * 设置封面
     */
    fun setCover(cover: String?) {
        XposedHelpers.callMethod(
            bookObj,
            "setCover",
            cover
        )
    }

    /**
     * 设置创建时间
     */
    fun setCreatetimeInSec(time: Long) {
        XposedHelpers.callMethod(
            bookObj,
            "setCreatetimeInSec",
            time
        )
    }

    /**
     * 设置过期状态
     */
    fun setExpired(expired: Int) {
        XposedHelpers.callMethod(
            bookObj,
            "setExpired",
            expired
        )
    }

    /**
     * 设置成员数量
     */
    fun setMemberCount(count: Int) {
        XposedHelpers.callMethod(
            bookObj,
            "setMemberCount",
            count
        )
    }

    /**
     * 设置成员ID
     */
    fun setMemberId(id: String?) {
        XposedHelpers.callMethod(
            bookObj,
            "setMemberId",
            id
        )
    }


    /**
     * 设置名称
     */
    fun setName(name: String?) {
        XposedHelpers.callMethod(
            bookObj,
            "setName",
            name
        )
    }

    /**
     * 设置范围配置
     */
    fun setRangeConfig(range: String?) {
        XposedHelpers.callMethod(
            bookObj,
            "setRangeConfig",
            range
        )
    }

    /**
     * 设置排序值
     */
    fun setSort(sort: Int) {
        XposedHelpers.callMethod(
            bookObj,
            "setSort",
            sort
        )
    }

    /**
     * 设置类型
     */
    fun setType(type: Int) {
        XposedHelpers.callMethod(
            bookObj,
            "setType",
            type
        )
    }

    /**
     * 设置类型名称
     */
    fun setTypename(typename: String?) {
        XposedHelpers.callMethod(
            bookObj,
            "setTypename",
            typename
        )
    }

    /**
     * 设置更新时间
     */
    fun setUpdateTimeInSec(time: Long) {
        XposedHelpers.callMethod(
            bookObj,
            "setUpdateTimeInSec",
            time
        )
    }

    /**
     * 设置用户ID
     */
    fun setUserid(userid: String?) {
        XposedHelpers.callMethod(
            bookObj,
            "setUserid",
            userid
        )
    }

    /**
     * 设置可见性
     */
    fun setVisible(visible: Int) {
        XposedHelpers.callMethod(
            bookObj,
            "setVisible",
            visible
        )
    }

    /**
     * 设置ID
     */
    fun set_id(id: Long?) {
        XposedHelpers.callMethod(
            bookObj,
            "set_id",
            id
        )
    }
}