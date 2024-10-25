/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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

package net.ankio.auto.xposed.core.utils

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Process
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.core.App.Companion.application
import net.ankio.auto.xposed.core.App.Companion.modulePath

object AppUtils {
    /**
     * 获取版本号
     * @return Int
     */
    fun getVersionCode(): Int {
        return runCatching {
            application!!.packageManager.getPackageInfo(
                application!!.packageName,
                0
            ).longVersionCode.toInt()
        }.getOrElse {
            0
        }
    }

    fun getVersionName(): String {
        return runCatching {
            application!!.packageManager.getPackageInfo(
                application!!.packageName,
                0
            ).versionName
        }.getOrElse {
            ""
        }
    }

    fun restart() {
        if (application == null)return
        val intent = application!!.packageManager.getLaunchIntentForPackage(application!!.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_NEW_TASK)
        application!!.startActivity(intent)
        Process.killProcess(Process.myPid())
    }


}