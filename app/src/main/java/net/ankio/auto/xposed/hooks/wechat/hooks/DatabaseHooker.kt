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

import io.github.oshai.kotlinlogging.KotlinLogging
import android.content.ContentValues
import com.google.gson.Gson
import com.google.gson.JsonObject
import fr.arnaudguyon.xmltojsonlib.XmlToJson
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.DataUtils
import org.ezbook.server.constant.DataType

class DatabaseHooker : PartHooker() {
    private val logger = KotlinLogging.logger(this::class.java.name)

    fun xmlToJson(xml: String): String {
        val xmlToJson: XmlToJson = XmlToJson.Builder(xml).build()
        return xmlToJson.toString()
    }

    override fun hook() {
       

        // 分析版本 8.0.43

        val database = Hooker.loader("com.tencent.wcdb.database.SQLiteDatabase")
        Hooker.after(
            database,
            "insertWithOnConflict",
            String::class.java,
            String::class.java,
            ContentValues::class.java,
            Int::class.javaPrimitiveType!!,
        ) { param ->
            val contentValues = param.args[2] as ContentValues
            val tableName = param.args[0] as String
            val arg = if (param.args[1] != null) param.args[1] as String else ""
            logger.debug { "table:$tableName, contentValues:$contentValues" }
            //无效数据表
            val usefulTable = listOf(
                "message",
                "AppMessage",
            )

            if (!usefulTable.contains(tableName)) return@after

            val type = contentValues.getAsInteger("type") ?: return@after
            // 补充数据
            contentValues.put("tableName", tableName)
            contentValues.put("arg", arg)
            //由于微信的数据经常没有时间，所以我们为他添加默认时间
            contentValues.put("t", System.currentTimeMillis())
            if (tableName == "message") {

                when (type) {
                    318767153 -> {
                        // 这是消息盒子
                        val content = contentValues.get("content").toString()
                    //    if (!content.contains("CDATA[微信支付")) return@after // 只对微信支付特殊处理
                        val json = Gson().fromJson(xmlToJson(content), JsonObject::class.java)
                        //TODO 有一些消息从这里漏出来了
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


                        val msg = json.get("msg").asJsonObject.get("appmsg").asJsonObject
                        tpl.addProperty("description", msg.get("des").asString)
                        tpl.addProperty("title", msg.get("title").asString)

                        runCatching {
                            val header = msg.get("mmreader").asJsonObject.get("template_header")
                            if (header.isJsonObject) {
                                tpl.addProperty(
                                    "display_name",
                                    header.asJsonObject.get("display_name").asString,
                                )
                            }
                        }

                        runCatching {
                            val publisher = msg.get("mmreader").asJsonObject.get("publisher")
                            if (publisher.isJsonObject && tpl.get("display_name").asString.isEmpty()) {
                                tpl.addProperty(
                                    "display_name",
                                    publisher.asJsonObject.get("nickname").asString,
                                )
                            }
                        }

                        runCatching {
                            // 加上公众号信息
                            tpl.addProperty(
                                "source",
                                json.get("msg").asJsonObject.get("appinfo")
                                    .asJsonObject.get("appname").asString,
                            )
                        }

                        putCache(tpl)
                        val result = JsonObject()
                        result.add("mMap", tpl)

                        logger.debug { "微信支付数据：$result" }

                        AppRuntime.manifest.analysisData(DataType.DATA, result.toString())
                    }
                    419430449 -> {
                        //微信转账消息
                        val content = contentValues.get("content").toString()
                        val json = JsonObject()
                        json.addProperty("type", "transfer")
                        json.addProperty("isSend",contentValues.getAsInteger("isSend"))
                        json.addProperty("content", xmlToJson(content))
                        json.addProperty(ChatUserHooker.CHAT_USER,ChatUserHooker.get(contentValues.get("talker").toString()))

                        putCache(json)
                        AppRuntime.manifest.analysisData(DataType.DATA, json.toString())
                    }
                    10000 -> {
                        // 微信支付群收款
                        val json = JsonObject()
                        val content = contentValues.get("content").toString()
                        // 排除不是群收款的
                        if (!content.contains("群收款")) return@after
                        json.addProperty("type", "groupCollection")
                        json.add("content", Gson().toJsonTree(contentValues))
                        json.addProperty("isSend",contentValues.getAsInteger("isSend"))
                        json.addProperty(ChatUserHooker.CHAT_USER,ChatUserHooker.get(contentValues.get("talker").toString()))
                        putCache(json)
                        AppRuntime.manifest.analysisData(DataType.DATA, json.toString())

                    }
                    436207665 -> { //微信红包
                        val json = JsonObject()
                        val content = contentValues.get("content").toString()
                        json.addProperty("type", "redPackage")
                        json.addProperty("content", xmlToJson(content))
                        json.addProperty("isSend",contentValues.getAsInteger("isSend"))
                        json.addProperty(ChatUserHooker.CHAT_USER,ChatUserHooker.get(contentValues.get("talker").toString()))
                        putCache(json)
                        AppRuntime.manifest.analysisData(DataType.DATA, json.toString())

                    }
                }
            } else if (tableName == "AppMessage") {
                if (type == 5) {
                    if (contentValues.get("source").equals("微信支付")) {
                        // 微信支付
                        return@after
                    }
                    // 这个应该是公众号推送
                    AppRuntime.manifest.analysisData(DataType.DATA, Gson().toJson(contentValues))
                } else if (type == 2000) {
                    // 这个应该是微信转账给别人
                    val xml = contentValues.get("xml")
                    if (xml === null) return@after
                    val json = JsonObject()
                    json.addProperty("type", "transfer")
                    json.addProperty("content", xmlToJson(xml as String))
                    putCache(json)
                    AppRuntime.manifest.analysisData(DataType.DATA, json.toString())
                }
            }
        }
    }


    private fun putCache(json: JsonObject) {
       // json.addProperty(ChatUserHooker.CHAT_USER, DataUtils.get(ChatUserHooker.CHAT_USER))
        json.addProperty(
            PayToolsHooker.PAY_TOOLS,
            AppRuntime.memoryCache.get(PayToolsHooker.PAY_TOOLS) as? String ?: ""
        )
        json.addProperty(
            PayToolsHooker.PAY_MONEY,
            AppRuntime.memoryCache.get(PayToolsHooker.PAY_MONEY) as? String ?: ""
        )
        json.addProperty(
            PayToolsHooker.PAY_SHOP,
            AppRuntime.memoryCache.get(PayToolsHooker.PAY_SHOP) as? String ?: ""
        )
        json.addProperty("t", System.currentTimeMillis())
    }


}
