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
import net.ankio.auto.xposed.core.api.HookerClazz
import net.ankio.dex.model.Clazz

/**
 * 账单信息模型类
 * 用于处理钱迹App中的账单相关数据
 */
class QjBillModel {


    // 存储实际的账单信息对象
    private var billObj: Any? = null

    companion object : HookerClazz() {
        // 钱迹App中账单信息类的完整类名
        private const val CLAZZ = "com.mutangtech.qianji.data.model.Bill"

        // 通过 Hooker 规则加载账单类
        private val billClazz by lazy { clazz() }

        // 提供给 Manifest 的规则定义（精确类名，保持简洁稳定）
        override var rule = Clazz(name = this::class.java.name, nameRule = CLAZZ)

        // 账户连接符
        const val ACCOUNT_CONNECT = "->"

        // 账单类型常量
        const val ALL = -1
        const val SPEND = 0
        const val INCOME = 1
        const val TRANSFER = 2
        const val CREDIT_HUANKUAN = 3
        const val BAOXIAO = 5

        // 账单平台类型
        const val BILL_PLATFORM_ALL = -1
        const val BILL_PLATFORM_BY_HAND = 0
        const val BILL_PLATFORM_REPEAT_TASK_OLD = 1
        const val BILL_PLATFORM_IMPORT = 2
        const val BILL_PLATFORM_REPEAT_TASK = 120
        const val BILL_PLATFORM_INSTALLMENT = 121

        // 账单状态
        const val STATUS_DELETE = 0
        const val STATUS_OK = 1
        const val STATUS_NOT_SYNC = 2


        const val ZHAIWU_DEBT: Int = 6

        const val ZHAIWU_HUANKUAN: Int = 9
        const val ZHAIWU_LIXI_INCOME: Int = 11
        const val ZHAIWU_LIXI_SPEND: Int = 10
        const val ZHAIWU_LOAN: Int = 7
        const val ZHAIWU_SHOUKUAN: Int = 4

        /**
         * 从现有对象创建Bill实例
         * @param obj 原始账单信息对象
         * @return 包装后的Bill对象
         */
        fun fromObject(obj: Any): QjBillModel {
            if (obj::class.java.name != CLAZZ) {
                throw IllegalArgumentException("${obj::class.java.name} must be instance of $CLAZZ")
            }
            val bill = QjBillModel()
            bill.billObj = obj
            return bill
        }

        /**
         * 检查是否可以使用优惠券
         * @param type 账单类型
         */
        fun canUseCoupon(type: Int): Boolean {
            return XposedHelpers.callStaticMethod(
                billClazz,
                "canUseCoupon",
                type
            ) as Boolean
        }

        /**
         * 获取所有支出类型
         */
        fun getAllSpendTypes(): IntArray {
            return XposedHelpers.callStaticMethod(
                billClazz,
                "getAllSpendTypes"
            ) as IntArray
        }

        /**
         * 检查是否为转账类型
         * @param type 账单类型
         * @return 是否为转账
         */
        fun isAllTransfer(type: Int): Boolean =
            type == TRANSFER || type == CREDIT_HUANKUAN

        /**
         * 检查是否为账单类型
         * @param type 账单类型
         * @return 是否为账单类型
         */
        fun isBillType(type: Int): Boolean =
            type == SPEND || type == INCOME || type == BAOXIAO

        /**
         * 检查是否为债务记录
         * @param type 账单类型
         * @return 是否为债务记录
         */
        fun isDebtRecord(type: Int): Boolean = type == ZHAIWU_DEBT

        /**
         * 检查是否支持报销类型
         * @param type 账单类型
         * @return 是否支持报销
         */
        fun isSupportBaoxiaoType(type: Int): Boolean = type == SPEND

        /**
         * 检查是否为债务类型
         * @param type 账单类型
         * @return 是否为债务类型
         */
        fun isZhaiwuType(type: Int): Boolean = when (type) {
            ZHAIWU_SHOUKUAN, ZHAIWU_DEBT, ZHAIWU_LOAN,
            ZHAIWU_HUANKUAN, ZHAIWU_LIXI_SPEND, ZHAIWU_LIXI_INCOME -> true

            else -> false
        }

        /**
         * 创建新的账单实例
         * @param type 账单类型
         * @param remark 备注
         * @param money 金额
         * @param timeInSec 时间戳
         * @param images 图片列表
         * @return 新建的账单对象
         */
        fun newInstance(
            type: Int,
            remark: String,
            money: Double,
            timeInSec: Long,
            images: ArrayList<String>
        ): QjBillModel {
            val obj = XposedHelpers.callStaticMethod(
                billClazz,
                "newInstance",
                type,
                remark,
                money,
                timeInSec,
                images
            )
            return fromObject(obj)
        }

        /**
         * 设置债务当前资产
         * @param bill 账单对象
         * @param assetAccount 资产账户
         */
        fun setZhaiwuCurrentAsset(bill: QjBillModel, assetAccount: QjAssetAccountModel) {
            XposedHelpers.callStaticMethod(
                billClazz,
                "setZhaiwuCurrentAsset",
                bill.billObj,
                assetAccount.toObject()
            )
        }

        /**
         * 获取账单标记导出值
         */
        private fun getBillFlagExportValue(context: Context, bill: QjBillModel): String? {
            return XposedHelpers.callStaticMethod(
                billClazz,
                "getBillFlagExportValue",
                context,
                bill.toObject()
            ) as String?
        }

        /**
         * 获取CSV标题
         */
        @Suppress("UNCHECKED_CAST")
        fun getCsvTitle(): Array<String> {
            return XposedHelpers.callStaticMethod(
                billClazz,
                "getCsvTitle"
            ) as Array<String>
        }

        /**
         * 获取图片URL
         */
        private fun getImageUrl(str: String, isHuge: Boolean): String {
            return XposedHelpers.callStaticMethod(
                billClazz,
                "getImageUrl",
                str,
                isHuge
            ) as String
        }

        /**
         * 根据类型获取金额颜色
         */
        fun getMoneyColorByType(context: Context, type: Int): Int {
            return XposedHelpers.callStaticMethod(
                billClazz,
                "getMoneyColorByType",
                context,
                type
            ) as Int
        }

        /**
         * 获取支出金额符号
         */
        fun getOutMoneySign(bill: QjBillModel): String? {
            return XposedHelpers.callStaticMethod(
                billClazz,
                "getOutMoneySign",
                bill.toObject()
            ) as String?
        }

        /**
         * 获取源账单ID
         */
        private fun getSourceBillId(bill: QjBillModel?): String? {
            return XposedHelpers.callStaticMethod(
                billClazz,
                "getSourceBillId",
                bill?.toObject()
            ) as String?
        }

        /**
         * 获取类型字符串
         */
        fun getTypeString(context: Context, type: Int): String {
            return XposedHelpers.callStaticMethod(
                billClazz,
                "getTypeString",
                context,
                type
            ) as String
        }

        /**
         * 获取用户名
         */
        private fun getUserName(bill: QjBillModel): String {
            return XposedHelpers.callStaticMethod(
                billClazz,
                "getUserName",
                bill.toObject()
            ) as String
        }



        /**
         * 获取债务相关资产ID
         */
        fun getZhaiwuAboutAssetId(bill: QjBillModel): Long {
            return XposedHelpers.callStaticMethod(
                billClazz,
                "getZhaiwuAboutAssetId",
                bill.toObject()
            ) as Long
        }

        /**
         * 获取债务当前资产ID
         */
        fun getZhaiwuCurrentAssetId(bill: QjBillModel): Long {
            return XposedHelpers.callStaticMethod(
                billClazz,
                "getZhaiwuCurrentAssetId",
                bill.toObject()
            ) as Long
        }

        /**
         * 检查是否为大图片
         */
        fun isHugeImage(str: String): Boolean {
            return XposedHelpers.callStaticMethod(
                billClazz,
                "isHugeImage",
                str
            ) as Boolean
        }

        /**
         * 创建新的打包账单
         */
        fun newPackBill(bill: QjBillModel): QjBillModel {
            val newBill = XposedHelpers.callStaticMethod(
                billClazz,
                "newPackBill",
                bill.toObject()
            )
            return fromObject(newBill)
        }

        /**
         * 解析大图
         */
        fun parseLargeImage(str: String): String {
            return XposedHelpers.callStaticMethod(
                billClazz,
                "parseLargeImage",
                str
            ) as String
        }

        /**
         * 解析小图
         */
        fun parseSmallImage(str: String): String {
            return XposedHelpers.callStaticMethod(
                billClazz,
                "parseSmallImage",
                str
            ) as String
        }

        /**
         * 准备导出图片
         */
        private fun prepareExportImages(images: ArrayList<String>): Any? {
            return XposedHelpers.callStaticMethod(
                billClazz,
                "prepareExportImages",
                images
            )
        }

        /**
         * 设置债务相关资产
         */
        fun setZhaiwuAboutAsset(bill: QjBillModel, assetAccount: QjAssetAccountModel) {
            XposedHelpers.callStaticMethod(
                billClazz,
                "setZhaiwuAboutAsset",
                bill.toObject(),
                assetAccount.toObject()
            )
        }

        /**
         * 导出为CSV
         */
        @Suppress("UNCHECKED_CAST")
        fun toExportCsv(context: Context, bill: QjBillModel): Array<String>? {
            return XposedHelpers.callStaticMethod(
                billClazz,
                "toExportCsv",
                context,
                bill.toObject()
            ) as Array<String>?
        }

        /**
         * 包装账单ID
         */
        private fun wrapBillId(id: Long): String {
            return XposedHelpers.callStaticMethod(
                billClazz,
                "wrapBillId",
                id
            ) as String
        }

    }

