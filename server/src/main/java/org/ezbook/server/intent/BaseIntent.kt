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

import android.content.ComponentName
import android.content.Intent
import org.ezbook.server.Server

open class BaseIntent(
    val type: IntentType,
    val t: Long = System.currentTimeMillis()
) {


    open fun toIntent(): Intent {
        val intent = Intent()
        intent.putExtra("intentType", type.name)
        intent.setComponent(
            ComponentName(
                Server.packageName,
                "net.ankio.auto.ui.activity.FloatingWindowTriggerActivity"
            )
        )
        // 使用单次设置的方式合并所有标志位：
        // - NEW_TASK: 允许从服务端拉起 Activity
        // - NO_HISTORY/EXCLUDE_FROM_RECENTS: 不保留历史、从最近任务中排除
        // - NO_ANIMATION: 禁用切换动画
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
        )
        return intent
    }

}