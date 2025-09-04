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
import net.ankio.auto.storage.Logger
import org.ezbook.server.db.model.BillInfoModel
import net.ankio.auto.utils.SystemUtils
import net.ankio.auto.utils.appendIfNotBlank
import org.ezbook.server.constant.BillAction
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
        // 仅支持：支出/收入；其他类型直接返回
        if (billInfoModel.type != BillType.Expend && billInfoModel.type != BillType.Income) return

        // 分类（父/子）：收入场景将父类固定为"收入"，子类使用原父类；支出保持原父/子
        val (rawParent, rawChild) = billInfoModel.categoryPair()
        val (parentCategory, childCategory) = if (billInfoModel.type == BillType.Income) {
            "收入" to rawParent
        } else {
            rawParent to rawChild
        }

        // 必填：金额
        val money = billInfoModel.money.toString()

        // 可选：时间、备注、账户、账本、标签
        val timeString = billInfoModel.time.takeIf { it > 0 }?.let(::formatTime).orEmpty()
        val asset = sequenceOf(billInfoModel.accountNameFrom, billInfoModel.accountNameTo)
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()
        val bookName = billInfoModel.bookName
        val tags = billInfoModel.tags.trim(',', ' ')

        // 构建 yimu://api/addbill?... 协议
        val uri = Uri.Builder()
            .scheme("yimu")
            .authority("api")
            .appendPath("addbill")
            .appendQueryParameter("money", money)
            .apply {
                appendIfNotBlank("parentCategory", parentCategory)
                appendIfNotBlank("childCategory", childCategory)
                appendIfNotBlank("time", timeString)
                appendIfNotBlank("remark", billInfoModel.remark)
                appendIfNotBlank("asset", asset)
                appendIfNotBlank("bookName", bookName)
                appendIfNotBlank("tags", tags)
            }
            .build()
        Logger.i("构建：$uri")
        // 调起目标 App 处理并在成功后标记同步完成
        val intent = Intent(Intent.ACTION_VIEW, uri).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        SystemUtils.startActivityIfResolvable(intent, name) {
            AppAdapterManager.markSynced(billInfoModel)
        }
    }

    /**
     * 将毫秒时间戳格式化为 yyyy-MM-dd HH:mm:ss
     */
    private fun formatTime(timeMillis: Long): String {
        val date = Date(timeMillis)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(date)
    }

    // 拆分类扩展函数定义在 BillTool.kt



    override fun syncWaitBills(billAction: BillAction) {

    }

    override fun sleep(): Long {
        return 0L
    }
}