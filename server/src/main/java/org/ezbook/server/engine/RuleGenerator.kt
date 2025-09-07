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
import org.ezbook.server.tools.SettingUtils

/**
 * 这个代码运行在服务端，所以可以直接查库
 */
object RuleGenerator {
    // 生成 hook的 js代码
    // creator: "system" 生成系统规则，"user" 生成用户规则；为兼容旧接口，默认使用全部（保持原行为）
    suspend fun data(app: String, type: DataType, creator: String? = null): String {
        val rules = when (creator) {
            "system" -> Db.get().ruleDao().loadAllEnabledByCreator(app, type.name, "system")
            "user" -> Db.get().ruleDao().loadAllEnabledByCreator(app, type.name, "user")
            else -> Db.get().ruleDao().loadAllEnabled(app, type.name)
        }
        if (rules.isEmpty()) return ""
        val js = StringBuilder()

        //注入common.js
        val commonJs = SettingUtils.jsCommon()

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

        js.append("\n")
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
    continue; // 出错就跳过
  }

  if (result && result.money && parseFloat(result.money) > 0) {
    result.ruleName = rule.name;
    print(JSON.stringify(result));
    break;
  }
}



             
        """.trimIndent()
        )

        return js.toString()
    }

    suspend fun category(): String {
        val categoryCustom = Db.get().categoryRuleDao().loadAll().joinToString("\n") {
            it.js
        }
        val category = SettingUtils.jsCategory()
        //注入common.js
        val commonJs = SettingUtils.jsCommon()
        return """
            var window = JSON.parse(data);
            $commonJs
            function getCategory(money,type,shopName,shopItem,time,ruleName){ $categoryCustom return null;};
            var categoryInfo = getCategory(window.money,window.type,window.shopName,window.shopItem,window.time,window.ruleName);
            if(categoryInfo !== null) { 
                print(JSON.stringify(categoryInfo));  
            } else {
                $category
                print(JSON.stringify(category.get(window.money, window.type, window.shopName, window.shopItem, window.time,window.ruleName)));
            }
        """.trimIndent()
    }

}