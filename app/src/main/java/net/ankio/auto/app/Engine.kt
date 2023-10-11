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

import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.json.JsonParser
import java.lang.Exception


object Engine {

    /**
     *   App(0),//app
     *     Sms(1),//短信
     *     Notice(2),//通知
     *     Helper(3)//无障碍
     */
    fun runAnalyze(dataType: Int, app: String, data: String, script: String):String {
        var result = ""
        try {
            val context: Context = Context.enter()
            val scope: Scriptable = context.initStandardObjects()
            val parser = JsonParser(context, scope)
            val jsonObject = parser.parseValue(data.trimIndent())
            ScriptableObject.putProperty(scope, "json", jsonObject)
            ScriptableObject.putProperty(scope, "dataType", dataType)
            ScriptableObject.putProperty(scope, "app", app)
            result = context.evaluateString(scope, script, "JavaScriptForRule", 1, null) as String
        }catch (e:Exception){

        }finally {
            Context.exit()
        }
        return result

    }

    /**
     *
     */
    fun runCategory(money: Float, type: Int, shopName: String, shopItem: String,time:String, script: String):String {
        var result = ""
        try {
            val context: Context = Context.enter()
            val scope: Scriptable = context.initStandardObjects()
            ScriptableObject.putProperty(scope, "money", money)
            ScriptableObject.putProperty(scope, "type", type)
            ScriptableObject.putProperty(scope, "shopName", shopName)
            ScriptableObject.putProperty(scope, "shopItem", shopItem)
            ScriptableObject.putProperty(scope, "time", time)
             result = context.evaluateString(scope, script, "JavaScriptForCategory", 1, null) as String

        }catch (_:Exception){

        }finally {
            Context.exit()
        }
        return result
    }


}