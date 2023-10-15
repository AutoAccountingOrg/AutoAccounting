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

package net.ankio.auto.app

import net.ankio.auto.constant.BillType
import net.ankio.auto.constant.Currency
import net.ankio.auto.database.table.BillInfo
import net.ankio.auto.database.table.BookName
import net.ankio.auto.utils.ActiveUtils
import net.ankio.auto.utils.DateUtils
import net.ankio.auto.utils.SpUtils
import org.json.JSONObject
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.json.JsonParser


object Engine {

    /**
     *   App(0),//app
     *     Sms(1),//短信
     *     Notice(2),//通知
     *     Helper(3)//无障碍
     */
    suspend fun runAnalyze(dataType: Int, app: String, data: String ):BillInfo? {
        var billInfo:BillInfo?
        val script = ActiveUtils.get("dataRule")
        try {
            val context: Context = Context.enter()
            val scope: Scriptable = context.initStandardObjects()
            val parser = JsonParser(context, scope)
            val jsonObject = parser.parseValue(data.trimIndent())
            ScriptableObject.putProperty(scope, "json", jsonObject)
            ScriptableObject.putProperty(scope, "dataType", dataType)
            ScriptableObject.putProperty(scope, "app", app)
            val result = context.evaluateString(scope, script, "JavaScriptForRule", 1, null) as String
            //{"type":1,"money":"2","fee":0,"shopName":"","shopItem":"","accountNameFrom":"微信余额","accountNameTo":"","currency":"CNY"}
            billInfo =  BillInfo()
            val jsonObject2 = JSONObject(result)
            billInfo.type = BillType.valueOf(jsonObject2.getInt("type").toString())
            billInfo.money = (BillUtils.removeSpecialCharacters(jsonObject2.getString("money"))).toFloat()
            billInfo.fee = (BillUtils.removeSpecialCharacters(jsonObject2.getString("fee"))).toFloat()
            billInfo.shopItem = jsonObject2.getString("shopItem")
            billInfo.shopName = jsonObject2.getString("shopName")
            billInfo.accountNameFrom = BillUtils.getAccountMap(jsonObject2.getString("accountNameFrom"))
            billInfo.accountNameTo =  BillUtils.getAccountMap(jsonObject2.getString("accountNameTo"))
            billInfo.currency = Currency.valueOf(jsonObject2.getString("currency"))
            billInfo.timeStamp = DateUtils.getAnyTime(jsonObject2.getString("time"))
            billInfo.channel = jsonObject2.getString("channel")
            runCategory(billInfo)

        }catch (_:Exception){
            billInfo = null
        }finally {
            Context.exit()
        }
        return billInfo

    }

    /**
     *
     */
    private  fun runCategory(billInfo: BillInfo) {
        val script = ActiveUtils.get("dataCategory")
        try {
            val context: Context = Context.enter()
            val scope: Scriptable = context.initStandardObjects()
            ScriptableObject.putProperty(scope, "money", billInfo.money)
            ScriptableObject.putProperty(scope, "type", billInfo.type)
            ScriptableObject.putProperty(scope, "shopName", billInfo.shopName)
            ScriptableObject.putProperty(scope, "shopItem", billInfo.shopItem)
            ScriptableObject.putProperty(scope, "time", DateUtils.stampToDate(billInfo.timeStamp,"HH:mm"))
            billInfo.cateName = context.evaluateString(scope, script, "JavaScriptForCategory", 1, null) as String

        }catch (_:Exception){

        }finally {
            Context.exit()
        }
    }


}