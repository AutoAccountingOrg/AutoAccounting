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

package net.ankio.auto.update

import android.app.Activity
import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import net.ankio.auto.BuildConfig
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.utils.CustomTabsHelper
import org.ezbook.server.constant.Setting

class AppUpdate(context: Context) : BaseUpdate(context) {
    override val repo: String
        get() = "AutoAccounting"


    override val dir: String
        get() = "版本更新/" + ConfigUtils.getString(
            Setting.CHECK_UPDATE_TYPE,
            UpdateType.Stable.name
        )

    override fun ruleVersion(): String {
        return BuildConfig.VERSION_NAME
    }

    override fun onCheckedUpdate() {
        download = if (ConfigUtils.getString(
                Setting.UPDATE_CHANNEL,
                UpdateChannel.Github.name
            ) == UpdateChannel.Github.name
        ) {
            /* "https://cors.isteed.cc/github.com/AutoAccountingOrg/$repo/releases/download/$version/$version.apk"*/
            pan() + "/$version.apk"
        } else {
            pan() + "/$version.apk"
        }
    }

    override suspend fun checkVersionFromGithub(localVersion: String): Array<String> {
       val json = request.get("https://api.github.com/repos/AutoAccountingOrg/$repo/releases")
        val data =  Gson().fromJson(json.second, Array<JsonObject>::class.java)
        val channel = ConfigUtils.getString(
            Setting.CHECK_UPDATE_TYPE,
            UpdateType.Stable.name
        )
        for (i in data) {
            val tag = i["tag_name"].asString
            if (tag.contains(channel)){
                github = i["url"].asString
                return super.checkVersionFromGithub(localVersion)
            }
        }
        return arrayOf("", "", "")
    }

    override fun update(activity: Activity) {
        if (version.isEmpty()) return

        CustomTabsHelper.launchUrlOrCopy(activity, download)
    }
}