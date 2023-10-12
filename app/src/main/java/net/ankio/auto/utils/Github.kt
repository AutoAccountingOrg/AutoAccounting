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


import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException


interface CallbackListener {
    fun onSuccess(response: String)
    fun onFailure(e: IOException)
}
object  Github {
    private val clientId = "d91ad91003a65611d5a5"
    private val clientSecret = "ffaf0c5c1f6acb2c047b7689e7d14d4f4772899a"
    private val redirectUri = "autoaccounting://github/auth"

    fun getLoginUrl():String{
        return   "https://github.com/login/oauth/authorize?client_id=$clientId&redirect_uri=$redirectUri&scope=public_repo"
;
    }
    fun parseAuthCode(code:String?,listener: CallbackListener){
        if(code==null) {
            listener.onSuccess("响应代码为空！")
            return
        }
        val requestBody = FormBody.Builder()
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("code", code)
            .add("redirect_uri", redirectUri)
            .build()

        val  httpUtils = HttpUtils()
        httpUtils.post("https://github.com/login/oauth/access_token",requestBody,null,object : HttpUtils.CallbackListener {
            override fun onSuccess(response: String) {
                // 处理成功响应
                val params = response.split("&")
                for (param in params) {
                    val keyValue = param.split("=")
                    if (keyValue.size == 2 && keyValue[0] == "access_token") {
                        SpUtils.putString("accessToken",keyValue[1])
                        listener.onSuccess("授权成功！")
                    }
                }
            }

            override fun onFailure(e: IOException) {
                // 处理请求失败
                e.printStackTrace()
                listener.onSuccess("请求授权失败！")

            }
        })

    }

    fun getHeaders(): Headers {
        return   Headers.Builder()
            .add("Authorization", "Bearer ${SpUtils.getString("accessToken")}")
            .add("Accept","application/vnd.github+json")
            .add("X-GitHub-Api-Version","2022-11-28")
            .build()
    }
    fun createIssue(title: String, body: String,listener: CallbackListener) {
        val url = "https://api.github.com/repos/Auto-Accounting/AutoRule/issues"

        val jsonRequest = JSONObject()
        try {
            jsonRequest.put("title", title)
            jsonRequest.put("body", body)
        } catch (e: JSONException) {
            e.printStackTrace()
            listener.onFailure(IOException(e.message))
            return
        }



        val body = jsonRequest.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())


        val  httpUtils = HttpUtils()
        httpUtils.post(url,body, getHeaders(),object : HttpUtils.CallbackListener {
            override fun onSuccess(response: String) {

                try {
                    val jsonObject = JSONObject(response)
                    val id = jsonObject.getInt("number")
                    listener.onSuccess(id.toString())
                } catch (e: JSONException) {
                    e.printStackTrace()
                    listener.onFailure(IOException(e.message))
                }
                // 处理成功响应

            }

            override fun onFailure(e: IOException) {
                e.printStackTrace()
                // 处理请求失败
                listener.onFailure(e)
            }
        })
    }

    fun getIssueStatus( issueNumber: Int) {
        val url = "https://api.github.com/repos/Auto-Accounting/AutoRule/issues/$issueNumber"


        val  httpUtils = HttpUtils()
        httpUtils.get(url,getHeaders(),object : HttpUtils.CallbackListener {
            override fun onSuccess(response: String) {
                // 处理成功响应
            }

            override fun onFailure(e: IOException) {
                // 处理请求失败
            }
        })
    }
}