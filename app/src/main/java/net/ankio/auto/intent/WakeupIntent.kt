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
import org.ezbook.server.Server

data class WakeupIntent(
    val t: Long = System.currentTimeMillis(),
    val type: IntentType = IntentType.WakeupIntent
) {
    companion object {
        fun parse(intent: Intent): WakeupIntent {
            val t = intent.getLongExtra("t", 0)
            return WakeupIntent(t)
        }
    }

    fun toIntent(): Intent {
        val intent = Intent()
        intent.putExtra("t", t)
        intent.putExtra("type", type)
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