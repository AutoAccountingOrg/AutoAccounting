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

package org.ezbook.server.engine

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.ezbook.server.constant.DataType
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.Db

/**
 * 这个代码运行在服务端，所以可以直接查库
 */
object RuleGenerator {
    // 生成 hook的 js代码
    fun data(app: String, type: DataType): String {
        val rules = Db.get().ruleDao().loadAllEnabled(app, type.name)
        val js = StringBuilder()

        //注入common.js
        val commonJs = Db.get().settingDao().query(Setting.JS_COMMON)?.value ?: ""
        js.append(commonJs)
        // 注入规则
        val jsonArray = JsonArray()

        rules.forEach {
            val jsonObject = JsonObject()
            jsonObject.addProperty("name", it.name)
            jsonObject.addProperty("obj", it.systemRuleName)
            js.append(it.js)
            jsonArray.add(jsonObject)
        }


        val rulesStr = jsonArray.toString().replace(Regex("\"(rule_\\d+)\""), "$1")
        // 注入检测逻辑

        //基础的js
        js.append(
            """
             var window = {};
             window.rules = $rulesStr;
             window.data = data;
             
             
             var data = window.data || '';

             var rules = window.rules || [];

         for (var i = 0; i < rules.length; i++) {
                var rule = rules[i];
                 var result = null;
  try {
    result = rule.obj.get(data);
    } catch (e) {
    //print(rule.name+"执行出错",e);
    continue;
  }
    if (
      result !== null &&
      result.money !== null &&
      parseFloat(result.money) > 0
    ) {
      result.ruleName = rule.name;
      print(JSON.stringify(result));
      break;
    }
  
}


             
        """.trimIndent()
        )

        return js.toString()
    }

    fun category(): String {
        val categoryCustom = Db.get().categoryRuleDao().loadAll().joinToString("\n") {
            it.js
        }
        val category = Db.get().settingDao().query(Setting.JS_CATEGORY)?.value ?: ""
        return "var window = JSON.parse(data);" +
                "function getCategory(money,type,shopName,shopItem,time){ $categoryCustom return null;};" +
                "var categoryInfo = getCategory(window.money,window.type,window.shopName,window.shopItem,window.time);" +
                "if(categoryInfo !== null) { print(JSON.stringify(categoryInfo));  } else { $category" +
                "print(JSON.stringify(category.get(window.money, window.type, window.shopName, window.shopItem, window.time))); " +
                "}"
    }
}