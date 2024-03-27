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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.exceptions.GithubException
import net.ankio.auto.utils.request.RequestsUtils
import org.json.JSONObject


object Github {
    private const val clientId = "1fc6754f52bdfaae24ff"
    private const val clientSecret = "4b4417f52358526dcfc3370dc2c3bc87d83cbc6a"
    private const val redirectUri = "autoaccounting://github/auth"
    private val requestsUtils = RequestsUtils(AppUtils.getApplication())

    fun getLoginUrl(): String {
        return "https://github.com/login/oauth/authorize?client_id=$clientId&redirect_uri=$redirectUri&scope=public_repo"
    }

   suspend fun parseAuthCode(code: String?) = withContext(Dispatchers.IO) {
        if (code == null) {
            throw GithubException("授权代码为空")
        }
        val data = hashMapOf(
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "code" to code,
            "redirect_uri" to redirectUri
        )

       val result =  requestsUtils.post(
            url = "https://github.com/login/oauth/access_token",
            data = hashMapOf("json" to Gson().toJson(data)),
            contentType = RequestsUtils.TYPE_JSON
        )


        val params = String(result.byteArray).split("&")
        for (param in params) {
            val keyValue = param.split("=")
            if (keyValue.size == 2 && keyValue[0] == "access_token") {
                SpUtils.putString("accessToken", keyValue[1])
            }
        }

    }

    private fun getHeaders(): HashMap<String, String> {


        val hashMap = hashMapOf(
            "Accept" to "application/vnd.github+json",
            "X-GitHub-Api-Version" to "2022-11-28"
        )
        val accessToken = SpUtils.getString("accessToken", "")
        if (accessToken.isNotEmpty()) {
            hashMap["Authorization"] = "Bearer ${SpUtils.getString("accessToken")}"
        }
        return hashMap
    }

    suspend fun createIssue(
        title: String,
        body: String,
    ):String = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/AutoAccountingOrg/AutoRule/issues"

        val jsonRequest = JSONObject()
        jsonRequest.put("title", title)
        jsonRequest.put("body", body)

        val result = requestsUtils.post(
            url = url,
            data = hashMapOf("json" to jsonRequest.toString()),
            headers = getHeaders(),
            contentType = RequestsUtils.TYPE_JSON
        )
        if (result.code != 201) {
            throw GithubException("创建Issue失败")
        }
        val jsonObject = JSONObject(String(result.byteArray))
        val id = jsonObject.getInt("number")
        id.toString()
    }

}