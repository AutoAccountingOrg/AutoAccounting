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

package net.ankio.auto.hooks.wechat.hooks

import android.content.ContentValues
import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import fr.arnaudguyon.xmltojsonlib.XmlToJson
import kotlinx.coroutines.runBlocking
import net.ankio.auto.api.Hooker
import net.ankio.auto.api.PartHooker
import net.ankio.auto.constant.DataType



class DatabaseHooker (hooker: Hooker) : PartHooker(hooker){

    override fun onInit(classLoader: ClassLoader, context: Context) {
        val mAppClassLoader: ClassLoader = classLoader
        val mContext: Context = context

        //分析版本 8.0.43

        val database = XposedHelpers.findClass("com.tencent.wcdb.database.SQLiteDatabase", mAppClassLoader)
        XposedHelpers.findAndHookMethod(database, "insert",
            String::class.java,
            String::class.java,
            ContentValues::class.java, object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    val contentValues = param.args[2] as ContentValues
                    val tableName = param.args[0] as String
                    val arg = param.args[1] as String

                    logD("微信数据：${Gson().toJson(contentValues)} table:$tableName arg:$arg")

                    val type = contentValues.getAsInteger("type") ?: return
                    //补充数据
                    contentValues.put("tableName",tableName)
                    contentValues.put("arg",arg)

                    if(tableName == "message"){
                        if(type == 1){
                            //这是聊天消息，content就是对话内容
                        }else if (type == 318767153){
                            //这是消息盒子
                            val content = contentValues.get("content").toString()
                            if(!content.contains("CDATA[微信支付"))return //只对微信支付特殊处理
                            val json = Gson().fromJson(xmlToJson(content),JsonObject::class.java)
                            val tpl = Gson().fromJson("""
                                {
                                  "description": "",
                                        "source": "微信支付",
                                        "type": 5,
                                        "appId": "",
                                        "msgId": 99064,
                                        "title": ""
                                }
                            """.trimIndent(),JsonObject::class.java)

                            logD("微信支付数据JSON：$json")

                            val msg = json.get("msg").asJsonObject.get("appmsg").asJsonObject
                            tpl.addProperty("description",msg.get("des").asString)
                            tpl.addProperty("title",msg.get("title").asString)
                            tpl.addProperty("display_name",msg.get("mmreader").asJsonObject.get("template_header").asJsonObject.get("display_name").asString)


                            val result = JsonObject()
                            result.add("mMap",tpl)

                            logD("微信支付数据：$result")

                            analyzeData(DataType.App.ordinal,result.toString())

                        }
                    }else if (tableName == "AppMessage"){

                        if(type == 5){
                            if(contentValues.get("source").equals("微信支付")){
                                //微信支付
                                return
                            }
                            //这个应该是公众号推送
                            analyzeData(DataType.App.ordinal,Gson().toJson(contentValues))
                            return
                        }else if (type == 2000){
                            //补充用户数据
                            contentValues.put("hookUser",hooker.hookUtils.readData("hookerUser"))
                            //这个应该是微信转账给别人
                            val xml = contentValues.get("xml")

                            if(xml!== null){
                                contentValues.put("xml",xmlToJson(xml as String))
                                contentValues.put("cachedPayTools",hooker.hookUtils.readData("cachedPayTools"))
                                contentValues.put("cachedPayMoney",hooker.hookUtils.readData("cachedPayMoney"))
                                contentValues.put("cachedPayShop",hooker.hookUtils.readData("cachedPayShop"))

                            }

                        }
                    }else if(tableName == "bizchatmessage"){
                        //好像是小程序消息
                        if(type == 49){ //没啥有用的数据好像
                          /*  val xml = contentValues.get("xml")
                            if(xml!== null){
                                contentValues.put("xml",xmlToJson(xml as String))

                            }*/
                        }
                    }

                }
            })
    }

    fun xmlToJson(xml: String): String {
        val xmlToJson: XmlToJson = XmlToJson.Builder(xml).build()
        return xmlToJson.toString()
    }
    override val hookName: String
        get() = "微信数据库"


}