    fun toObject(): Any? = billObj


    /**
     * 添加打包账单
     */
    fun addPackBill(bill: QjBillModel?) {
        XposedHelpers.callMethod(
            billObj,
            "addPackBill",
            bill?.toObject()
        )
    }

    /**
     * 克隆账单
     */
    fun clone(bill: QjBillModel?) {
        XposedHelpers.callMethod(
            billObj,
            "clone",
            bill?.toObject()
        )
    }

    /**
     * 比较账单
     */
    fun compareTo(bill: QjBillModel): Int {
        return XposedHelpers.callMethod(
            billObj,
            "compareTo",
            bill.toObject()
        ) as Int
    }

    /**
     * 获取资产ID
     */
    fun getAssetid(): Long {
        return XposedHelpers.callMethod(
            billObj,
            "getAssetid"
        ) as Long
    }

    /**
     * 获取旧版报销资产ID
     */
    @Deprecated("使用新版报销接口")
    fun getBaoXiaoAssetIdOld(): Long {
        return XposedHelpers.callMethod(
            billObj,
            "getBaoXiaoAssetIdOld"
        ) as Long
    }

    /**
     * 获取新版报销资产ID
     */
    fun getBaoxiaoAssetIdV2(): Long {
        return XposedHelpers.callMethod(
            billObj,
            "getBaoxiaoAssetIdV2"
        ) as Long
    }

