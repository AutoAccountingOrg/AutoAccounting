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

import de.robv.android.xposed.XposedHelpers

class Tag(private val tagObj: Any) {
    companion object {
        const val MAX_GROUP_NAME_LENGTH = 10
        const val SIZE_LARGE = 3
        const val SIZE_NORMAL = 2
        const val SIZE_TINY = 1
        const val STATUS_ALL = -1
        const val STATUS_ARCHIVE = 2
        const val STATUS_DEFAULT = 1

        fun fromObject(obj: Any): Tag = Tag(obj)
    }

    fun toObject(): Any = tagObj

    /**
     * 获取账本ID
     */
    fun getBookId(): Long? {
        return XposedHelpers.callMethod(
            tagObj,
            "getBookId"
        ) as Long?
    }

    /**
     * 获取颜色
     */
    fun getColor(): String? {
        return XposedHelpers.callMethod(
            tagObj,
            "getColor"
        ) as String?
    }

    /**
     * 获取创建时间
     */
    fun getCreateTimeInSec(): Int {
        return XposedHelpers.callMethod(
            tagObj,
            "getCreateTimeInSec"
        ) as Int
    }

    /**
     * 获取分组ID
     */
    fun getGroupId(): String? {
        return XposedHelpers.callMethod(
            tagObj,
            "getGroupId"
        ) as String?
    }

    /**
     * 获取名称
     */
    fun getName(): String? {
        return XposedHelpers.callMethod(
            tagObj,
            "getName"
        ) as String?
    }

    /**
     * 获取排序
     */
    fun getSort(): Int {
        return XposedHelpers.callMethod(
            tagObj,
            "getSort"
        ) as Int
    }

    /**
     * 获取状态
     */
    fun getStatus(): Int {
        return XposedHelpers.callMethod(
            tagObj,
            "getStatus"
        ) as Int
    }

    /**
     * 获取标签颜色
     */
    fun getTagColor(): Long {
        return XposedHelpers.callMethod(
            tagObj,
            "getTagColor"
        ) as Long
    }

    /**
     * 获取标签ID
     */
    fun getTagId(): String? {
        return XposedHelpers.callMethod(
            tagObj,
            "getTagId"
        ) as String?
    }

    /**
     * 获取更新时间
     */
    fun getUpdateTimeInSec(): Int {
        return XposedHelpers.callMethod(
            tagObj,
            "getUpdateTimeInSec"
        ) as Int
    }

    /**
     * 获取用户ID
     */
    fun getUserId(): String? {
        return XposedHelpers.callMethod(
            tagObj,
            "getUserId"
        ) as String?
    }

    /**
     * 设置账本ID
     */
    fun setBookId(id: Long?) {
        XposedHelpers.callMethod(
            tagObj,
            "setBookId",
            id
        )
    }

    /**
     * 设置颜色
     */
    fun setColor(color: Long) {
        XposedHelpers.callMethod(
            tagObj,
            "setColor",
            color
        )
    }

    /**
     * 设置颜色(字符串)
     */
    fun setColor(color: String?) {
        XposedHelpers.callMethod(
            tagObj,
            "setColor",
            color
        )
    }

    /**
     * 设置创建时间
     */
    fun setCreateTimeInSec(time: Int) {
        XposedHelpers.callMethod(
            tagObj,
            "setCreateTimeInSec",
            time
        )
    }

    /**
     * 设置分组ID
     */
    fun setGroupId(groupId: String?) {
        XposedHelpers.callMethod(
            tagObj,
            "setGroupId",
            groupId
        )
    }

    /**
     * 设置名称
     */
    fun setName(name: String?) {
        XposedHelpers.callMethod(
            tagObj,
            "setName",
            name
        )
    }

    /**
     * 设置排序
     */
    fun setSort(sort: Int) {
        XposedHelpers.callMethod(
            tagObj,
            "setSort",
            sort
        )
    }

    /**
     * 设置状态
     */
    fun setStatus(status: Int) {
        XposedHelpers.callMethod(
            tagObj,
            "setStatus",
            status
        )
    }

    /**
     * 设置标签ID
     */
    fun setTagId(tagId: String?) {
        XposedHelpers.callMethod(
            tagObj,
            "setTagId",
            tagId
        )
    }

    /**
     * 设置更新时间
     */
    fun setUpdateTimeInSec(time: Int) {
        XposedHelpers.callMethod(
            tagObj,
            "setUpdateTimeInSec",
            time
        )
    }

    /**
     * 设置用户ID
     */
    fun setUserId(userId: String?) {
        XposedHelpers.callMethod(
            tagObj,
            "setUserId",
            userId
        )
    }
}