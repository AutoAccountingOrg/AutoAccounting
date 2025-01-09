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
 * 资产账户模型类
 * 用于处理钱迹App中的资产账户相关数据
 */
class AssetAccount {


    // 存储实际的资产账户对象
    var assetObj: Any? = null

    // 账户纳入统计标记
    val FLAG_INCOUNT: Int = 1

    // 付款日提醒次数
    val PAY_DATE_ALERT_COUNT: Int = 3

    // 账户默认状态
    val STATUS_DEFAULT: Int = 0

    // 账户隐藏状态
    val STATUS_HIDE: Int = 2

    // 账户无状态
    val STATUS_NONE: Int = -1

    // 债务已完成状态
    val STATUS_ZHAIWU_FINISHED: Int = 1

    companion object {
        // 钱迹App中资产账户类的完整类名
        val CLAZZ = "com.mutangtech.qianji.data.model.AssetAccount"

        // 加载钱迹资产账户类
        val assetClazz = Hooker.loader(CLAZZ)

        /**
         * 从现有对象创建AssetAccount实例
         * @param obj 原始资产账户对象
         * @return 包装后的AssetAccount对象
         */
        fun fromObject(obj: Any): AssetAccount {
            if (obj::class.java.name != CLAZZ) {
                throw IllegalArgumentException("${obj::class.java.name} is not a valid AssetAccount object")
            }
            val assetAccount = AssetAccount()
            assetAccount.assetObj = obj
            return assetAccount
        }

        /**
         * 创建新的AssetAccount实例
         * @param type 账户主类型
         * @param stype 账户子类型
         * @param name 账户名称
         * @return 新的AssetAccount对象
         */
        fun newInstance(type: Int, stype: Int, name: String): AssetAccount {
            //  public static AssetAccount newInstance(int i10, int i11) {
            //        AssetAccount assetAccount = new AssetAccount();
            //        assetAccount.type = i10;
            //        assetAccount.stype = i11;
            //        return assetAccount;
            //    }

            val assetAccount = AssetAccount()
            assetAccount.assetObj =
                XposedHelpers.callStaticMethod(assetClazz, "newInstance", type, stype)
            assetAccount.setName(name)
            return assetAccount
        }

    }

    fun toObject(): Any? = assetObj

    /**
     * 增加账户金额
     * @param money 要增加的金额
     */
    fun addMoney(money: Double) = XposedHelpers.callMethod(assetObj, "addMoney", money)

    /**
     * 增加账户使用次数
     */
    fun addUserCount() = XposedHelpers.callMethod(assetObj, "addUserCount")

    /**
     * 更改账户金额
     * @param money 新的金额值
     */
    fun changeMoney(money: Double) = XposedHelpers.callMethod(assetObj, "changeMoney", money)

    /**
     * 与另一个账户进行比较
     * @param other 要比较的账户
     * @return 比较结果
     */
    fun compareTo(other: AssetAccount) =
        XposedHelpers.callMethod(assetObj, "compareTo", other.assetObj) as Int

    /**
     * 复制本地数据
     * @param other 源账户对象
     */
    fun copyLocalData(other: AssetAccount) =
        XposedHelpers.callMethod(assetObj, "copyLocalData", other.assetObj)

    /**
     * 获取可用额度
     * @return 可用额度
     */
    fun getAvailable(): Double = XposedHelpers.callMethod(assetObj, "getAvailable") as Double

    /**
     * 获取创建时间
     * @return 创建时间戳
     */
    fun getCreatetime(): Long = XposedHelpers.callMethod(assetObj, "getCreatetime") as Long

    /**
     * 获取信用卡信息
     * @return 信用卡信息对象
     */
    fun getCreditInfo(): Any = XposedHelpers.callMethod(assetObj, "getCreditInfo")

    /**
     * 获取货币类型
     * @return 货币类型字符串
     */
    fun getCurrency(): String = XposedHelpers.callMethod(assetObj, "getCurrency") as String

    /**
     * 获取债务借款金额
     * @return 债务借款金额
     */
    fun getDebtLoanMoney(): Double =
        XposedHelpers.callMethod(assetObj, "getDebtLoanMoney") as Double

    /**
     * 获取额外信息
     * @return 额外信息对象
     */
    fun getExtra(): Any = XposedHelpers.callMethod(assetObj, "getExtra")

    /**
     * 获取初始值
     * @return 初始金额
     */
    fun getInitValue(): Double = XposedHelpers.callMethod(assetObj, "getInitValue") as Double

    /**
     * 获取分期付款列表
     * @return 分期付款列表
     */
    fun getInstallList(): List<Any> =
        XposedHelpers.callMethod(assetObj, "getInstallList") as List<Any>

    /**
     * 获取分期付款金额
     * @return 分期付款总金额
     */
    fun getInstallMoney(): Double = XposedHelpers.callMethod(assetObj, "getInstallMoney") as Double

    /**
     * 获取最后支付时间
     * @return 最后支付时间戳
     */
    fun getLastPayTime(): Long = XposedHelpers.callMethod(assetObj, "getLastPayTime") as Long

