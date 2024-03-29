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
import net.ankio.auto.constant.toDataType
import net.ankio.auto.database.table.BillInfo
import net.ankio.auto.utils.AppTimeMonitor
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.DateUtils
import net.ankio.auto.utils.Logger
import net.ankio.common.constant.BillType
import org.json.JSONObject
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import kotlin.coroutines.resume


object Engine {
    suspend fun analyze(
        dataType: Int, //类型
        app: String,  //来自哪个App或者手机号
        data: String, //具体的数据
    ): BillInfo? = withContext(Dispatchers.IO) {
        AppTimeMonitor.startMonitoring("规则识别")
        val billInfo = data(dataType, app, data) ?: return@withContext null
        category(billInfo)

        //做一些收尾工作
        // 1. 识别备注
        val tpl = async {
            suspendCancellableCoroutine { continuation ->
                AppUtils.getService().get("setting_bill_remark", onSuccess = { result ->
                    continuation.resume(result)
                })
            }
        }

        billInfo.remark  = BillUtils.getRemark(billInfo,tpl.await())

        // 2. 识别账户
        BillUtils.setAccountMap(billInfo)

        AppTimeMonitor.stopMonitoring("规则识别")
        return@withContext billInfo
    }


    private fun getMoney(data: String): Int {
        val regex = Regex("【^\\d.】")
        return BillUtils.getMoney((regex.replace(data, "")).toFloat())
    }

    suspend fun data(dataType: Int, app: String, data: String):BillInfo? = withContext(Dispatchers.IO) {
        log( "识别数据", "dataType:$dataType,app:$app,data:$data")
        val outputBuilder = StringBuilder()
        val rule = async {
            suspendCancellableCoroutine { continuation ->
                AppUtils.getService().get("auto_rule", onSuccess = { result ->
                    continuation.resume(result)
                })
            }
        }


        val billInfo = BillInfo()
        try {

            //识别脚本补充
            val js = "var window = {data:data, dataType:dataType, app:app};${rule.await()}"
            log( "执行识别脚本", js)
            val context: Context = Context.enter()
            val scope: Scriptable = context.initStandardObjects()
            context.setOptimizationLevel(-1)
            ScriptableObject.putProperty(scope, "data", data)
            ScriptableObject.putProperty(scope, "dataType", dataType)
            ScriptableObject.putProperty(scope, "app", app)
            val printFunction = CustomPrintFunction(outputBuilder)
            ScriptableObject.putProperty(scope, "print", printFunction)
            context.evaluateString(scope, js, "<analyzeBill>", 1, null)
            val json = outputBuilder.toString()
            log( "识别结果", json)
            val jsonObject2 = JSONObject(json)
            billInfo.type = BillType.fromInt(jsonObject2.getInt("type"))
            billInfo.money = getMoney(jsonObject2.getString("money"))
            billInfo.fee = getMoney(jsonObject2.getString("fee"))
            billInfo.shopItem = jsonObject2.getString("shopItem")
            billInfo.shopName = jsonObject2.getString("shopName")
            billInfo.accountNameFrom =jsonObject2.getString("accountNameFrom")
            billInfo.accountNameTo = jsonObject2.getString("accountNameTo")
            billInfo.currency =
                net.ankio.common.constant.Currency.valueOf(jsonObject2.getString("currency"))
            billInfo.timeStamp = DateUtils.getAnyTime(jsonObject2.getString("time"))
            billInfo.channel = jsonObject2.getString("channel")
            billInfo.fromType = dataType.toDataType()
            billInfo.from = app
            log( "最终整合", billInfo.toString())
        } catch (e: Exception) {
            log( "识别脚本执行出错", e.message ?: "",e)
        }finally {
          runCatching {
              Context.exit()
          }
        }
        return@withContext if(billInfo.money <= 0)null else billInfo
    }

    /**
     * 规则只能识别出一节分类，只有用户手动选择的时候才会有二级分类
     */

    suspend fun category(billInfo: BillInfo) = withContext(Dispatchers.IO) {
        val category = async {
            suspendCancellableCoroutine { continuation ->
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

        try {

            val categoryJs =
                "var window = {money:money, type:type, shopName:shopName, shopItem:shopItem, time:time};" +
                        "function getCategory(money,type,shopName,shopItem,time){ ${categoryCustom.await()} return null};" +
                        "var categoryInfo = getCategory(money,type,shopName,shopItem,time);" +
                        "if(categoryInfo !== null) { print(JSON.stringify(categoryInfo));  } else { ${category.await()} }"
            outputBuilder.clear() //清空
            log( "执行分类脚本", categoryJs)
            val context: Context = Context.enter()
            context.setOptimizationLevel(-1)
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
            log( "分类脚本执行结果", billInfo.cateName)

        }catch (e:Exception){
            log( "分类脚本执行出错", e.message ?: "",e)
        }finally {
            runCatching {
                Context.exit()
            }
        }

    }

    private fun log(prefix: String, data: String,throwable: Throwable?=null) {
        if(throwable!==null){
            Logger.e("$prefix: $data",throwable)
        }else{
            Logger.d("$prefix: $data")
        }
        AppUtils.getService().putLog("$prefix: $data")
    }
}