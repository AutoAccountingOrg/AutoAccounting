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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.crossbowffs.remotepreferences.RemotePreferences
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.database.table.AssetsMap


object ActiveUtils {
    fun getActiveAndSupportFramework(context: Context): Boolean {
        return false
    }

    fun startApp(mContext: Context) {
        val intent: Intent? =
            mContext.packageManager.getLaunchIntentForPackage(BuildConfig.APPLICATION_ID)
        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mContext.startActivity(intent)
        }
    }


    var APPLICATION_ID = BuildConfig.APPLICATION_ID

    private  var TOKEN = ""

    fun getToken(context: Context): String {
        if(TOKEN.isNotEmpty())return TOKEN
        val auth = "net.ankio.preferences"
        val file = "main_prefs"
        if (APPLICATION_ID == BuildConfig.APPLICATION_ID) {
            val token = AutoAccountingServiceUtils.get("token")
            TOKEN = token
            val prefs = context.getSharedPreferences(file, Context.MODE_PRIVATE)
            prefs.edit().putString("token", token).apply()
            return token
        } else {

            val prefs: SharedPreferences = RemotePreferences(context, auth, file)
            TOKEN = prefs.getString("token", "") ?: ""
            return TOKEN
        }
    }
}