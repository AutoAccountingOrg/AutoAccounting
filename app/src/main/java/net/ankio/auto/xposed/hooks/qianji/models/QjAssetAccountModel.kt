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
 * 资产账户模型类
 * 用于处理钱迹App中的资产账户相关数据
 */
class QjAssetAccountModel {


    // 存储实际的资产账户对象
    var assetObj: Any? = null

    // 常量在当前工程未使用，保持最小实现，避免噪音

    companion object : HookerClazz() {
        // 钱迹App中资产账户类的完整类名
        private const val CLAZZ = "com.mutangtech.qianji.data.model.AssetAccount"

        // 加载钱迹资产账户类
        private val assetClazz by lazy { clazz() }

        /**
         * 从现有对象创建AssetAccount实例
         * @param obj 原始资产账户对象
         * @return 包装后的AssetAccount对象
         */
        fun fromObject(obj: Any): QjAssetAccountModel {
            if (obj::class.java.name != CLAZZ) {
                throw IllegalArgumentException("${obj::class.java.name} is not a valid AssetAccount object")
            }
            val assetAccount = QjAssetAccountModel()
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
        fun newInstance(type: Int, stype: Int, name: String): QjAssetAccountModel {
            //  public static AssetAccount newInstance(int i10, int i11) {
            //        AssetAccount assetAccount = new AssetAccount();
            //        assetAccount.type = i10;
            //        assetAccount.stype = i11;
            //        return assetAccount;
            //    }

            val assetAccount = QjAssetAccountModel()
            assetAccount.assetObj =
                XposedHelpers.callStaticMethod(assetClazz, "newInstance", type, stype)
            assetAccount.setName(name)
            return assetAccount
        }

        override var rule = Clazz(nameRule = CLAZZ, name = this::class.java.name)


    }

    fun toObject(): Any? = assetObj

    /**
     * 增加账户金额
     * @param money 要增加的金额
     */
    fun addMoney(money: Double) = XposedHelpers.callMethod(assetObj, "addMoney", money)


    fun getIcon(): String = XposedHelpers.callMethod(assetObj, "getIcon") as String

    fun getId(): Long = XposedHelpers.callMethod(assetObj, "getId") as Long


    fun getLoanInfo(): LoanInfoModel =
        LoanInfoModel.fromObject(XposedHelpers.callMethod(assetObj, "getLoanInfo"))

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
    /**
     * 获取账户排序值
     * @return 排序序号
     */
    fun getSort(): Int = XposedHelpers.callMethod(assetObj, "getSort") as Int

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
     * 获取货币类型代码，如 CNY、USD。
     */
    fun getCurrency(): String = XposedHelpers.callMethod(assetObj, "getCurrency") as String

    /**
     * 判断账户是否纳入统计
     * @return 是否纳入统计
     */
    // 业务只依赖可见性与债务结清状态，其余判断移除以降低噪音

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
     * 设置账户图标
     * @param icon 图标字符串
     */
    fun setIcon(icon: String) = XposedHelpers.callMethod(assetObj, "setIcon", icon)

    /**
     * 设置是否纳入统计
     * @param incount 是否纳入统计的标志
     */
    fun setIncount(incount: Int) = XposedHelpers.callMethod(assetObj, "setIncount", incount)

    /**
     * 设置借贷信息
     * @param loanInfo 借贷信息对象
     */
    fun setLoanInfo(loanInfo: LoanInfoModel) =
        XposedHelpers.callMethod(assetObj, "setLoanInfo", loanInfo.toObject())

    /**
     * 设置账户名称
     * @param name 账户名称
     */
    fun setName(name: String) = XposedHelpers.callMethod(assetObj, "setName", name)


}