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
import net.ankio.auto.xposed.core.hook.Hooker

/**
 * 借贷信息模型类
 * 用于处理钱迹App中的借贷相关数据
 */
class LoanInfo {
    // 钱迹App中借贷信息类的完整类名
    private val CLAZZ = "com.mutangtech.qianji.asset.model.LoanInfo"

    // 加载钱迹借贷信息类
    private val loanInfoClazz = Hooker.loader(CLAZZ)

    // 存储实际的借贷信息对象
    private var loanInfoObj: Any? = null

    companion object {
        /**
         * 从现有对象创建LoanInfo实例
         * @param obj 原始借贷信息对象
         * @return 包装后的LoanInfo对象
         */
        fun fromObject(obj: Any): LoanInfo {
            val loanInfo = LoanInfo()
            loanInfo.loanInfoObj = obj
            return loanInfo
        }

        fun newInstance(): LoanInfo {
            val loanInfo = LoanInfo()
            loanInfo.loanInfoObj = XposedHelpers.newInstance(loanInfo.loanInfoClazz)
            return loanInfo
        }
    }

    fun toObject(): Any? = loanInfoObj

    /**
     * 获取账户ID
     * @return 账户ID
     * @deprecated 已废弃
     */
    @Deprecated("已废弃")
    fun getAccountId(): Long = XposedHelpers.callMethod(loanInfoObj, "getAccountId") as Long

    /**
     * 获取结束日期
     * @return 结束日期字符串
     */
    fun getEnddate(): String = XposedHelpers.callMethod(loanInfoObj, "getEnddate") as String

    /**
     * 获取备注信息
     * @return 备注信息字符串
     * @deprecated 已废弃
     */
    @Deprecated("已废弃")
    fun getRemark(): String = XposedHelpers.callMethod(loanInfoObj, "getRemark") as String

    /**
     * 获取开始日期
     * @return 开始日期字符串
     */
    fun getStartdate(): String = XposedHelpers.callMethod(loanInfoObj, "getStartdate") as String

    /**
     * 获取总金额
     * @return 总金额
     */
    fun getTotalMoney(): Double = XposedHelpers.callMethod(loanInfoObj, "getTotalMoney") as Double

    /**
     * 获取总支付金额
     * @return 总支付金额
     */
    fun getTotalpay(): Double = XposedHelpers.callMethod(loanInfoObj, "getTotalpay") as Double

    /**
     * 设置结束日期
     * @param enddate 结束日期字符串
     */
    fun setEnddate(enddate: String) = XposedHelpers.callMethod(loanInfoObj, "setEnddate", enddate)

    /**
     * 设置备注信息
     * @param remark 备注信息字符串
     */
    fun setRemark(remark: String) = XposedHelpers.callMethod(loanInfoObj, "setRemark", remark)

    /**
     * 设置开始日期
     * @param startdate 开始日期字符串
     */
    fun setStartdate(startdate: String) =
        XposedHelpers.callMethod(loanInfoObj, "setStartdate", startdate)

    /**
     * 设置总金额
     * @param totalMoney 总金额
     */
    fun setTotalMoney(totalMoney: Double) =
        XposedHelpers.callMethod(loanInfoObj, "setTotalMoney", totalMoney)

    /**
     * 设置总支付金额
     * @param totalpay 总支付金额
     */
    fun setTotalpay(totalpay: Double) =
        XposedHelpers.callMethod(loanInfoObj, "setTotalpay", totalpay)
}