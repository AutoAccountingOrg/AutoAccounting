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

package net.ankio.auto.xposed.hooks.wechat.hooks

import android.app.Application
import android.content.ContentValues
import com.google.gson.Gson
import com.google.gson.JsonObject
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import fr.arnaudguyon.xmltojsonlib.XmlToJson
import net.ankio.auto.xposed.core.App
import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.api.PartHooker
import org.ezbook.server.constant.DataType

class DatabaseHooker : PartHooker() {
    fun xmlToJson(xml: String): String {
        val xmlToJson: XmlToJson = XmlToJson.Builder(xml).build()
        return xmlToJson.toString()
    }

    override fun hook(
        hookerManifest: HookerManifest,
        application: Application?,
        classLoader: ClassLoader
    ) {
        val mAppClassLoader: ClassLoader = classLoader
        val mContext = application!!

        // 分析版本 8.0.43

        val database =
            XposedHelpers.findClass("com.tencent.wcdb.database.SQLiteDatabase", mAppClassLoader)
        XposedHelpers.findAndHookMethod(
            database,
            "insertWithOnConflict",
            String::class.java,
            String::class.java,
            ContentValues::class.java,
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    val contentValues = param.args[2] as ContentValues
                    val tableName = param.args[0] as String
                    val arg = if (param.args[1] != null) param.args[1] as String else ""
                    hookerManifest.logD("table:$tableName, contentValues:$contentValues")
                    //无效数据表
                    val usefulTable = listOf(
                        "message",
                        "AppMessage",
                    )

                    if (!usefulTable.contains(tableName)) return

                    val type = contentValues.getAsInteger("type") ?: return
                    // 补充数据
                    contentValues.put("tableName", tableName)
                    contentValues.put("arg", arg)
                    //由于微信的数据经常没有时间，所以我们为他添加默认时间
                    contentValues.put("t",System.currentTimeMillis())

                    if (tableName == "message") {
                        if (type == 1) {
                            // 这是聊天消息，content就是对话内容
                        } else if (type == 318767153) {
                            // 这是消息盒子
                            val content = contentValues.get("content").toString()
                            if (!content.contains("CDATA[微信支付")) return // 只对微信支付特殊处理
                            val json = Gson().fromJson(xmlToJson(content), JsonObject::class.java)
                            val tpl =
                                Gson().fromJson(
                                    """
                                    {
                                      "description": "",
                                            "source": "微信支付",
                                            "type": 5,
                                            "appId": "",
                                            "msgId": 99064,
                                            "title": ""
                                    }
                                    """.trimIndent(),
                                    JsonObject::class.java,
                                )

                            //   logD("微信支付数据JSON：$json")

                            val msg = json.get("msg").asJsonObject.get("appmsg").asJsonObject
                            tpl.addProperty("description", msg.get("des").asString)
                            tpl.addProperty("title", msg.get("title").asString)
                            tpl.addProperty(
                                "display_name",
                                msg.get("mmreader")
                                    .asJsonObject.get("template_header")
                                    .asJsonObject.get("display_name").asString,
                            )
                            tpl.addProperty("cachedPayTools", App.get("cachedPayTools"))
                            tpl.addProperty("cachedPayMoney", App.get("cachedPayMoney"))
                            tpl.addProperty("cachedPayShop", App.get("cachedPayShop"))
                            tpl.addProperty("t",System.currentTimeMillis())
                            val result = JsonObject()
                            result.add("mMap", tpl)

                            hookerManifest.logD("微信支付数据：$result")

                            hookerManifest.analysisData(DataType.DATA, result.toString())
                        }
                    } else if (tableName == "AppMessage") {
                        if (type == 5) {
                            if (contentValues.get("source").equals("微信支付")) {
                                // 微信支付
                                return
                            }
                            // 这个应该是公众号推送
                            hookerManifest.analysisData(DataType.DATA, Gson().toJson(contentValues))
                            return
                        } else if (type == 2000) {
                            // 补充用户数据
                            contentValues.put("hookUser", App.get("hookerUser"))
                            // 这个应该是微信转账给别人
                            val xml = contentValues.get("xml")

                            if (xml != null) {
                                contentValues.put("xml", xmlToJson(xml as String))
                                contentValues.put("cachedPayTools", App.get("cachedPayTools"))
                                contentValues.put("cachedPayMoney", App.get("cachedPayMoney"))
                                contentValues.put("cachedPayShop", App.get("cachedPayShop"))
                            }
                        }
                    }
                }
            },
        )
    }


}
