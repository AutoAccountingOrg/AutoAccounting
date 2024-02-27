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

package net.ankio.auto.app.js

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.ankio.auto.app.BillUtils
import net.ankio.auto.database.table.BillInfo
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.DateUtils
import net.ankio.auto.utils.HookUtils
import net.ankio.auto.utils.Logger
import net.ankio.common.constant.BillType
import org.json.JSONObject
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import kotlin.coroutines.resume

/**
 * 在Xposed环境中也需要调用
 */
object Engine {
    suspend fun analyze(
        dataType: Int, //类型
        app: String,  //来自哪个App或者手机号
        data: String, //具体的数据
        hookUtils: HookUtils? = null,
    ): BillInfo? = withContext(Dispatchers.IO) {

        val outputBuilder = StringBuilder()
        val rule = async {
            suspendCancellableCoroutine<String> { continuation ->
                AppUtils.getService().get("auto_rule", onSuccess = { result ->
                    continuation.resume(result)
                })
            }
        }

        val result = runCatching {
            val context: Context = Context.enter()
            val scope: Scriptable = context.initStandardObjects()
            context.setOptimizationLevel(-1)
            //识别脚本补充
            val js = "var window = {data:data, dataType:dataType, app:app};${rule.await()}"

            log(hookUtils, "执行识别脚本", js)
            ScriptableObject.putProperty(scope, "data", data)
            ScriptableObject.putProperty(scope, "dataType", dataType)
            ScriptableObject.putProperty(scope, "app", app)
            val printFunction = CustomPrintFunction(outputBuilder)
            ScriptableObject.putProperty(scope, "print", printFunction)
            context.evaluateString(scope, js, "<analyze>", 1, null)
            val json = outputBuilder.toString()
            log(hookUtils, "识别结果", json)
            //{"type":1,"money":"0.01","fee":0,"shopName":"支付宝商家服务","shopItem":"老顾客消费","accountNameFrom":"","accountNameTo":"支付宝余额","currency":"CNY","time":1703056950000,"channel":"支付宝收款码收款","ruleName":"支付宝消息盒子"}


            val billInfo = BillInfo()
            val jsonObject2 = JSONObject(json)
            billInfo.type = BillType.fromInt(jsonObject2.getInt("type"))
            billInfo.money =
                (BillUtils.removeSpecialCharacters(jsonObject2.getString("money"))).toFloat()
            billInfo.fee =
                (BillUtils.removeSpecialCharacters(jsonObject2.getString("fee"))).toFloat()
            billInfo.shopItem = jsonObject2.getString("shopItem")
            billInfo.shopName = jsonObject2.getString("shopName")
            billInfo.accountNameFrom =
                BillUtils.getAccountMap(jsonObject2.getString("accountNameFrom"))
            billInfo.accountNameTo = BillUtils.getAccountMap(jsonObject2.getString("accountNameTo"))
            billInfo.currency =
                net.ankio.common.constant.Currency.valueOf(jsonObject2.getString("currency"))
            billInfo.timeStamp = DateUtils.getAnyTime(jsonObject2.getString("time"))
            billInfo.channel = jsonObject2.getString("channel")


            context.close()

            category(billInfo, hookUtils)

           billInfo
        }.onFailure {
            log(hookUtils, "识别脚本执行出错", it.message ?: "")
        }

        return@withContext result.getOrNull()

    }


    suspend fun category(billInfo: BillInfo, hookUtils: HookUtils?) = withContext(Dispatchers.IO) {
        val category = async {
            suspendCancellableCoroutine<String> { continuation ->
                AppUtils.getService().get("auto_category", onSuccess = { result ->
                    continuation.resume(result)
                })
            }
        }
        val categoryCustom = async {
            suspendCancellableCoroutine { continuation ->
                AppUtils.getService().get("auto_category_custom"){
                    continuation.resume(it)
                }
            }
        }
        val outputBuilder = StringBuilder()
        runCatching {
            val context: Context = Context.enter()
            context.setOptimizationLevel(-1)
            val categoryJs =
                "var window = {money:money, type:type, shopName:shopName, shopItem:shopItem, time:time};" +
                        "function getCategory(money,type,shopName,shopItem,time){ ${categoryCustom.await()} return null};" +
                        "var categoryInfo = getCategory(money,type,shopName,shopItem,time);" +
                        "if(categoryInfo !== null) { print(JSON.stringify(categoryInfo));  } else { ${category.await()} }"
            outputBuilder.clear() //清空
            log(hookUtils, "执行分类脚本", categoryJs)

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
            val printFunction = CustomPrintFunction(outputBuilder)
            ScriptableObject.putProperty(categoryScope, "print", printFunction)
            context.evaluateString(categoryScope, categoryJs, "<category>", 1, null)
            val cateJson = JSONObject(outputBuilder.toString())
            billInfo.cateName = cateJson.getString("category")
            billInfo.bookName = cateJson.getString("book")
            log(hookUtils, "分类脚本执行结果", billInfo.cateName)
            context.close()
        }.onFailure {
            log(hookUtils, "分类脚本执行出错", it.message ?: "")
        }
    }

    private fun log(hookUtils: HookUtils? = null, prefix: String, data: String) {
        if (hookUtils !== null) {
            hookUtils.logD(prefix, data)
        } else {
            Logger.d("$prefix:$data")
        }
    }
}