    /**
     * 获取兼容版本的报销金额
     */
    fun getBaoxiaoedMoneyCompat(): Double {
        return XposedHelpers.callMethod(
            billObj,
            "getBaoxiaoedMoneyCompat"
        ) as Double
    }

    /**
     * 获取旧版报销金额
     */
    @Deprecated("使用新版报销接口")
    fun getBaoxiaoedMoneyOld(): Double {
        return XposedHelpers.callMethod(
            billObj,
            "getBaoxiaoedMoneyOld"
        ) as Double
    }

    /**
     * 获取新版报销金额
     */
    fun getBaoxiaoedMoneyV2(): Double {
        return XposedHelpers.callMethod(
            billObj,
            "getBaoxiaoedMoneyV2"
        ) as Double
    }

    /**
     * 获取账单ID
     */
    fun getBillid(): Long {
        return XposedHelpers.callMethod(
            billObj,
            "getBillid"
        ) as Long
    }

    /**
     * 获取账本ID
     */
    fun getBookId(): Long {
        return XposedHelpers.callMethod(
            billObj,
            "getBookId"
        ) as Long
    }

    /**
     * 获取账本名称
     */
    fun getBookName(): String? {
        return XposedHelpers.callMethod(
            billObj,
            "getBookName"
        ) as String?
    }

