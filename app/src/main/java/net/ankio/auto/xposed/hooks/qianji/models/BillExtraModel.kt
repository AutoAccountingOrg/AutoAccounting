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

/**
 * 账单附加信息模型（钱迹 BillExtra 的轻量包装）
 *
 * 设计原则：
 * - 不复制数据结构，不重写业务逻辑；仅通过反射调用原对象的方法，保持向后兼容。
 * - 方法命名与原类一致，降低适配成本并避免“臆想实现”。
 */
class BillExtraModel(private val obj: Any) {


    companion object : HookerClazz() {
        /** 原始类全名 */
        const val CLAZZ = "com.mutangtech.qianji.data.model.BillExtra"

        /** 标志位：无 */
        const val FLAG_NONE = 0

        /** 标志位：不计入统计 */
        const val FLAG_NOT_COUNT = 1

        /** 标志位：不计入预算 */
        const val FLAG_NOT_BUDGET = 2

        override var rule = Clazz(
            name = this::class.java.name,
            nameRule = CLAZZ,
        )

        /** 从已有 BillExtra 实例创建包装 */
        fun fromObject(origin: Any): BillExtraModel = BillExtraModel(origin)

        fun newInstance(): BillExtraModel {
            return fromObject(XposedHelpers.newInstance(clazz()))
        }

        /**
         * 静态：判断标志位包含
         * @param value 标志集合
         * @param flag 目标标志
         */
        fun hasFlag(value: Int, flag: Int): Boolean = (value and flag) == flag

        /**
         * 静态：设置或清除标志位
         * @param value 原标志集合
         * @param flag 目标标志
         * @param on 是否设置
         */
        fun markFlag(value: Int, flag: Int, on: Boolean): Int =
            if (on) value or flag else value and flag.inv()
    }

    /** 暴露原始对象 */
    fun toObject(): Any = obj

    /** 已报销（旧判定） */
    fun hasBaoxiaoedOld(): Boolean = XposedHelpers.callMethod(obj, "hasBaoxiaoedOld") as Boolean

    /** 已报销（V2 判定，依据 bxs 字段） */
    fun hasBaoxiaoedV2(): Boolean = XposedHelpers.callMethod(obj, "hasBaoxiaoedV2") as Boolean

    /** 获取标志位 */
    fun getFlag(): Int = XposedHelpers.callMethod(obj, "getFlag") as Int

    /** 实例方法：判断是否包含某标志位 */
    fun hasFlag(flag: Int): Boolean = hasFlag(getFlag(), flag)

    /** 获取报销时间（秒） */
    fun getBaoxiaotimeInSec(): Long = XposedHelpers.callMethod(obj, "getBaoxiaotimeInSec") as Long

    /** 获取报销金额 */
    fun getBaoxiaoMoney(): Double =
        (XposedHelpers.callMethod(obj, "getBaoxiaoMoney") as Number).toDouble()

    /** 获取报销资产ID（Deprecated，保持兼容） */
    fun getBaoXiaoAssetId(): Long = XposedHelpers.callMethod(obj, "getBaoXiaoAssetId") as Long

    /** 获取币种扩展（原始对象），如需进一步封装可在上层转换 */
    fun getCurrencyExtra(): Any? = XposedHelpers.callMethod(obj, "getCurrencyExtra")

    /** 获取已报销账单ID列表（V2） */
    @Suppress("UNCHECKED_CAST")
    fun getEDBaoXiaoBillIds(): List<Long>? =
        XposedHelpers.callMethod(obj, "getEDBaoXiaoBillIds") as? List<Long>

    /** 获取标签ID列表 */
    @Suppress("UNCHECKED_CAST")
    fun getTagIds(): List<String>? = XposedHelpers.callMethod(obj, "getTagIds") as? List<String>

    /** 获取分期手续费 */
    fun getInstallmentFee(): Double =
        (XposedHelpers.callMethod(obj, "getInstallmentFee") as Number).toDouble()

    /** 获取转账手续费 */
    fun getTransfee(): Double = (XposedHelpers.callMethod(obj, "getTransfee") as Number).toDouble()

    /** 获取退款来源账单ID */
    fun getRefundSourceBillId(): Long =
        XposedHelpers.callMethod(obj, "getRefundSourceBillId") as Long

    /** 获取退款账单ID列表 */
    @Suppress("UNCHECKED_CAST")
    fun getRefundBillIds(): List<Long> =
        (XposedHelpers.callMethod(obj, "getRefundBillIds") as? List<Long>) ?: emptyList()

    /** 获取退款条目数量 */
    fun getRefundCount(): Int = XposedHelpers.callMethod(obj, "getRefundCount") as Int

    /** 是否存在退款 */
    fun hasRefund(): Boolean = XposedHelpers.callMethod(obj, "hasRefund") as Boolean

    /** 获取退款总金额 */
    fun getTotalRefundMoney(): Double =
        (XposedHelpers.callMethod(obj, "getTotalRefundMoney") as Number).toDouble()

    /** 设置是否报销 */
    fun setBaoXiao(value: Boolean) {
        XposedHelpers.callMethod(obj, "setBaoXiao", value)
    }

    /** 设置报销金额 */
    fun setBaoxiaoMoney(value: Double) {
        XposedHelpers.callMethod(obj, "setBaoxiaoMoney", value)
    }

    /** 设置货币扩展（原始对象） */
    fun setCurrencyExtra(value: CurrencyExtraModel) {
        XposedHelpers.callMethod(obj, "setCurrencyExtra", value.toObject())
    }

    /** 设置标志位 */
    fun setFlag(value: Int) {
        XposedHelpers.callMethod(obj, "setFlag", value)
    }

    /** 设置标签ID列表 */
    fun setTagIds(value: List<String>?) {
        XposedHelpers.callMethod(obj, "setTagIds", value)
    }

    /** 设置转账手续费 */
    fun setTransfee(value: Double) {
        XposedHelpers.callMethod(obj, "setTransfee", value)
    }

    /** 移除退款记录 */
    fun removeRefund(billId: Long) {
        XposedHelpers.callMethod(obj, "removeRefund", billId)
    }

    /** 字符串表示（透传原对象实现） */
    override fun toString(): String = XposedHelpers.callMethod(obj, "toString") as String
}