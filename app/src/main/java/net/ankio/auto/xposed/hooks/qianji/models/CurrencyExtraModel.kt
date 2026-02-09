/*
 * Copyright (C) 2026 ankio(ankio@ankio.net)
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
 * 币种扩展信息模型（钱迹 CurrencyExtra 的轻量包装）
 *
 * 设计原则：
 * - 原类使用公有字段（非 getter/setter），故通过 getObjectField/setObjectField 访问；
 * - 静态工厂方法内部依赖混淆类，不复制业务逻辑，直接反射透传；
 * - 方法/字段命名与原类一致，保持向后兼容。
 */
class CurrencyExtraModel(private val obj: Any) {

    companion object : HookerClazz() {
        /** 原始类全名 */
        const val CLAZZ = "com.mutangtech.qianji.data.model.CurrencyExtra"

        /** 懒加载原始类引用 */
        private val currencyExtraClazz by lazy { clazz() }

        override var rule = Clazz(
            name = this::class.java.name,
            nameRule = CLAZZ,
        )

        /** 从已有 CurrencyExtra 实例创建包装 */
        fun fromObject(origin: Any): CurrencyExtraModel = CurrencyExtraModel(origin)

        /** 创建空实例（baseValue 默认 0.0，与原类构造一致） */
        fun newInstance(): CurrencyExtraModel =
            fromObject(XposedHelpers.newInstance(currencyExtraClazz))

        /**
         * 构建收支类型的币种扩展
         * @param srcSymbol 源币种符号
         * @param srcRate 源币种汇率
         * @param targetSymbol 目标币种符号
         * @param targetRate 目标币种汇率
         * @param baseSymbol 基础币种符号
         * @param amount 金额
         * @param fee 手续费（负数时取绝对值参与计算）
         * @return 币种扩展对象，源币种与基础币种相同时返回 null
         */
        fun buildCurrencyIncomeSpend(
            srcSymbol: String,
            srcRate: Double,
            targetSymbol: String,
            targetRate: Double,
            baseSymbol: String,
            amount: Double,
            fee: Double,
        ): CurrencyExtraModel? {
            val result = XposedHelpers.callStaticMethod(
                currencyExtraClazz,
                "buildCurrencyIncomeSpend",
                srcSymbol, srcRate, targetSymbol, targetRate, baseSymbol, amount, fee
            ) ?: return null
            return fromObject(result)
        }

        /**
         * 构建转账类型的币种扩展
         * @param srcSymbol 源币种符号
         * @param srcRate 源币种汇率
         * @param targetSymbol 目标币种符号
         * @param targetRate 目标币种汇率
         * @param baseSymbol 基础币种符号
         * @param amount 金额
         * @param fee 手续费（正数时参与计算）
         * @return 币种扩展对象，源/目标币种均与基础币种相同时返回 null
         */
        fun buildTransferCurrency(
            srcSymbol: String,
            srcRate: Double,
            targetSymbol: String,
            targetRate: Double,
            baseSymbol: String,
            amount: Double,
            fee: Double,
        ): CurrencyExtraModel? {
            val result = XposedHelpers.callStaticMethod(
                currencyExtraClazz,
                "buildTransferCurrency",
                srcSymbol, srcRate, targetSymbol, targetRate, baseSymbol, amount, fee
            ) ?: return null
            return fromObject(result)
        }
    }

    /** 暴露原始对象 */
    fun toObject(): Any = obj

    // ─── 源币种字段 ───

    /** 获取源币种符号 */
    fun getSrcSymbol(): String? = XposedHelpers.getObjectField(obj, "srcSymbol") as? String

    /** 设置源币种符号 */
    fun setSrcSymbol(value: String?) {
        XposedHelpers.setObjectField(obj, "srcSymbol", value)
    }

    /** 获取源币种金额 */
    fun getSrcValue(): Double = XposedHelpers.getDoubleField(obj, "srcValue")

    /** 设置源币种金额 */
    fun setSrcValue(value: Double) {
        XposedHelpers.setDoubleField(obj, "srcValue", value)
    }

    // ─── 目标币种字段 ───

    /** 获取目标币种符号 */
    fun getTargetSymbol(): String? = XposedHelpers.getObjectField(obj, "targetSymbol") as? String

    /** 设置目标币种符号 */
    fun setTargetSymbol(value: String?) {
        XposedHelpers.setObjectField(obj, "targetSymbol", value)
    }

    /** 获取目标币种金额 */
    fun getTargetValue(): Double = XposedHelpers.getDoubleField(obj, "targetValue")

    /** 设置目标币种金额 */
    fun setTargetValue(value: Double) {
        XposedHelpers.setDoubleField(obj, "targetValue", value)
    }

    // ─── 基础币种字段 ───

    /** 获取基础币种符号 */
    fun getBaseSymbol(): String? = XposedHelpers.getObjectField(obj, "baseSymbol") as? String

    /** 设置基础币种符号 */
    fun setBaseSymbol(value: String?) {
        XposedHelpers.setObjectField(obj, "baseSymbol", value)
    }

    /** 获取基础币种金额 */
    fun getBaseValue(): Double = XposedHelpers.getDoubleField(obj, "baseValue")

    /** 设置基础币种金额 */
    fun setBaseValue(value: Double) {
        XposedHelpers.setDoubleField(obj, "baseValue", value)
    }

    /** 字符串表示（透传原对象实现） */
    override fun toString(): String = XposedHelpers.callMethod(obj, "toString") as String
}