    /**
     * 获取分类
     */
    fun getCategory(): QjCategoryModel? {
        return XposedHelpers.callMethod(
            billObj,
            "getCategory"
        )?.let { QjCategoryModel.fromObject(it) }
    }

    /**
     * 获取分类ID
     */
    fun getCategoryId(): Long {
        return XposedHelpers.callMethod(
            billObj,
            "getCategoryId"
        ) as Long
    }

    /**
     * 获取创建时间
     */
    fun getCreatetimeInSec(): Long {
        return XposedHelpers.callMethod(
            billObj,
            "getCreatetimeInSec"
        ) as Long
    }


    /**
     * 获取描述信息
     */
    fun getDescinfo(): String? {
        return XposedHelpers.callMethod(
            billObj,
            "getDescinfo"
        ) as String?
    }

    /**
     * 获取报销账单ID列表
     */
    @Suppress("UNCHECKED_CAST")
    fun getEDBaoXiaoBillIds(): List<Long>? {
        return XposedHelpers.callMethod(
            billObj,
            "getEDBaoXiaoBillIds"
        ) as List<Long>?
    }


    /**
     * 获取手续费金额字符串
     */
    fun getFeeMoneyStr(context: Context): CharSequence? {
        return XposedHelpers.callMethod(
            billObj,
            "getFeeMoneyStr",
            context
        ) as CharSequence?
    }

    /**
     * 获取第一张图片
     */
    fun getFirstImage(): String? {
        return XposedHelpers.callMethod(
            billObj,
            "getFirstImage"
        ) as String?
    }

    /**
     * 获取来源账户
     */
    fun getFromact(): String? {
        return XposedHelpers.callMethod(
            billObj,
            "getFromact"
        ) as String?
    }

    /**
     * 获取来源ID
     */
    fun getFromid(): Long {
        return XposedHelpers.callMethod(
            billObj,
            "getFromid"
        ) as Long
    }

    /**
     * 获取图片列表
     */
    @Suppress("UNCHECKED_CAST")
    fun getImages(): ArrayList<String>? {
        return XposedHelpers.callMethod(
            billObj,
            "getImages"
        ) as ArrayList<String>?
    }

    /**
     * 获取导入包ID
     */
    fun getImportPackId(): Long {
        return XposedHelpers.callMethod(
            billObj,
            "getImportPackId"
        ) as Long
    }

    /**
     * 获取分期手续费
     */
    fun getInstallmentFee(): Double {
        return XposedHelpers.callMethod(
            billObj,
            "getInstallmentFee"
        ) as Double
    }

    /**
     * 获取金额
     */
    fun getMoney(): Double {
        return XposedHelpers.callMethod(
            billObj,
            "getMoney"
        ) as Double
    }

    /**
     * 获取统计用金额
     */
    fun getMoneyForStat(str: String?): Double {
        return XposedHelpers.callMethod(
            billObj,
            "getMoneyForStat",
            str
        ) as Double
    }

    /**
     * 获取金额字符串
     */
    fun getMoneyStr(context: Context): CharSequence? {
        return XposedHelpers.callMethod(
            billObj,
            "getMoneyStr",
            context
        ) as CharSequence?
    }

