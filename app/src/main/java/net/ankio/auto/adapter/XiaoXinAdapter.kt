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

package net.ankio.auto.adapter

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import io.github.oshai.kotlinlogging.KotlinLogging
import net.ankio.auto.constant.BookFeatures
import net.ankio.auto.utils.SystemUtils
import org.ezbook.server.constant.BillAction
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BillInfoModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 小星记账适配器
 * 支持通过URL Scheme进行快捷记账和自动记账弹窗功能
 *
 * 小星记账URL Scheme接口文档：
 * 1. 记账接口：xxjz://api/create?参数
 * 2. 自动记账弹窗：xxjz://api/dialog?参数
 */
class XiaoXinAdapter : IAppAdapter {

    private val logger = KotlinLogging.logger(this::class.java.name)

    override val pkg: String
        get() = "com.cxincx.xxjz" // 小星记账包名，需要根据实际情况调整

    override val link: String
        get() = "https://cxincx.com/" // 小星记账官网

    override val icon: String
        get() = "https://pp.myapp.com/ma_icon/0/icon_54171824_1756282591/256" // 图标链接，需要根据实际情况调整

    override val desc: String
        get() = "小星记账，让记账变得前所未有的简单。无论是记账小白还是资深用户，都能在几分钟内掌握核心操作，享受即刻上手的便捷！"

    override val name: String
        get() = "小星记账"

    /**
     * 小星记账支持的功能特性
     */
    override fun features(): List<BookFeatures> {
        return listOf(
            BookFeatures.MULTI_BOOK,     // 多账本支持
            BookFeatures.ASSET_MANAGE,   // 账户管理
            BookFeatures.TAG,            // 标签支持
            BookFeatures.MULTI_CURRENCY, // 多币种支持
            BookFeatures.FEE             // 手续费支持（转账）
        )
    }

    override fun supportSyncAssets(): Boolean {
        // 小星记账暂不支持资产同步
        return false
    }

    override fun syncAssets() {
        // 暂不支持资产同步
    }

    override fun syncWaitBills(billAction: BillAction) {
        // 暂不支持待处理账单同步
    }

    /**
     * 将账单同步到小星记账
     * 使用小星记账的URL Scheme接口进行记账
     */
    override fun syncBill(billInfoModel: BillInfoModel) {
        // 根据账单信息构建URL Scheme参数
        val url = buildXiaoXinUrl(billInfoModel)

        // 通过隐式Intent启动小星记账
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        logger.info { "目标应用uri：$url" }
        SystemUtils.startActivityIfResolvable(intent, name) {
            AppAdapterManager.markSynced(billInfoModel)
        }
    }

    /**
     * 构建小星记账URL Scheme
     * 优先使用精确记账接口，如果分类信息不完整则使用自动记账弹窗
     */
    private fun buildXiaoXinUrl(billInfoModel: BillInfoModel): String {
        // 判断是否使用精确记账接口
        val useCreateApi = shouldUseCreateApi(billInfoModel)

        return if (useCreateApi) {
            buildCreateApiUrl(billInfoModel)
        } else {
            buildDialogApiUrl(billInfoModel)
        }
    }

    /**
     * 判断是否应该使用精确记账接口
     * 如果分类信息完整且账户信息明确，使用create接口；否则使用dialog接口
     */
    private fun shouldUseCreateApi(billInfoModel: BillInfoModel): Boolean {
        // 基本条件：必须有分类信息和账户信息
        val hasCategory = billInfoModel.cateName.isNotEmpty() &&
                billInfoModel.cateName != "其他" &&
                billInfoModel.cateName != "其它"
        val hasAccount = billInfoModel.accountNameFrom.isNotEmpty()

        // 转账类型需要两个账户
        val isTransferValid = if (billInfoModel.type == BillType.Transfer) {
            billInfoModel.accountNameTo.isNotEmpty()
        } else true

        return hasCategory && hasAccount && isTransferValid
    }

