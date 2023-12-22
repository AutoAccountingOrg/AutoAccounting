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

import net.ankio.auto.BuildConfig
import net.ankio.auto.constant.BillType
import net.ankio.auto.constant.Currency
import net.ankio.auto.database.table.BillInfo
import net.ankio.auto.utils.ActiveUtils
import net.ankio.auto.utils.DateUtils
import net.ankio.auto.utils.HookUtils
import org.json.JSONObject
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

class CustomPrintFunction(private val output: StringBuilder) : BaseFunction() {
    override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<Any>): Any {
        args.forEach { arg ->
            output.append(Context.toString(arg)).append("\n")
        }
        return Context.getUndefinedValue()
    }
}

object Engine {

    /**
     *   App(0),//app
     *     Sms(1),//短信
     *     Notice(2),//通知
     *     Helper(3)//无障碍
     */
    fun runAnalyze(
        dataType: Int, //类型
        app: String,  //来自哪个App或者手机号
        data: String, //具体的数据
        hookUtils: HookUtils? = null, //hook工具
    ): BillInfo? {
        var billInfo: BillInfo?
        val outputBuilder = StringBuilder()
        try {
            val context: Context = Context.enter()
            val scope: Scriptable = context.initStandardObjects()
            context.setOptimizationLevel(-1)
            //识别脚本补充
            var js = "var window = {data:data, dataType:dataType, app:app};${hookUtils?.getConfig("dataRule")}"
            hookUtils?.logD("执行识别脚本", js)
            ScriptableObject.putProperty(scope, "data", data)
            ScriptableObject.putProperty(scope, "dataType", dataType)
            ScriptableObject.putProperty(scope, "app", app)
            val printFunction = CustomPrintFunction(outputBuilder)
            ScriptableObject.putProperty(scope, "print", printFunction)
            context.evaluateString(scope, js, "<analyze>", 1, null)
            val json = outputBuilder.toString()
            hookUtils?.logD("识别结果", json)
            //{"type":1,"money":"0.01","fee":0,"shopName":"支付宝商家服务","shopItem":"老顾客消费","accountNameFrom":"","accountNameTo":"支付宝余额","currency":"CNY","time":1703056950000,"channel":"支付宝收款码收款","ruleName":"支付宝消息盒子"}
            billInfo = BillInfo()
            val jsonObject2 = JSONObject(json)
            billInfo.type = BillType.values()[jsonObject2.getInt("type")]
            billInfo.money =
                (BillUtils.removeSpecialCharacters(jsonObject2.getString("money"))).toFloat()
            billInfo.fee =
                (BillUtils.removeSpecialCharacters(jsonObject2.getString("fee"))).toFloat()
            billInfo.shopItem = jsonObject2.getString("shopItem")
            billInfo.shopName = jsonObject2.getString("shopName")
            billInfo.accountNameFrom =
                BillUtils.getAccountMap(jsonObject2.getString("accountNameFrom"))
            billInfo.accountNameTo = BillUtils.getAccountMap(jsonObject2.getString("accountNameTo"))
            billInfo.currency = Currency.valueOf(jsonObject2.getString("currency"))
            billInfo.timeStamp = DateUtils.getAnyTime(jsonObject2.getString("time"))
            billInfo.channel = jsonObject2.getString("channel")
            //分类脚本补充
            js =
                "var window = {money:money, type:type, shopName:shopName, shopItem:shopItem, time:time};${hookUtils?.getConfig("dataCategory")}"
            outputBuilder.clear();//清空
            hookUtils?.logD("执行分类脚本", js)

            val categoryScope: Scriptable = context.initStandardObjects()
            ScriptableObject.putProperty(categoryScope, "money", billInfo.money)
            ScriptableObject.putProperty(categoryScope, "type", billInfo.type)
            ScriptableObject.putProperty(categoryScope, "shopName", billInfo.shopName)
            ScriptableObject.putProperty(categoryScope, "shopItem", billInfo.shopItem)
            ScriptableObject.putProperty(
                categoryScope,
                "time",
                DateUtils.stampToDate(billInfo.timeStamp, "HH:mm")
            )
            ScriptableObject.putProperty(categoryScope, "print", printFunction)
            context.evaluateString(categoryScope, js, "<category>", 1, null)
            val cateJson = JSONObject(outputBuilder.toString())
            billInfo.cateName = cateJson.getString("category")
            billInfo.bookName = cateJson.getString("book")
            hookUtils?.logD("分类脚本执行结果", billInfo.cateName)

        } catch (e: Exception) {
            hookUtils?.logD("执行脚本失败",  e.stackTraceToString())
            billInfo = null
        } finally {
            Context.exit()
            hookUtils?.logD("脚本执行完毕",  "")
        }
        return billInfo

    }


}