    /**
     * 获取支付类型
     */
    @Deprecated("已弃用")
    fun getPaytype(): Int {
        return XposedHelpers.callMethod(
            billObj,
            "getPaytype"
        ) as Int
    }

    /**
     * 获取平台
     */
    fun getPlatform(): Int {
        return XposedHelpers.callMethod(
            billObj,
            "getPlatform"
        ) as Int
    }

    /**
     * 获取实际金额
     */
    fun getRealMoney(): Double {
        return XposedHelpers.callMethod(
            billObj,
            "getRealMoney"
        ) as Double
    }

    /**
     * 获取退款账单ID列表
     */
    @Suppress("UNCHECKED_CAST")
    fun getRefundBillIds(): List<Long>? {
        return XposedHelpers.callMethod(
            billObj,
            "getRefundBillIds"
        ) as List<Long>?
    }

    /**
     * 获取退款金额字符串
     */
    fun getRefundMoneyStr(context: Context): CharSequence? {
        return XposedHelpers.callMethod(
            billObj,
            "getRefundMoneyStr",
            context
        ) as CharSequence?
    }

    /**
     * 获取退款源账单ID
     */
    fun getRefundSourceBillId(): Long {
        return XposedHelpers.callMethod(
            billObj,
            "getRefundSourceBillId"
        ) as Long
    }

    /**
     * 获取备注
     */
    fun getRemark(): String? {
        return XposedHelpers.callMethod(
            billObj,
            "getRemark"
        ) as String?
    }

    /**
     * 获取短日期字符串
     */
    fun getShortDateStr(): String? {
        return XposedHelpers.callMethod(
            billObj,
            "getShortDateStr"
        ) as String?
    }

    /**
     * 获取状态
     */
    fun getStatus(): Int {
        return XposedHelpers.callMethod(
            billObj,
            "getStatus"
        ) as Int
    }

    /**
     * 获取子金额字符串
     */
    fun getSubMoneyStr(context: Context, showDesc: Boolean, showExtra: Boolean): CharSequence? {
        return XposedHelpers.callMethod(
            billObj,
            "getSubMoneyStr",
            context,
            showDesc,
            showExtra
        ) as CharSequence?
    }

    /**
     * 获取目标账户
     */
    fun getTargetact(): String? {
        return XposedHelpers.callMethod(
            billObj,
            "getTargetact"
        ) as String?
    }

    /**
     * 获取目标ID
     */
    fun getTargetid(): Long {
        return XposedHelpers.callMethod(
            billObj,
            "getTargetid"
        ) as Long
    }

    /**
     * 获取时间戳
     */
    fun getTimeInSec(): Long {
        return XposedHelpers.callMethod(
            billObj,
            "getTimeInSec"
        ) as Long
    }

    /**
     * 获取标题
     */
    fun getTitle(context: Context): String? {
        return XposedHelpers.callMethod(
            billObj,
            "getTitle",
            context
        ) as String?
    }

    /**
     * 获取总报销金额统计
     */
    fun getTotalEDBaoxiaoMoneyStat(str: String?): Double {
        return XposedHelpers.callMethod(
            billObj,
            "getTotalEDBaoxiaoMoneyStat",
            str
        ) as Double
    }

    /**
     * 获取总退款金额统计
     */
    fun getTotalRefundMoneyStat(str: String?): Double {
        return XposedHelpers.callMethod(
            billObj,
            "getTotalRefundMoneyStat",
            str
        ) as Double
    }

    /**
     * 获取手续费
     */
    fun getTransFee(): Double {
        return XposedHelpers.callMethod(
            billObj,
            "getTransFee"
        ) as Double
    }

    /**
     * 获取统计用手续费
     */
    fun getTransFeeForStat(str: String?): Double {
        return XposedHelpers.callMethod(
            billObj,
            "getTransFeeForStat",
            str
        ) as Double
    }

    /**
     * 获取类型
     */
    fun getType(): Int {
        return XposedHelpers.callMethod(
            billObj,
            "getType"
        ) as Int
    }