    /**
     * 获取额度限制
     * @return 额度限制值
     */
    fun getLimit(): Double = XposedHelpers.callMethod(assetObj, "getLimit") as Double

    /**
     * 获取带分期的账户金额
     * @return 包含分期在内的总金额
     */
    fun getMoneyWithInstalment(): Double =
        XposedHelpers.callMethod(assetObj, "getMoneyWithInstalment") as Double

    fun getIcon(): String = XposedHelpers.callMethod(assetObj, "getIcon") as String

    fun getId(): Long = XposedHelpers.callMethod(assetObj, "getId") as Long

    fun getIncount(): Int = XposedHelpers.callMethod(assetObj, "getIncount") as Int

    fun getLoanInfo(): LoanInfo =
        LoanInfo.fromObject(XposedHelpers.callMethod(assetObj, "getLoanInfo"))

    fun getMoney(): Double = XposedHelpers.callMethod(assetObj, "getMoney") as Double

    /**
     * 获取账户名称
     * @return 账户名称字符串
     */
    fun getName(): String = XposedHelpers.callMethod(assetObj, "getName") as String

    /**
     * 获取账户备注
     * @return 账户备注字符串
     */
    fun getRemark(): String = XposedHelpers.callMethod(assetObj, "getRemark") as String

    /**
     * 获取账户排序值
     * @return 排序序号
     */
    fun getSort(): Int = XposedHelpers.callMethod(assetObj, "getSort") as Int

    /**
     * 获取账户状态
     * @return 账户状态值
     */
    fun getStatus(): Int = XposedHelpers.callMethod(assetObj, "getStatus") as Int

    /**
     * 获取账户子类型
     * @return 账户子类型值
     */
    fun getStype(): Int = XposedHelpers.callMethod(assetObj, "getStype") as Int

    /**
     * 获取账户主类型
     * @return 账户主类型值
     */
    fun getType(): Int = XposedHelpers.callMethod(assetObj, "getType") as Int

    /**
     * 获取账户使用次数
     * @return 使用次数
     */
    fun getUsecount(): Int = XposedHelpers.callMethod(assetObj, "getUsecount") as Int

    /**
     * 获取用户ID
     * @return 用户ID字符串
     */
    fun getUserid(): String = XposedHelpers.callMethod(assetObj, "getUserid") as String

    /**
     * 判断是否为银行卡账户
     * @return 是否为银行卡
     */
    fun isCard(): Boolean = XposedHelpers.callMethod(assetObj, "isCard") as Boolean

    /**
     * 判断是否为现金账户
     * @return 是否为现金
     */
    fun isCash(): Boolean = XposedHelpers.callMethod(assetObj, "isCash") as Boolean

    /**
     * 判断是否为普通账户
     * @return 是否为普通账户
     */
    fun isCommon(): Boolean = XposedHelpers.callMethod(assetObj, "isCommon") as Boolean

    /**
     * 判断是否为信用账户
     * @return 是否为信用账户
     */
    fun isCredit(): Boolean = XposedHelpers.callMethod(assetObj, "isCredit") as Boolean

    /**
     * 判断是否为债务账户
     * @return 是否为债务账户
     */
    fun isDebt(): Boolean = XposedHelpers.callMethod(assetObj, "isDebt") as Boolean

    /**
     * 判断是否为债务借入账户
     * @return 是否为债务借入账户
     */
    fun isDebtLoan(): Boolean = XposedHelpers.callMethod(assetObj, "isDebtLoan") as Boolean

    /**
     * 判断是否为债务借入包装账户
     * @return 是否为债务借入包装账户
     */
    fun isDebtLoanWrapper(): Boolean =
        XposedHelpers.callMethod(assetObj, "isDebtLoanWrapper") as Boolean

    /**
     * 判断是否为债务包装账户
     * @return 是否为债务包装账户
     */
    fun isDebtWrapper(): Boolean = XposedHelpers.callMethod(assetObj, "isDebtWrapper") as Boolean

    /**
     * 判断是否为花呗账户
     * @return 是否为花呗账户
     */
    fun isHuaBei(): Boolean = XposedHelpers.callMethod(assetObj, "isHuaBei") as Boolean

    /**
     * 判断账户是否纳入统计
     * @return 是否纳入统计
     */
    fun isIncount(): Boolean = XposedHelpers.callMethod(assetObj, "isIncount") as Boolean

    /**
     * 判断是否为借入账户
     * @return 是否为借入账户
     */
    fun isLoan(): Boolean = XposedHelpers.callMethod(assetObj, "isLoan") as Boolean

    /**
     * 判断是否为借入包装账户
     * @return 是否为借入包装账户
     */
    fun isLoanWrapper(): Boolean = XposedHelpers.callMethod(assetObj, "isLoanWrapper") as Boolean

    /**
     * 判断是否与基础货币相同
     * @return 是否与基础货币相同
     */
    fun isSameWithBaseCurrency(): Boolean =
        XposedHelpers.callMethod(assetObj, "isSameWithBaseCurrency") as Boolean

    /**
     * 判断是否设置了货币类型
     * @return 是否设置了货币类型
     */
    fun isSetCurrency(): Boolean = XposedHelpers.callMethod(assetObj, "isSetCurrency") as Boolean

