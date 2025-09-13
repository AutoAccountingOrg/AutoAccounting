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
import net.ankio.auto.xposed.core.api.HookerClazz
import net.ankio.dex.model.Clazz
import net.ankio.dex.model.ClazzMethod

/**
 * 自动任务日志模型
 * 与钱迹的自动任务日志对象进行交互的轻量包装，遵循统一的 Hook 模型风格。
 */
class AutoTaskLogModel(private val logObj: Any) {


    companion object : HookerClazz() {
        // 业务状态常量：沿用原有常量，保持向后兼容
        const val ERROR = -1
        const val SUCCESS = 1
        const val CLAZZ = "com.mutangtech.qianji.data.model.AutoTaskLog"

        // 使用方法签名进行解析，避免依赖固定类名，降低适配成本
        override var rule = Clazz(
            name = this::class.java.name,
            nameRule = CLAZZ,
        )

        /**
         * 从已有对象创建包装实例
         */
        fun fromObject(obj: Any): AutoTaskLogModel {
            return AutoTaskLogModel(obj)
        }
    }

    /** 将内部对象暴露给外部使用（保持原签名，保证兼容） */
    fun toObject(): Any = logObj

    /** 获取账单ID */
    fun getBillId(): Long = XposedHelpers.callMethod(logObj, "getBillId") as Long

    /** 获取错误信息 */
    fun getError(): String? = XposedHelpers.callMethod(logObj, "getError") as String?

    /** 获取来源 */
    fun getFrom(): String? = XposedHelpers.callMethod(logObj, "getFrom") as String?

    /** 获取状态 */
    fun getStatus(): Int = XposedHelpers.callMethod(logObj, "getStatus") as Int

    /** 获取时间 */
    fun getTime(): Long = XposedHelpers.callMethod(logObj, "getTime") as Long

    /** 获取用户ID */
    fun getUserid(): String? = XposedHelpers.callMethod(logObj, "getUserid") as String?

    /** 获取值 */
    fun getValue(): String? = XposedHelpers.callMethod(logObj, "getValue") as String?

    /** 获取ID */
    fun get_id(): Long? = XposedHelpers.callMethod(logObj, "get_id") as Long?

    /** 设置账单ID */
    fun setBillId(billId: Long) {
        XposedHelpers.callMethod(logObj, "setBillId", billId)
    }

    /** 设置错误信息 */
    fun setError(error: String?) {
        XposedHelpers.callMethod(logObj, "setError", error)
    }

    /** 设置来源 */
    fun setFrom(from: String?) {
        XposedHelpers.callMethod(logObj, "setFrom", from)
    }

    /** 设置状态 */
    fun setStatus(status: Int) {
        XposedHelpers.callMethod(logObj, "setStatus", status)
    }

    /** 设置时间 */
    fun setTime(time: Long) {
        XposedHelpers.callMethod(logObj, "setTime", time)
    }

    /** 设置用户ID */
    fun setUserid(userid: String?) {
        XposedHelpers.callMethod(logObj, "setUserid", userid)
    }

    /** 设置值 */
    fun setValue(value: String?) {
        XposedHelpers.callMethod(logObj, "setValue", value)
    }

    /** 设置ID */
    fun set_id(id: Long?) {
        XposedHelpers.callMethod(logObj, "set_id", id)
    }
}