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
import net.ankio.auto.autoApp
import net.ankio.auto.constant.BookFeatures
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BillInfoModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri

class QianJiAdapter : IAppAdapter {
    override val pkg: String
        get() = "com.mutangtech.qianji"
    override val link: String
        get() = "https://qianjiapp.com/"
    override val icon: String
        get() = "https://pp.myapp.com/ma_icon/0/icon_52573842_1744768940/256"
    override val desc: String
        get() = """
钱迹，一款简洁纯粹的记账 App，是一个 “无广告、无开屏、无理财” 的 “三无” 产品。
力求极简，专注个人记账，将每一笔收支都清晰记录，消费及资产随时了然于心。
        """.trimIndent()
    override val name: String
        get() = "钱迹"

    override fun features(): List<BookFeatures> {
        return if (AppAdapterManager.xposedMode()) {
            listOf(
                BookFeatures.MULTI_BOOK,
                BookFeatures.FEE,
                //  BookFeatures.TAG,
                BookFeatures.LEADING,
                BookFeatures.ASSET_MANAGE,
                BookFeatures.MULTI_CURRENCY,
                BookFeatures.REIMBURSEMENT
            )
        } else {
            listOf(
                BookFeatures.MULTI_BOOK,
                BookFeatures.FEE,
                //   BookFeatures.TAG,
                //   BookFeatures.LEADING,
                BookFeatures.ASSET_MANAGE,
                //  BookFeatures.MULTI_CURRENCY,
                BookFeatures.REIMBURSEMENT
            )
        }
    }

    override fun syncAssets() {
        if (AppAdapterManager.ocrMode()) {
            return
        }
        //TODO 钱迹同步逻辑
    }


    override fun syncBill(billInfoModel: BillInfoModel) {
        // 通过钱迹提供的 Uri-Scheme 完成自动化记账。
        // 文档：qianji://publicapi/addbill?type=0&money=26.5&time=2020-01-31 12:30:00&remark=备注&catename=分类&accountname=账户&accountname2=转入账户&bookname=账本&fee=手续费&showresult=0

        // 1) 映射账单类型到钱迹类型编码
        val qjType = toQianJiType(billInfoModel.type)

        // 2) 必填参数：type、money
        val uriBuilder = StringBuilder("qianji://publicapi/addbill")
            .append("?type=").append(qjType)
            .append("&money=").append(billInfoModel.money)

        // 3) 可选参数：时间（yyyy-MM-dd HH:mm:ss）
        if (billInfoModel.time > 0) {
            uriBuilder.append("&time=").append(formatTime(billInfoModel.time))
        }

        // 4) 备注
        if (billInfoModel.remark.isNotEmpty()) {
            uriBuilder.append("&remark=")
                .append(Uri.encode(billInfoModel.remark))
        }

        // 5) 分类：支持一二级分类，内部以 "-" 拆分转换为 "/::/"
        if (qjType == 0 || qjType == 1) { // 仅收入/支出需要分类
            val cate = buildQianJiCategory(billInfoModel.cateName)
            if (cate.isNotEmpty()) {
                uriBuilder.append("&catename=")
                    .append(Uri.encode(cate))
            }
        }

        // 6) 账本（可选）
        if (billInfoModel.bookName.isNotEmpty()) {
            uriBuilder.append("&bookname=")
                .append(Uri.encode(billInfoModel.bookName))
        }

        // 7) 账户信息与手续费（转账/信用卡还款需要账户；收支可选）
        if (qjType == 2 || qjType == 3) {
            if (billInfoModel.accountNameFrom.isNotEmpty()) {
                uriBuilder.append("&accountname=")
                    .append(Uri.encode(billInfoModel.accountNameFrom))
            }
            if (billInfoModel.accountNameTo.isNotEmpty()) {
                uriBuilder.append("&accountname2=")
                    .append(Uri.encode(billInfoModel.accountNameTo))
            }
            if (billInfoModel.fee > 0) {
                uriBuilder.append("&fee=").append(billInfoModel.fee)
            }
        } else {
            // 收入/支出可选带出账户
            if (billInfoModel.accountNameFrom.isNotEmpty()) {
                uriBuilder.append("&accountname=")
                    .append(Uri.encode(billInfoModel.accountNameFrom))
            }
        }

        // 8) 统一不弹出成功提示
        uriBuilder.append("&showresult=0")

        // TODO 添加更多同步数据

        // 9) 发起隐式 Intent 调起钱迹
        val intent = Intent(Intent.ACTION_VIEW, uriBuilder.toString().toUri()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        autoApp.startActivity(intent)
    }
    override fun supportSyncAssets(): Boolean {
        if (AppAdapterManager.xposedMode()) return true
        //TODO 初始化钱迹默认资产，只有没有账本和分类的时候才初始化


        return false
    }

    /**
     * 将内部账单类型映射到钱迹类型编码。
     * 钱迹当前支持：0 支出、1 收入、2 转账、3 信用卡还款、5 报销。
     * 对于未明确支持的类型，尽量回退到支出或收入大类以保证可用性。
     */
    private fun toQianJiType(type: BillType): Int {
        return when (type) {
            BillType.Expend -> 0
            BillType.Income -> 1
            BillType.Transfer -> 2
            BillType.ExpendReimbursement, BillType.IncomeReimbursement -> 5
            // 其他类型按支出/收入大类回退
            BillType.ExpendLending, BillType.ExpendRepayment -> 0
            BillType.IncomeLending, BillType.IncomeRepayment, BillType.IncomeRefund -> 1
        }
    }

    /**
     * 构造钱迹可识别的分类字符串。
     * - 若内部以 "-" 表示一二级分类（如 "三餐-午餐"），则转换为 "三餐/::/午餐"。
     * - 否则原样返回。
     */
    private fun buildQianJiCategory(cateName: String): String {
        val name = cateName.trim()
        if (name.isEmpty()) return ""
        return if (name.contains("-")) {
            val parts = name.split("-", limit = 2).map { it.trim() }
            if (parts.size == 2 && parts[1].isNotEmpty()) "${parts[0]}/::/${parts[1]}" else name
        } else name
    }

    /**
     * 将毫秒时间戳格式化为钱迹要求的时间格式：yyyy-MM-dd HH:mm:ss。
     */
    private fun formatTime(timeMillis: Long): String {
        val date = Date(timeMillis)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(date)
    }
}