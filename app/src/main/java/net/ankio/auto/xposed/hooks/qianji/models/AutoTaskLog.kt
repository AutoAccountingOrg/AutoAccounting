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

class AutoTaskLog(private val autoTaskLogObj: Any) {
    companion object {
        const val ERROR = -1
        const val SUCCESS = 1

        fun fromObject(obj: Any): AutoTaskLog = AutoTaskLog(obj)
    }

    fun toObject(): Any = autoTaskLogObj

    /**
     * 获取账单ID
     */
    fun getBillId(): Long {
        return XposedHelpers.callMethod(
            autoTaskLogObj,
            "getBillId"
        ) as Long
    }

    /**
     * 获取错误信息
     */
    fun getError(): String? {
        return XposedHelpers.callMethod(
            autoTaskLogObj,
            "getError"
        ) as String?
    }

    /**
     * 获取来源
     */
    fun getFrom(): String? {
        return XposedHelpers.callMethod(
            autoTaskLogObj,
            "getFrom"
        ) as String?
    }

    /**
     * 获取状态
     */
    fun getStatus(): Int {
        return XposedHelpers.callMethod(
            autoTaskLogObj,
            "getStatus"
        ) as Int
    }

    /**
     * 获取时间
     */
    fun getTime(): Long {
        return XposedHelpers.callMethod(
            autoTaskLogObj,
            "getTime"
        ) as Long
    }

    /**
     * 获取用户ID
     */
    fun getUserid(): String? {
        return XposedHelpers.callMethod(
            autoTaskLogObj,
            "getUserid"
        ) as String?
    }

    /**
     * 获取值
     */
    fun getValue(): String? {
        return XposedHelpers.callMethod(
            autoTaskLogObj,
            "getValue"
        ) as String?
    }

    /**
     * 获取ID
     */
    fun get_id(): Long? {
        return XposedHelpers.callMethod(
            autoTaskLogObj,
            "get_id"
        ) as Long?
    }

    /**
     * 设置账单ID
     */
    fun setBillId(billId: Long) {
        XposedHelpers.callMethod(
            autoTaskLogObj,
            "setBillId",
            billId
        )
    }

    /**
     * 设置错误信息
     */
    fun setError(error: String?) {
        XposedHelpers.callMethod(
            autoTaskLogObj,
            "setError",
            error
        )
    }

    /**
     * 设置来源
     */
    fun setFrom(from: String?) {
        XposedHelpers.callMethod(
            autoTaskLogObj,
            "setFrom",
            from
        )
    }

    /**
     * 设置状态
     */
    fun setStatus(status: Int) {
        XposedHelpers.callMethod(
            autoTaskLogObj,
            "setStatus",
            status
        )
    }

    /**
     * 设置时间
     */
    fun setTime(time: Long) {
        XposedHelpers.callMethod(
            autoTaskLogObj,
            "setTime",
            time
        )
    }

    /**
     * 设置用户ID
     */
    fun setUserid(userid: String?) {
        XposedHelpers.callMethod(
            autoTaskLogObj,
            "setUserid",
            userid
        )
    }

    /**
     * 设置值
     */
    fun setValue(value: String?) {
        XposedHelpers.callMethod(
            autoTaskLogObj,
            "setValue",
            value
        )
    }

    /**
     * 设置ID
     */
    fun set_id(id: Long?) {
        XposedHelpers.callMethod(
            autoTaskLogObj,
            "set_id",
            id
        )
    }
}