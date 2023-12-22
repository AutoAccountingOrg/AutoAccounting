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


import android.annotation.SuppressLint
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat



object  Github {
    private const val clientId = "d91ad91003a65611d5a5"
    private const val clientSecret = "ffaf0c5c1f6acb2c047b7689e7d14d4f4772899a"
    private const val redirectUri = "autoaccounting://github/auth"

    fun getLoginUrl():String{
        return   "https://github.com/login/oauth/authorize?client_id=$clientId&redirect_uri=$redirectUri&scope=public_repo"
;
    }
    fun parseAuthCode(code:String?,onSuccess: ()->Unit,onError: (String)->Unit){
        if(code==null) {
            onError("响应代码为空！")
            return
        }
        val requestBody = FormBody.Builder()
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("code", code)
            .add("redirect_uri", redirectUri)
            .build()

        val  httpUtils = HttpUtils()
        httpUtils.post("https://github.com/login/oauth/access_token",requestBody,null,{
            val params = it.split("&")
            for (param in params) {
                val keyValue = param.split("=")
                if (keyValue.size == 2 && keyValue[0] == "access_token") {
                    SpUtils.putString("accessToken",keyValue[1])
                    onSuccess()
                }
            }
        },{ _, error ->
            onError(error)
        })

    }

    fun getHeaders(): Headers {
        val accessToken = SpUtils.getString("accessToken","")
        val builder =   Headers.Builder()
            .add("Accept","application/vnd.github+json")
            .add("X-GitHub-Api-Version","2022-11-28")
        if(accessToken.isNotEmpty()){
            builder.add("Authorization", "Bearer ${SpUtils.getString("accessToken")}")
        }
        return  builder.build();
    }
    fun createIssue(title: String, body: String,onSuccess: (String)->Unit,onError: (String)->Unit) {
        val url = "https://api.github.com/repos/Auto-Accounting/AutoRule/issues"

        val jsonRequest = JSONObject()
        try {
            jsonRequest.put("title", title)
            jsonRequest.put("body", body)
        } catch (e: JSONException) {
            e.printStackTrace()
            onError(e.message.toString())
            return
        }

        val bodyInfo = jsonRequest.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val  httpUtils = HttpUtils()
        httpUtils.post(url,bodyInfo, getHeaders(),{
            try {
                val jsonObject = JSONObject(it)
                val id = jsonObject.getInt("number")
                onSuccess(id.toString())
            } catch (e: JSONException) {
                e.printStackTrace()
                onError(e.message.toString())
            }
        },{url, errorInfo ->
            onError(errorInfo)
        })
    }

    fun getIssueStatus( issueNumber: Int) {
        val url = "https://api.github.com/repos/Auto-Accounting/AutoRule/issues/$issueNumber"
        val  httpUtils = HttpUtils()
        httpUtils.get(url,getHeaders(),{

        },{url, errorInfo ->  })
    }

    fun checkVersionUpdate(onUpdate: (updateInfo: UpdateInfo) -> Unit) {
        val url = "https://api.github.com/repos/Auto-Accounting/AutoRule/issues"

    }
    @SuppressLint("SimpleDateFormat")
    private fun checkUpdate(url:String, name: String, onUpdate: (updateInfo: UpdateInfo) -> Unit){
        val  httpUtils = HttpUtils()
        val ruleVersion =  SpUtils.getInt("${name}Version",0)
        httpUtils.get(url, getHeaders(),{
            Log.e("更新信息",it)
            try {
                val jsonArray = Gson().fromJson(it,JsonArray::class.java)
                if(jsonArray.isEmpty){
                    return@get
                }
                val json = jsonArray[0].asJsonObject
                val version = json.get("tag_name").asString.replace(".","").replace("v","").toInt()
                if(version<=ruleVersion){
                    return@get
                }
                // SpUtils.putInt("${name}Version",version)
                val updateInfo = UpdateInfo()
                val  sdf  =  SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                val  date  =  sdf.parse(json.get("created_at").asString)
                val dateFormat  =  SimpleDateFormat("yyyy-MM-dd  HH:mm:ss")
                updateInfo.date  =
                    date?.let { it2 -> dateFormat.format(it2).toString() } ?:""
                updateInfo.name = json.get("tag_name").asString
                updateInfo.log = json.get("body").asString?:"无日志"
                updateInfo.version = version
                val arrayList = ArrayList<String>()
                val assets = json.get("assets").asJsonArray
                //SpUtils.putString("${name}VersionName",updateInfo.name)
                for (asset in assets){
                    arrayList.add(asset.asJsonObject.get("browser_download_url").asString)
                }
                updateInfo.downloadUrl = arrayList
               onUpdate(updateInfo)
            } catch (e: JSONException) {
                e.printStackTrace()
                Log.e("Github",e.message?:"")
            }
        },{url, errorInfo ->
            Log.e("Github",errorInfo)
        })
    }
    fun checkRuleUpdate(onUpdate:(updateInfo:UpdateInfo)->Unit) {
        val url = "https://api.github.com/repos/Auto-Accounting/AutoRule/releases"
        checkUpdate(url,"rule") {
            SpUtils.putInt("ruleVersion",it.version)
            SpUtils.putString("ruleVersionName",it.name)
            for (downloadUrl in it.downloadUrl) {
                val httpUtils = HttpUtils()
                //使用镜像站下载
                httpUtils.get("https://mirror.ghproxy.com/$downloadUrl", getHeaders(), { response ->
                    Log.e(downloadUrl, response)
                    if (downloadUrl.contains("rule.js")) {
                        ActiveUtils.put("dataRule", response)
                    } else {
                        ActiveUtils.put("dataCategory", response)
                    }

                }, { _, errorInfo ->
                    Log.e("Github", errorInfo)
                    //若是下载失败，不更新
                    SpUtils.putInt("ruleVersion",0)
                    SpUtils.putString("ruleVersionName","Update Error")
                })
            }
            it.type = "Rule"
            onUpdate(it)
        }

    }
}