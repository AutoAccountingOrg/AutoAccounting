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
import net.ankio.auto.constant.BookFeatures
import org.ezbook.server.db.model.BillInfoModel
import net.ankio.auto.App
import net.ankio.auto.utils.SystemUtils
import org.ezbook.server.constant.BillType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 一木记账适配接口 https://www.yimuapp.com/doc/import/scheme-url.html
class YiMuAdapter : IAppAdapter {
    override val pkg: String
        get() = "com.wangc.bill"
    override val link: String
        get() = "https://www.yimuapp.com/doc/index.html"
    override val icon: String
        get() = "https://pp.myapp.com/ma_icon/0/icon_54055885_1749720845/256"
    override val desc: String
        get() = "一木记账提供轻便、智能的记账服务。说话、打字即可快速录入、智能分类；按月查看分类统计，消费走向一目了然；支持Android、iOS、微信公众号等平台，数据实时互通，省心放心；一木记账，记录精彩生活！"
    override val name: String
        get() = "一木记账"

    override fun supportSyncAssets(): Boolean {
        // Xposed版本可能考虑支持
        return false
    }

    override fun features(): List<BookFeatures> {
        return listOf(
            BookFeatures.MULTI_BOOK,
            // BookFeatures.FEE,
            BookFeatures.TAG, //标签
            // BookFeatures.LEADING, //债务
            BookFeatures.ASSET_MANAGE,
            //  BookFeatures.MULTI_CURRENCY, //币种
            //   BookFeatures.REIMBURSEMENT ,//报销
        )
    }

    override fun syncAssets() {
        //不支持资产同步
    }

    override fun syncBill(billInfoModel: BillInfoModel) {
        // 构建一木记账 URI 并拉起应用
        // 参考格式：
        // yimu://api/addbill?parentCategory=父分类&childCategory=子分类&money=100&time=2023-02-23&remark=备注&asset=账户&bookName=日常账本&tags=标签1,标签2
        // 只支持支出
        if (billInfoModel.type !== BillType.Expend) return

        // 1) 处理分类：尝试从 cateName 拆分父/子分类；无法拆分时仅传子分类
        val (parentCategory, childCategory) = splitCategory(billInfoModel.cateName)

        // 2) 处理金额：为必填参数，按字符串传递
        val money = billInfoModel.money.toString()

        // 3) 处理时间：可空；若 time>0 则格式化为 yyyy-MM-dd HH:mm:ss
        val timeString = if (billInfoModel.time > 0) formatTime(billInfoModel.time) else ""

        // 4) 处理备注：可空
        val remark = billInfoModel.remark

        // 5) 处理账户：优先使用转出账户，其次转入账户
        val asset = when {
            billInfoModel.accountNameFrom.isNotEmpty() -> billInfoModel.accountNameFrom
            billInfoModel.accountNameTo.isNotEmpty() -> billInfoModel.accountNameTo
            else -> ""
        }

        // 6) 处理账本：可空
        val bookName = billInfoModel.bookName

        // 7) 处理标签：可空，多个以逗号分隔
        val tags = billInfoModel.tags.trim(',', ' ')

        // 使用 Uri.Builder 自动进行编码
        val uriBuilder = Uri.Builder()
            .scheme("yimu")
            .authority("api")
            .appendPath("addbill")
            .appendQueryParameter("money", money)

        if (!parentCategory.isNullOrEmpty()) uriBuilder.appendQueryParameter(
            "parentCategory",
            parentCategory
        )
        if (!childCategory.isNullOrEmpty()) uriBuilder.appendQueryParameter(
            "childCategory",
            childCategory
        )
        if (timeString.isNotEmpty()) uriBuilder.appendQueryParameter("time", timeString)
        if (remark.isNotEmpty()) uriBuilder.appendQueryParameter("remark", remark)
        if (asset.isNotEmpty()) uriBuilder.appendQueryParameter("asset", asset)
        if (bookName.isNotEmpty()) uriBuilder.appendQueryParameter("bookName", bookName)
        if (tags.isNotEmpty()) uriBuilder.appendQueryParameter("tags", tags)

        val uri = uriBuilder.build()

        // 通过隐式 Intent 打开 yimu:// 协议
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        SystemUtils.startActivity(intent)
    }

    /**
     * 将毫秒时间戳格式化为 yyyy-MM-dd HH:mm:ss
     */
    private fun formatTime(timeMillis: Long): String {
        val date = Date(timeMillis)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(date)
    }

    /**
     * 尝试按常见分隔符拆分父/子分类。
     * 支持分隔符："/"、">"、"-"；若无法拆分，则返回 ("", 原字符串)。
     */
    private fun splitCategory(cateName: String?): Pair<String?, String?> {
        val name = (cateName ?: "").trim()
        if (name.isEmpty()) return "" to ""
        val separators = listOf("/", ">", "-")
        for (sep in separators) {
            val parts = name.split(sep, limit = 2).map { it.trim() }
            if (parts.size == 2 && parts[1].isNotEmpty()) {
                return parts[0] to parts[1]
            }
        }
        return "" to name
    }

    override fun syncWaitBills() {

    }
}