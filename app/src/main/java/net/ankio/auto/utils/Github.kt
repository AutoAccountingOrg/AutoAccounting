/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
 * Licensed under  the Apache License, Version 3.0 (the "License");
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

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.exceptions.GithubException
import net.ankio.auto.request.RequestsUtils
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import org.ezbook.server.constant.Setting

object Github {
    private const val CLIENT_ID = "1fc6754f52bdfaae24ff"
    private const val CLIENT_SECRET = "4b4417f52358526dcfc3370dc2c3bc87d83cbc6a"
    private const val REDIRECT_URI = "autoaccounting://github/auth"

    fun getLoginUrl(): String {
        return "https://github.com/login/oauth/authorize?client_id=$CLIENT_ID&redirect_uri=$REDIRECT_URI&scope=public_repo"
    }

    suspend fun parseAuthCode(code: String?) =
        withContext(Dispatchers.IO) {
            if (code == null) {
                throw GithubException("授权代码为空")
            }
            val data = JsonObject().apply {
                addProperty("client_id", CLIENT_ID)
                addProperty("client_secret", CLIENT_SECRET)
                addProperty("code", code)
                addProperty("redirect_uri", REDIRECT_URI)
            }

            val requestsUtils = RequestsUtils(App.app)
            requestsUtils.addHeader("Accept", "application/vnd.github.v3+json")
            requestsUtils.addHeader("X-GitHub-Api-Version", "2022-11-28")
            val result =
                requestsUtils.json(
                    url = "https://github.com/login/oauth/access_token",
                    body = data
                )


            val json = Gson().fromJson(result.second, JsonObject::class.java)

            ConfigUtils.putString(Setting.GITHUB_ACCESS_TOKEN, json.get("access_token").asString)
        }


    suspend fun createIssue(
        title: String,
        body: String,
        repo: String = "AutoRule",
    ): Int =
        withContext(Dispatchers.IO) {
            val url = "https://api.github.com/repos/AutoAccountingOrg/$repo/issues"

            val jsonRequest = JsonObject()
            jsonRequest.addProperty("title", title)
            jsonRequest.addProperty("body", body)
            val requestsUtils = RequestsUtils(App.app)
            requestsUtils.addHeader("Accept", "application/vnd.github+json")
            requestsUtils.addHeader("X-GitHub-Api-Version", "2022-11-28")
            val accessToken = ConfigUtils.getString(Setting.GITHUB_ACCESS_TOKEN, "")
            if (accessToken.isEmpty()){
                throw GithubException("创建Issue失败, token = null")
            }
            requestsUtils.addHeader("Authorization", "Bearer $accessToken")
            val result = requestsUtils.json(url, jsonRequest)
            if (result.first != 201) {
                Logger.e("创建Issue失败: ${result.second}")
                throw GithubException("创建Issue失败")
            }
            val jsonObject = Gson().fromJson(result.second, JsonObject::class.java)
            val id = jsonObject.get("number").asInt
            id
        }
}
