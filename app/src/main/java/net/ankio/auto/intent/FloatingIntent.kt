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

package net.ankio.auto.intent

import android.content.ComponentName
import android.content.Intent
import com.google.gson.Gson
import org.ezbook.server.Server
import org.ezbook.server.db.model.BillInfoModel

data class FloatingIntent(
    val billInfoModel: BillInfoModel,
    val showTip: Boolean,
    val from: String,
    val parent: BillInfoModel? = null,
    val t: Long = System.currentTimeMillis(),
    val type: IntentType = IntentType.FloatingIntent
) {

    companion object {
        fun parse(intent: Intent): FloatingIntent {
            val billInfo = runCatching {
                Gson().fromJson(
                    intent.getStringExtra("billInfo"),
                    BillInfoModel::class.java
                )
            }.getOrDefault(BillInfoModel())

            val showTip = intent.getBooleanExtra("showWaitTip", false)
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
            val t = intent.getLongExtra("t", 0)
            return FloatingIntent(billInfo, showTip, from, parent, t)
        }
    }


    fun toIntent(): Intent {
        val intent = Intent()
        intent.putExtra("billInfo", Gson().toJson(billInfoModel))
        intent.putExtra("id", billInfoModel.id)
        intent.putExtra("showWaitTip", showTip)
        intent.putExtra("t", t)
        intent.putExtra("type", type)
        if (parent != null) {
            intent.putExtra("parent", Gson().toJson(parent))
        }
        intent.putExtra("from", from)
        intent.setComponent(
            ComponentName(
                Server.packageName,
                "net.ankio.auto.ui.activity.FloatingWindowTriggerActivity"
            )
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        return intent
    }
}