    /**
     * 构建精确记账接口URL
     * xxjz://api/create?参数
     */
    private fun buildCreateApiUrl(billInfoModel: BillInfoModel): String {
        val params = mutableMapOf<String, String>()

        // 必填参数：类型、金额、账户
        params["type"] = mapBillTypeToXiaoXin(billInfoModel.type)
        params["amount"] = billInfoModel.money.toString()
        params["account"] = billInfoModel.accountNameFrom

        // 分类处理：支持一二级分类（默认单级在前）
        val (parent, child) = billInfoModel.categoryPair()
        if (parent.isNotEmpty()) params["parent"] = parent
        if (child.isNotEmpty()) params["child"] = child

        // 转账和借贷需要第二个账户
        if (billInfoModel.type == BillType.Transfer ||
            billInfoModel.type == BillType.IncomeLending ||
            billInfoModel.type == BillType.ExpendLending ||
            billInfoModel.type == BillType.IncomeRepayment ||
            billInfoModel.type == BillType.ExpendRepayment
        ) {
            if (billInfoModel.accountNameTo.isNotEmpty()) {
                params["account2"] = billInfoModel.accountNameTo
            }
        }

        // 可选参数
        addOptionalParams(params, billInfoModel)

        return buildUrl("xxjz://api/create", params)
    }

    /**
     * 构建自动记账弹窗接口URL
     * xxjz://api/dialog?参数
     */
    private fun buildDialogApiUrl(billInfoModel: BillInfoModel): String {
        val params = mutableMapOf<String, String>()

        // 基本参数
        params["type"] = mapBillTypeToXiaoXin(billInfoModel.type)
        params["amount"] = billInfoModel.money.toString()

        // 账户信息（作为关键字传递）
        if (billInfoModel.accountNameFrom.isNotEmpty()) {
            params["account"] = billInfoModel.accountNameFrom
        }
        if (billInfoModel.accountNameTo.isNotEmpty()) {
            params["account2"] = billInfoModel.accountNameTo
        }

        // 商户信息（作为关键字）
        if (billInfoModel.shopName.isNotEmpty()) {
            params["shop"] = billInfoModel.shopName
        }

        // 渠道信息
        if (billInfoModel.channel.isNotEmpty()) {
            params["channel"] = billInfoModel.channel
        }

        // 可选参数
        addOptionalParams(params, billInfoModel)

        return buildUrl("xxjz://api/dialog", params)
    }

    /**
     * 添加通用可选参数
     */
    private fun addOptionalParams(
        params: MutableMap<String, String>,
        billInfoModel: BillInfoModel
    ) {
        // 备注
        if (billInfoModel.remark.isNotEmpty()) {
            params["remark"] = billInfoModel.remark
        }

        // 账本
        if (billInfoModel.bookName.isNotEmpty() &&
            billInfoModel.bookName != "默认账本" &&
            billInfoModel.bookName != "日常账本"
        ) {
            params["book"] = billInfoModel.bookName
        }

        // 标签（空格分隔）
        if (billInfoModel.tags.isNotEmpty()) {
            val tagList = billInfoModel.getTagList()
            if (tagList.isNotEmpty()) {
                params["tag"] = tagList.joinToString(" ")
            }
        }

        // 商家
        if (billInfoModel.shopName.isNotEmpty()) {
            params["shop"] = billInfoModel.shopName
        }

        // 币种
        if (billInfoModel.currency.isNotEmpty() && billInfoModel.currency != "CNY") {
            params["currency"] = billInfoModel.currency
        }

        // 时间
        if (billInfoModel.time > 0) {
            params["time"] = formatTime(billInfoModel.time)
        }
    }

    /**
     * 将内部账单类型映射为小星记账类型
     */
    private fun mapBillTypeToXiaoXin(type: BillType): String {
        return when (type) {
            BillType.Expend,
            BillType.ExpendReimbursement,
            BillType.ExpendLending,
            BillType.ExpendRepayment -> "支出"

            BillType.Income,
            BillType.IncomeLending,
            BillType.IncomeRepayment,
            BillType.IncomeReimbursement,
            BillType.IncomeRefund -> "收入"

            BillType.Transfer -> "转账"
        }
    }

    /**
     * 构建URL字符串
     */
    private fun buildUrl(baseUrl: String, params: Map<String, String>): String {
        if (params.isEmpty()) return baseUrl

        val paramString = params.entries.joinToString("&") { (key, value) ->
            "$key=${Uri.encode(value)}"
        }
        return "$baseUrl?$paramString"
    }

    /**
     * 格式化时间为小星记账要求的格式
     */
    private fun formatTime(timeMillis: Long): String {
        val date = Date(timeMillis)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(date)
    }

    override fun sleep(): Long {
        return 0L
    }
}