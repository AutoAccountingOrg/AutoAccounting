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

package org.ezbook.server.intent

import android.content.Intent
import com.google.gson.Gson
import org.ezbook.server.db.model.BillInfoModel

data class BillInfoIntent(
    val billInfoModel: BillInfoModel,
    val from: String,
    val parent: BillInfoModel? = null,
) : BaseIntent(IntentType.FloatingIntent) {

    companion object {
        /**
         * 从 Intent 解析 BillInfoIntent
         * @param intent 可能为空的 Intent 对象
         * @return 解析后的 BillInfoIntent，如果 intent 为空则返回默认值
         */
        fun parse(intent: Intent?): BillInfoIntent? {
            // 如果 intent 为空，返回默认的 BillInfoIntent
            if (intent == null) {
                return null
            }
            
            val billInfo = runCatching {
                Gson().fromJson(
                    intent.getStringExtra("billInfo"),
                    BillInfoModel::class.java
                )
            }.getOrNull()

            if (billInfo == null) return null

            val from = intent.getStringExtra("from") ?: ""
            val parent = if (intent.hasExtra("parent")) {
                runCatching {
                    Gson().fromJson(
                        intent.getStringExtra("parent"),
                        BillInfoModel::class.java
                    )
                }.getOrNull()
            } else {
                null
            }

            return BillInfoIntent(billInfo, from, parent)
        }
    }


    override fun toIntent(): Intent {
        val intent = super.toIntent()
        intent.putExtra("billInfo", Gson().toJson(billInfoModel))
        intent.putExtra("id", billInfoModel.id)
        if (parent != null) {
            intent.putExtra("parent", Gson().toJson(parent))
        }
        return intent
    }

    /**
     * 便于日志调试的人类可读表示，仅包含关键信息，避免过长输出
     */
    override fun toString(): String {
        val parentId = parent?.id ?: -1L
        return "BillInfoIntent(type=$type, t=$t, from='${from}', billId=${billInfoModel.id}, billType=${billInfoModel.type}, money=${billInfoModel.money}, currency='${billInfoModel.currencyCode()}', app='${billInfoModel.app}', parentId=${parentId})"
    }
}