    /**
     * 获取更新时间
     */
    fun getUpdateTimeInSec(): Long {
        return XposedHelpers.callMethod(
            billObj,
            "getUpdateTimeInSec"
        ) as Long
    }

    /**
     * 获取用户ID
     */
    fun getUserid(): String? {
        return XposedHelpers.callMethod(
            billObj,
            "getUserid"
        ) as String?
    }

    /**
     * 获取用户名
     */
    fun getUsername(): String? {
        return XposedHelpers.callMethod(
            billObj,
            "getUsername"
        ) as String?
    }

    /**
     * 获取ID
     */
    fun get_id(): Long? {
        return XposedHelpers.callMethod(
            billObj,
            "get_id"
        ) as Long?
    }

    /**
     * 检查是否有兼容版本的报销
     */
    fun hasBaoXiaoedCompat(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "hasBaoXiaoedCompat"
        ) as Boolean
    }

    /**
     * 检查是否有旧版报销
     */
    @Deprecated("使用新版报销接口")
    fun hasBaoXiaoedOld(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "hasBaoXiaoedOld"
        ) as Boolean
    }

    /**
     * 检查是否有新版报销
     */
    fun hasBaoXiaoedV2(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "hasBaoXiaoedV2"
        ) as Boolean
    }

    /**
     * 检查是否有自定义报销金额
     */
    fun hasCustomeBaoxiaoMoney(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "hasCustomeBaoxiaoMoney"
        ) as Boolean
    }


    /**
     * 检查是否有多个打包账单
     */
    fun hasMultiplePackBills(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "hasMultiplePackBills"
        ) as Boolean
    }

    /**
     * 检查是否有退款
     */
    fun hasRefund(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "hasRefund"
        ) as Boolean
    }

    /**
     * 检查是否为收入
     */
    fun isAllIncome(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "isAllIncome"
        ) as Boolean
    }

    /**
     * 检查是否为支出
     */
    fun isAllSpend(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "isAllSpend"
        ) as Boolean
    }

    /**
     * 检查是否为报销
     */
    fun isBaoXiao(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "isBaoXiao"
        ) as Boolean
    }

    /**
     * 检查是否为信用卡还款
     */
    fun isCreditHuanKuan(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "isCreditHuanKuan"
        ) as Boolean
    }

    /**
     * 检查是否为报销账单
     */
    fun isEDBaoxiao(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "isEDBaoxiao"
        ) as Boolean
    }

    /**
     * 检查是否来自重复任务
     */
    fun isFromRepeatTask(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "isFromRepeatTask"
        ) as Boolean
    }

    /**
     * 检查是否为收入
     */
    fun isIncome(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "isIncome"
        ) as Boolean
    }

    /**
     * 检查是否不计入预算
     */
    fun isNotBudget(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "isNotBudget"
        ) as Boolean
    }

    /**
     * 检查是否不计入统计
     */
    fun isNotCount(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "isNotCount"
        ) as Boolean
    }

    /**
     * 检查是否为打包报销
     */
    fun isPackBaoxiao(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "isPackBaoxiao"
        ) as Boolean
    }

    /**
     * 检查是否为退款
     */
    fun isRefund(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "isRefund"
        ) as Boolean
    }

    /**
     * 检查是否为支出
     */
    fun isSpend(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "isSpend"
        ) as Boolean
    }

    /**
     * 检查是否支持退款类型
     */
    fun isSupportRefundType(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "isSupportRefundType"
        ) as Boolean
    }

    /**
     * 检查是否为转账
     */
    fun isTransfer(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "isTransfer"
        ) as Boolean
    }

    /**
     * 检查是否为债务还款
     */
    fun isZhaiWuHuanKuan(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "isZhaiWuHuanKuan"
        ) as Boolean
    }

    /**
     * 检查是否为债务利息收入
     */
    fun isZhaiWuLiXiIncome(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "isZhaiWuLiXiIncome"
        ) as Boolean
    }

    /**
     * 检查是否为债务利息支出
     */
    fun isZhaiWuLiXiSpend(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "isZhaiWuLiXiSpend"
        ) as Boolean
    }

    fun getExtra(): BillExtraModel {
        val extra = XposedHelpers.getObjectField(
            billObj,
            "extra"
        )
        return BillExtraModel(extra)
    }

    fun setExtra(extra: BillExtraModel) {
        XposedHelpers.setObjectField(
            billObj,
            "extra", extra.toObject()
        )
    }

    /**
     * 检查是否为债务收款
     */
    fun isZhaiWuShouKuan(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "isZhaiWuShouKuan"
        ) as Boolean
    }

    /**
     * 检查是否为债务
     */
    fun isZhaiwuDebt(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "isZhaiwuDebt"
        ) as Boolean
    }

    /**
     * 检查是否为债务借款
     */
    fun isZhaiwuLoan(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "isZhaiwuLoan"
        ) as Boolean
    }

    /**
     * 重置金额字符串
     */
    fun resetMoneyStr() {
        XposedHelpers.callMethod(
            billObj,
            "resetMoneyStr"
        )
    }

    /**
     * 设置资产
     */
    fun setAsset(assetAccount: QjAssetAccountModel?) {
        XposedHelpers.callMethod(
            billObj,
            "setAsset",
            assetAccount?.toObject()
        )
    }

    /**
     * 设置资产ID
     */
    @Deprecated("已弃用")
    fun setAssetid(id: Long) {
        XposedHelpers.callMethod(
            billObj,
            "setAssetid",
            id
        )
    }

    /**
     * 设置账单ID
     */
    fun setBillid(id: Long) {
        XposedHelpers.callMethod(
            billObj,
            "setBillid",
            id
        )
    }

    /**
     * 设置账本
     */
    fun setBook(book: QjBookModel) {
        XposedHelpers.callMethod(
            billObj,
            "setBook",
            book.toObject()
        )
    }

    /**
     * 设置账本ID
     */
    fun setBookId(id: Long) {
        XposedHelpers.callMethod(
            billObj,
            "setBookId",
            id
        )
    }

    /**
     * 设置账本名称
     */
    fun setBookName(name: String?) {
        XposedHelpers.callMethod(
            billObj,
            "setBookName",
            name
        )
    }

    /**
     * 设置分类
     */
    fun setCategory(category: QjCategoryModel) {
        XposedHelpers.callMethod(
            billObj,
            "setCategory",
            category.toObject()
        )
    }

    /**
     * 设置分类ID
     */
    fun setCategoryId(id: Long) {
        XposedHelpers.callMethod(
            billObj,
            "setCategoryId",
            id
        )
    }

    /**
     * 设置创建时间
     */
    fun setCreatetimeInSec(time: Long) {
        XposedHelpers.callMethod(
            billObj,
            "setCreatetimeInSec",
            time
        )
    }


    /**
     * 设置描述信息
     */
    fun setDescinfo(desc: String?) {
        XposedHelpers.callMethod(
            billObj,
            "setDescinfo",
            desc
        )
    }


    /**
     * 设置标记
     */
    fun setFlag(flag: Int) {
        XposedHelpers.callMethod(
            billObj,
            "setFlag",
            flag
        )
    }

    /**
     * 设置来源账户
     */
    fun setFromact(fromact: String?) {
        XposedHelpers.callMethod(
            billObj,
            "setFromact",
            fromact
        )
    }

    /**
     * 设置来源ID
     */
    @Deprecated("已弃用")
    fun setFromid(id: Long) {
        XposedHelpers.callMethod(
            billObj,
            "setFromid",
            id
        )
    }

    /**
     * 设置图片列表
     */
    fun setImages(images: ArrayList<String>?) {
        XposedHelpers.callMethod(
            billObj,
            "setImages",
            images
        )
    }

    /**
     * 设置导入包ID
     */
    fun setImportPackId(id: Long) {
        XposedHelpers.callMethod(
            billObj,
            "setImportPackId",
            id
        )
    }

    /**
     * 设置金额
     */
    fun setMoney(money: Double) {
        XposedHelpers.callMethod(
            billObj,
            "setMoney",
            money
        )
    }

    /**
     * 设置支付类型
     */
    @Deprecated("已弃用")
    fun setPaytype(type: Int) {
        XposedHelpers.callMethod(
            billObj,
            "setPaytype",
            type
        )
    }

    /**
     * 设置平台
     */
    fun setPlatform(platform: Int) {
        XposedHelpers.callMethod(
            billObj,
            "setPlatform",
            platform
        )
    }

    /**
     * 设置备注
     */
    fun setRemark(remark: String?) {
        XposedHelpers.callMethod(
            billObj,
            "setRemark",
            remark
        )
    }

    /**
     * 设置状态
     */
    fun setStatus(status: Int) {
        XposedHelpers.callMethod(
            billObj,
            "setStatus",
            status
        )
    }

    /**
     * 设置标签列表
     */
    fun setTagList(tags: ArrayList<QjTagModel>?) {
        XposedHelpers.callMethod(
            billObj,
            "setTagList",
            tags?.map { it.toObject() }
        )
    }

    /**
     * 设置目标账户
     */
    fun setTargetact(targetact: String?) {
        XposedHelpers.callMethod(
            billObj,
            "setTargetact",
            targetact
        )
    }

    /**
     * 设置目标ID
     */
    @Deprecated("已弃用")
    fun setTargetid(id: Long) {
        XposedHelpers.callMethod(
            billObj,
            "setTargetid",
            id
        )
    }

    /**
     * 设置时间戳
     */
    fun setTimeInSec(time: Long) {
        XposedHelpers.callMethod(
            billObj,
            "setTimeInSec",
            time
        )
    }

    /**
     * 设置转账资产
     */
    fun setTransferAsset(from: QjAssetAccountModel, to: QjAssetAccountModel, fee: Double) {
        XposedHelpers.callMethod(
            billObj,
            "setTransferAsset",
            from.toObject(),
            to.toObject(),
            fee
        )
    }

    /**
     * 设置类型
     */
    fun setType(type: Int) {
        XposedHelpers.callMethod(
            billObj,
            "setType",
            type
        )
    }

    /**
     * 设置更新时间
     */
    fun setUpdateTimeInSec(time: Long) {
        XposedHelpers.callMethod(
            billObj,
            "setUpdateTimeInSec",
            time
        )
    }

    /**
     * 设置用户ID
     */
    fun setUserid(userid: String?) {
        XposedHelpers.callMethod(
            billObj,
            "setUserid",
            userid
        )
    }

    /**
     * 设置用户名
     */
    fun setUsername(username: String?) {
        XposedHelpers.callMethod(
            billObj,
            "setUsername",
            username
        )
    }

    /**
     * 设置ID
     */
    fun set_id(id: Long?) {
        XposedHelpers.callMethod(
            billObj,
            "set_id",
            id
        )
    }

    /**
     * 检查是否显示平台
     */
    fun showPlatform(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "showPlatform"
        ) as Boolean
    }

    /**
     * 检查是否支持子账单
     */
    fun supportSubBill(): Boolean {
        return XposedHelpers.callMethod(
            billObj,
            "supportSubBill"
        ) as Boolean
    }

    /**
     * 撤销报销
     */
    fun undoBaoxiao() {
        XposedHelpers.callMethod(
            billObj,
            "undoBaoxiao"
        )
    }

    /**
     * 设置债务当前资产（实例便捷方法，委托到静态实现）
     */
    fun setZhaiwuCurrentAsset(bill: QjBillModel, accountFrom: QjAssetAccountModel) {
        QjBillModel.setZhaiwuCurrentAsset(bill, accountFrom)
    }
    override fun toString(): String = XposedHelpers.callMethod(billObj, "toString") as String

}