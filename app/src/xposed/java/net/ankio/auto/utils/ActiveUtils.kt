/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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

package net.ankio.auto.utils

import android.content.Context
import android.content.Intent
import net.ankio.auto.BuildConfig

object ActiveUtils {
    fun getActiveAndSupportFramework(): Boolean {
        return false
    }

    fun getFramework(): String {
        return "Unknown"
    }

    fun startApp(mContext: Context) {
        val intent: Intent? =
            mContext.packageManager.getLaunchIntentForPackage(BuildConfig.APPLICATION_ID)
        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mContext.startActivity(intent)
        }
    }
}
