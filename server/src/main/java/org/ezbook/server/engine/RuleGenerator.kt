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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ezbook.server.db.Db

/**
 * 这个代码运行在服务端，所以可以直接查库
 */
object RuleGenerator {
    // 生成 hook的 js代码
    suspend fun data(data:String,app:String,type:String = "data"):String = withContext(Dispatchers.IO){
        val rules = Db.get().ruleDao().loadAllEnabled(app,type)
        val js = StringBuilder()

        //注入common.js
        val commonJs = Db.get().settingDao().query("commonJs")?.value ?: ""
        js.append(commonJs)
        // 注入规则
        val jsonArray = JsonArray()
        rules.forEach {
            val jsonObject = JsonObject()
            jsonObject.addProperty("name",it.name)
            jsonObject.addProperty("obj",it.systemRuleName)
            js.append(it.js)
            jsonArray.add(jsonObject)
        }

        val escapedInput = data.replace("'", "\\'")

        // 注入检测逻辑

        //基础的js
        js.append("""
             var window = {};
             window.rules = $jsonArray;
             window.data = '$escapedInput';
             
             
             var data = window.data || '';

             var rules = window.rules || [];

         for (var i = 0; i < rules.length; i++) {
                var rule = rules[i];
                 var result = null;
  try {
    result = rule.obj.get(data);
    if (
      result !== null &&
      result.money !== null &&
      parseFloat(result.money) > 0
    ) {
      result.ruleName = rule.name;
      print(JSON.stringify(result));
      break;
    }
  } catch (e) {
    print(e.message);
  }
}


             
        """.trimIndent())

        js.toString()
    }
}