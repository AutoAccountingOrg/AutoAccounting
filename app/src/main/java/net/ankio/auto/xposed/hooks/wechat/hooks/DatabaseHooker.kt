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

import android.content.ContentValues
import com.google.gson.Gson
import com.google.gson.JsonObject
import fr.arnaudguyon.xmltojsonlib.XmlToJson
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.hooks.wechat.models.WechatUserModel
import org.ezbook.server.constant.DataType
import org.ezbook.server.tools.MD5HashTable
import org.ezbook.server.tools.MemoryCache

class DatabaseHooker : PartHooker() {

    /**
     * 消息表与类型常量，避免魔法数字与硬编码字符串。
     */
    private companion object {
        const val TABLE_MESSAGE = "message"
        const val TABLE_APP_MESSAGE = "AppMessage"

        // message 表 type 值
        const val TYPE_MSG_BOX = 318767153
        const val TYPE_TRANSFER = 419430449
        const val TYPE_GROUP_SYSTEM = 10000
        const val TYPE_RED_PACKAGE = 436207665

        // AppMessage 表 type 值
        const val APPMSG_TYPE_ARTICLE = 5
        const val APPMSG_TYPE_TRANSFER = 2000
    }

    private fun xmlToJson(xml: String): String {
        val xmlToJson: XmlToJson = XmlToJson.Builder(xml).build()
        return xmlToJson.toString()
    }

    private val mD5HashTable by lazy {
        MD5HashTable(300_000)
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

            // 仅处理关心的数据表
            if (tableName != TABLE_MESSAGE && tableName != TABLE_APP_MESSAGE) return@after

            val type = contentValues.getAsInteger("type") ?: return@after

            // 补充通用字段
            contentValues.put("tableName", tableName)
            contentValues.put("arg", arg)
            contentValues.put("t", System.currentTimeMillis()) // 微信常缺时间，补默认值

            when (tableName) {
                TABLE_MESSAGE -> handleMessageInsert(type, contentValues)
                TABLE_APP_MESSAGE -> handleAppMessageInsert(type, contentValues)
            }
        }
    }

    /** 处理 message 表插入 */
    private fun handleMessageInsert(type: Int, contentValues: ContentValues) {
        when (type) {
            TYPE_MSG_BOX -> handleMsgBox(contentValues)
            TYPE_TRANSFER -> handleMsgTransfer(contentValues)
            TYPE_GROUP_SYSTEM -> handleMsgGroupCollection(contentValues)
            TYPE_RED_PACKAGE -> handleMsgRedPackage(contentValues)
        }
    }

    /** 处理 AppMessage 表插入 */
    private fun handleAppMessageInsert(type: Int, contentValues: ContentValues) {
        if (type == APPMSG_TYPE_ARTICLE) {
            val md5 = MD5HashTable.md5(contentValues.get("description").toString())
            if (mD5HashTable.contains(md5)) {
                return
            }
            mD5HashTable.put(md5)
            analysisData(DataType.DATA, Gson().toJson(contentValues))
            return
        }
        if (type == APPMSG_TYPE_TRANSFER) {
            val xml = contentValues.get("xml") ?: return
            val json = JsonObject()
            json.addProperty("type", "transfer")
            json.addProperty("content", xmlToJson(xml as String))
            putCache(json)
            analysisData(DataType.DATA, json.toString())
        }
    }

    /** 消息盒子内容解析（支付通知类） */
    private fun handleMsgBox(contentValues: ContentValues) {
        val content = contentValues.get("content").toString()
        val json = Gson().fromJson(xmlToJson(content), JsonObject::class.java)
        // 构造基础模板，避免字符串解析带来的不必要开销
        val tpl = JsonObject().apply {
            addProperty("description", "")
            addProperty("source", "")
            addProperty("title", "")
        }


        val msg = json.get("msg").asJsonObject.get("appmsg").asJsonObject


        val md5 = MD5HashTable.md5(msg.get("des").asString.trim())
        if (mD5HashTable.contains(md5)) {
            return
        }
        mD5HashTable.put(md5)
        tpl.addProperty("description", msg.get("des").asString)


        tpl.addProperty("title", msg.get("title").asString)

        runCatching {
            val header = msg.get("mmreader").asJsonObject.get("template_header")
            if (header.isJsonObject) {
                tpl.addProperty("display_name", header.asJsonObject.get("display_name").asString)
            }
        }

        runCatching {
            val publisher = msg.get("mmreader").asJsonObject.get("publisher")
            if (publisher.isJsonObject && tpl.get("display_name").asString.isEmpty()) {
                tpl.addProperty("display_name", publisher.asJsonObject.get("nickname").asString)
            }
        }

        runCatching {
            tpl.addProperty(
                "source",
                json.get("msg").asJsonObject.get("appinfo").asJsonObject.get("appname").asString,
            )
        }

        putCache(tpl)
        val result = JsonObject().apply { add("mMap", tpl) }
        AppRuntime.manifest.logD("微信支付数据：$result")
        analysisData(DataType.DATA, result.toString())
    }

    /** 微信转账消息（message.type=419430449） */
    private fun handleMsgTransfer(contentValues: ContentValues) {
        val content = contentValues.get("content").toString()
        val json = JsonObject()
        json.addProperty("type", "transfer")
        json.addProperty("isSend", contentValues.getAsInteger("isSend"))
        json.addProperty("content", xmlToJson(content))
        json.addProperty(
            WechatUserModel.CHAT_USER,
            WechatUserModel.get(contentValues.get("talker").toString()),
        )
        putCache(json)
        analysisData(DataType.DATA, json.toString())
    }

    /** 群收款系统消息（message.type=10000，含“群收款”关键字） */
    private fun handleMsgGroupCollection(contentValues: ContentValues) {
        val content = contentValues.get("content").toString()
        // if (!content.contains("群收款")) return
        val json = JsonObject()
        json.addProperty("type", "groupCollection")
        json.add("content", Gson().toJsonTree(contentValues))
        json.addProperty("isSend", contentValues.getAsInteger("isSend"))
        json.addProperty(
            WechatUserModel.CHAT_USER,
            WechatUserModel.get(contentValues.get("talker").toString()),
        )
        putCache(json)
        analysisData(DataType.DATA, json.toString())
    }

    /** 微信红包（message.type=436207665） */
    private fun handleMsgRedPackage(contentValues: ContentValues) {
        val content = contentValues.get("content").toString()
        val json = JsonObject()
        json.addProperty("type", "redPackage")
        json.addProperty("content", xmlToJson(content))
        json.addProperty("isSend", contentValues.getAsInteger("isSend"))
        json.addProperty(
            WechatUserModel.CHAT_USER,
            WechatUserModel.get(contentValues.get("talker").toString()),
        )
        putCache(json)
        analysisData(DataType.DATA, json.toString())
    }


    private fun putCache(json: JsonObject) {
       // json.addProperty(ChatUserHooker.CHAT_USER, DataUtils.get(ChatUserHooker.CHAT_USER))
        json.addProperty(
            PayToolsHooker.PAY_TOOLS,
            MemoryCache.get(PayToolsHooker.PAY_TOOLS) as? String ?: ""
        )
        json.addProperty(
            PayToolsHooker.PAY_MONEY,
            MemoryCache.get(PayToolsHooker.PAY_MONEY) as? String ?: ""
        )
        json.addProperty(
            PayToolsHooker.PAY_SHOP,
            MemoryCache.get(PayToolsHooker.PAY_SHOP) as? String ?: ""
        )
        json.addProperty("t", System.currentTimeMillis())
    }


}
