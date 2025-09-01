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
import net.ankio.auto.App
import net.ankio.auto.constant.BookFeatures
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.constant.BillType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri
import net.ankio.auto.autoApp

//TODO 适配页面
class YiYuAdapter : IAppAdapter {
    override val pkg: String
        get() = "kylec.me.lightbookkeeping"
    override val link: String
        get() = "https://doc.yiyujizhang.cn/"
    override val icon: String
        get() = "https://pp.myapp.com/ma_icon/0/icon_54078946_1748268164/256"
    override val desc: String
        get() = "简洁、清爽、秒开、无广告的记账APP\n" +
                "记账；不动声色"
    override val name: String
        get() = "一羽记账"

    override fun supportSyncAssets(): Boolean {
        //不支持同步资产
        return false
    }

    override fun features(): List<BookFeatures> {
        return listOf(
            BookFeatures.MULTI_BOOK,
            BookFeatures.FEE,
            BookFeatures.ASSET_MANAGE,
            BookFeatures.TAG,
        )
    }

    override fun syncAssets() {
        //  不支持资产同步
    }

    override fun syncBill(billInfoModel: BillInfoModel) {
        // 使用一羽记账的 URL Scheme 进行账单同步
        // 规范示例：yyjz://addbill?type=0&money=12&category=早餐&remark=豆浆油条
        // 主要参数映射：
        // - type: 0支出/1收入/2转账/3报销
        // - book: 账本名（可选）
        // - category: 分类名（收支必填、转账不填）
        // - money: 金额（必填）
        // - discount: 优惠金额（仅支出，暂不支持，留空）
        // - asset1/asset2: 账户（转账必填；收支可选）
        // - fee: 转账手续费（转账可选）
        // - remark: 备注（可选）
        // - datetime: yyyy-MM-dd[ HH:mm[:ss]]（可选，这里统一传 yyyy-MM-dd HH:mm:ss）
        // - tags: 标签（多个用英文逗号分隔，可选）

        // 1) 基础与必填参数
        val type = toYiYuType(billInfoModel.type)
        val money = billInfoModel.money.toString()

        // 2) 可选参数
        val category = billInfoModel.cateName.trim()
        val book = billInfoModel.bookName.trim()
        val remark = billInfoModel.remark.trim()
        val tags = billInfoModel.tags.trim(',', ' ').trim()

        val isTransfer = type == 2
        val asset1 = billInfoModel.accountNameFrom.trim()
        val asset2 = billInfoModel.accountNameTo.trim()
        val fee = billInfoModel.fee

        val datetime = if (billInfoModel.time > 0) formatTime(billInfoModel.time) else ""

        // 3) 组装 URL（手动拼接以保持与文档示例一致的 yyjz://addbill 形式）
        val url = StringBuilder("yyjz://addbill")
        val params = mutableListOf<String>()

        fun addParam(key: String, value: String) {
            if (value.isNotEmpty()) params.add("$key=${Uri.encode(value)}")
        }

        // 必填
        addParam("type", type.toString())
        addParam("money", money)

        // 收支传分类；转账不传分类
        if (!isTransfer) addParam("category", category)

        // 账本、备注、标签
        addParam("book", book)
        addParam("remark", remark)
        addParam("tags", tags)

        // 时间
        addParam("datetime", datetime)

        // 账户与手续费
        if (isTransfer) {
            addParam("asset1", asset1)
            addParam("asset2", asset2)
            if (fee > 0) addParam("fee", fee.toString())
        } else {
            // 收/支可选账户（若存在则带上）
            addParam("asset1", asset1)
        }

        if (params.isNotEmpty()) {
            url.append('?').append(params.joinToString("&"))
        }

        val uri = url.toString().toUri()

        // 4) 通过隐式 Intent 拉起一羽记账
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        autoApp.startActivity(intent)

        AppAdapterManager.markSynced(billInfoModel)
    }

    /**
     * 将内部账单类型映射为一羽记账的类型编码。
     * - 0: 支出
     * - 1: 收入
     * - 2: 转账
     * - 3: 报销（尽量覆盖 ExpendReimbursement / IncomeReimbursement）
     */
    private fun toYiYuType(type: BillType): Int {
        return when (type) {
            BillType.Expend -> 0
            BillType.Income -> 1
            BillType.Transfer -> 2
            BillType.ExpendReimbursement, BillType.IncomeReimbursement -> 3
            // 其他类型按收支大类回退
            BillType.ExpendLending, BillType.ExpendRepayment -> 0
            BillType.IncomeLending, BillType.IncomeRepayment, BillType.IncomeRefund -> 1
        }
    }

    /**
     * 将毫秒时间戳格式化为一羽可识别的时间字符串（yyyy-MM-dd HH:mm:ss）。
     */
    private fun formatTime(timeMillis: Long): String {
        val date = Date(timeMillis)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(date)
    }

    override fun syncWaitBills() {

    }
}