    /**
     * 判断是否为共享额度账户
     * @return 是否为共享额度账户
     */
    fun isSharedLimit(): Boolean = XposedHelpers.callMethod(assetObj, "isSharedLimit") as Boolean

    /**
     * 判断是否为共享额度子账户
     * @return 是否为共享额度子账户
     */
    fun isSharedLimitSubAsset(): Boolean =
        XposedHelpers.callMethod(assetObj, "isSharedLimitSubAsset") as Boolean

    /**
     * 判断是否为贵宾卡
     * @return 是否为贵宾卡
     */
    fun isVipCard(): Boolean = XposedHelpers.callMethod(assetObj, "isVipCard") as Boolean

    /**
     * 判断账户是否可见
     * @return 是否可见
     */
    fun isVisible(): Boolean = XposedHelpers.callMethod(assetObj, "isVisible") as Boolean

    /**
     * 判断债务是否已结清
     * @return 是否已结清
     */
    fun isZhaiWuFinished(): Boolean =
        XposedHelpers.callMethod(assetObj, "isZhaiWuFinished") as Boolean

    /**
     * 设置账户颜色
     * @param color 颜色值字符串
     */
    fun setColor(color: String) = XposedHelpers.callMethod(assetObj, "setColor", color)

    /**
     * 设置创建时间
     * @param time 创建时间戳
     */
    fun setCreatetime(time: Long) = XposedHelpers.callMethod(assetObj, "setCreatetime", time)

    /**
     * 设置信用卡信息
     * @param creditInfo 信用卡信息对象
     */
    fun setCreditInfo(creditInfo: Any) =
        XposedHelpers.callMethod(assetObj, "setCreditInfo", creditInfo)

    /**
     * 设置货币类型
     * @param currency 货币类型字符串
     */
    fun setCurrency(currency: String) = XposedHelpers.callMethod(assetObj, "setCurrency", currency)

    /**
     * 设置额外信息
     * @param extra 额外信息对象
     */
    fun setExtra(extra: Any) = XposedHelpers.callMethod(assetObj, "setExtra", extra)

    /**
     * 设置账户图标
     * @param icon 图标字符串
     */
    fun setIcon(icon: String) = XposedHelpers.callMethod(assetObj, "setIcon", icon)

    /**
     * 设置账户ID
     * @param id 账户ID
     */
    fun setId(id: Long) = XposedHelpers.callMethod(assetObj, "setId", id)

    /**
     * 设置是否纳入统计
     * @param incount 是否纳入统计的标志
     */
    fun setIncount(incount: Int) = XposedHelpers.callMethod(assetObj, "setIncount", incount)

    /**
     * 设置分期付款列表
     * @param list 分期付款列表
     */
    fun setInstallList(list: List<Any>) = XposedHelpers.callMethod(assetObj, "setInstallList", list)

    /**
     * 设置最后支付时间
     * @param time 最后支付时间戳
     */
    fun setLastPayTime(time: Long) = XposedHelpers.callMethod(assetObj, "setLastPayTime", time)

    /**
     * 设置借贷信息
     * @param loanInfo 借贷信息对象
     */
    fun setLoanInfo(loanInfo: LoanInfo) =
        XposedHelpers.callMethod(assetObj, "setLoanInfo", loanInfo.toObject())

    /**
     * 设置账户金额
     * @param money 金额
     */
    fun setMoney(money: Double) = XposedHelpers.callMethod(assetObj, "setMoney", money)

    /**
     * 设置账户名称
     * @param name 账户名称
     */
    fun setName(name: String) = XposedHelpers.callMethod(assetObj, "setName", name)

    /**
     * 设置账户备注
     * @param remark 备注内容
     */
    fun setRemark(remark: String) = XposedHelpers.callMethod(assetObj, "setRemark", remark)

    /**
     * 设置排序值
     * @param sort 排序序号
     */
    fun setSort(sort: Int) = XposedHelpers.callMethod(assetObj, "setSort", sort)

    /**
     * 设置账户状态
     * @param status 状态值
     */
    fun setStatus(status: Int) = XposedHelpers.callMethod(assetObj, "setStatus", status)

    /**
     * 设置账户子类型
     * @param stype 子类型值
     */
    fun setStype(stype: Int) = XposedHelpers.callMethod(assetObj, "setStype", stype)

    /**
     * 设置账户主类型
     * @param type 主类型值
     */
    fun setType(type: Int) = XposedHelpers.callMethod(assetObj, "setType", type)

    /**
     * 设置使用次数
     * @param usecount 使用次数
     */
    fun setUsecount(usecount: Int) = XposedHelpers.callMethod(assetObj, "setUsecount", usecount)

    /**
     * 设置用户ID
     * @param userid 用户ID字符串
     */
    fun setUserid(userid: String) = XposedHelpers.callMethod(assetObj, "setUserid", userid)

    /**
     * 切换账户可见性
     * 如果当前可见则设为隐藏，如果当前隐藏则设为可见
     */
    fun toggleVisible() = XposedHelpers.callMethod(assetObj, "toggleVisible")


}