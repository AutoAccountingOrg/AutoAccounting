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
import net.ankio.auto.storage.Logger
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
 * 仅使用 URL Scheme 精确记账接口
 *
 * 接口：xxjz://api/create?参数
 */
class XiaoXinAdapter : IAppAdapter {

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

    override fun syncWaitBills(billAction: BillAction, bookName: String) {
        // 暂不支持待处理账单同步
    }

    /**
     * 将账单同步到小星记账
     * 使用小星记账的URL Scheme接口进行记账
     */
    override fun syncBill(billInfoModel: BillInfoModel) {
        // 根据账单信息构建URL Scheme参数
        val url = buildCreateApiUrl(billInfoModel)

        // 通过隐式Intent启动小星记账
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        Logger.i("目标应用uri：$url")
        SystemUtils.startActivityIfResolvable(intent, name) {
            AppAdapterManager.markSynced(billInfoModel)
        }
    }

    /**
     * 构建精确记账接口URL
     * xxjz://api/create?参数
     */
    private fun buildCreateApiUrl(billInfoModel: BillInfoModel): String {
        val params = mutableMapOf<String, String>()
        val type = mapBillTypeToXiaoXin(billInfoModel.type)
        // 必填参数：类型、金额、账户
        params["type"] = type
        params["amount"] = billInfoModel.money.toString()
        params["account"] = billInfoModel.accountNameFrom

        // 分类处理：支持一二级分类（默认单级在前）
        val (parent, child) = billInfoModel.categoryPair()

        when (type) {
            "支出" -> {
                if (parent.isNotEmpty()) params["parent"] = parent
                params["child"] = child.ifEmpty { "其他支出" }
            }

            "收入" -> {
                // 不传 parent，只传 child，保证收入分类正确
                params["child"] = when {
                    child.isNotEmpty() -> child
                    billInfoModel.cateName.isNotEmpty() -> billInfoModel.cateName
                    else -> "其他收入"
                }
            }

            "转账" -> {
                params["child"] = "账户互转"
                if (billInfoModel.accountNameTo.isNotEmpty()) {
                    params["account2"] = billInfoModel.accountNameTo
                }
            }

            "借贷" -> {
                params["child"] = if (child.isNotEmpty()) child else "借入"
                if (billInfoModel.accountNameTo.isNotEmpty()) {
                    params["account2"] = billInfoModel.accountNameTo
                }
            }
        }
        // 可选参数
        addOptionalParams(params, billInfoModel)

        return buildUrl("xxjz://api/create", params)
    }

    // dialog 接口已移除：根据需求，始终使用 create 接口

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
            BillType.Transfer -> "转账"

            // 借贷/还款
            BillType.IncomeLending,
            BillType.ExpendLending,
            BillType.IncomeRepayment,
            BillType.ExpendRepayment -> "借贷"

            // 普通收支
            BillType.Expend, BillType.ExpendReimbursement -> "支出"
            BillType.Income, BillType.IncomeReimbursement, BillType.IncomeRefund -> "收入"
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