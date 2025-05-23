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

import net.ankio.auto.constant.BookFeatures
import org.ezbook.server.db.model.BillInfoModel

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
        //TODO 将账单同步到目标应用
    }
}