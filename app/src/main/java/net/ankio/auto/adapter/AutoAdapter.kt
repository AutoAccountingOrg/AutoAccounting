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

import net.ankio.auto.BuildConfig
import net.ankio.auto.constant.BookFeatures
import org.ezbook.server.db.model.BillInfoModel

class AutoAdapter : IAppAdapter {
    override val pkg: String
        get() = BuildConfig.APPLICATION_ID
    override val link: String
        get() = ""
    override val icon: String
        get() = ""
    override val desc: String
        get() = "仅将账单记录到自动记账，不同步到任何记账软件。"
    override val name: String
        get() = "仅自动记账"

    override fun features(): List<BookFeatures> {
        return BookFeatures.entries
    }

    override fun syncAssets() {

    }

    override fun syncBill(billInfoModel: BillInfoModel) {

    }

    override fun supportSyncAssets(): Boolean {
        return false
    }

    override fun syncWaitBills() {

    }
}