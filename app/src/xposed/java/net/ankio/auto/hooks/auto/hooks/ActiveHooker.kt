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

package net.ankio.auto.hooks.auto.hooks

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.api.Hooker
import net.ankio.auto.api.PartHooker
import net.ankio.auto.database.table.AccountMap
import net.ankio.auto.utils.ActiveUtils


class ActiveHooker(hooker: Hooker) : PartHooker(hooker) {
    override fun onInit(classLoader: ClassLoader?, context: Context?) {
        val activeUtils =
            XposedHelpers.findClass("net.ankio.auto.utils.ActiveUtils", classLoader)
        // hook激活方法
        XposedHelpers.findAndHookMethod(
            activeUtils,
            "getActiveAndSupportFramework",
            XC_MethodReplacement.returnConstant(true))


        // hook get方法
        XposedHelpers.findAndHookMethod(
            activeUtils,
            "get",
            String::class.java,
            String::class.java,
            object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    val key  = param.args[0] as String
                    val app  = param.args[1] as String
                    return try {
                        val pref = XSharedPreferences(app, "AutoAccounting")
                        pref.reload()
                        pref.getString(key,"")?:""
                    }catch (e:Exception){
                        log("获取配置异常：${e.message}")
                        ""
                    }
                }
            })
        // hook getAccountMap方法
        XposedHelpers.findAndHookMethod(
            activeUtils,
            "getAccountMap",
            object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    val string = ActiveUtils.get("AccountMap")
                    if(string.isEmpty()){
                        return arrayListOf<AccountMap>()
                    }
                    return  Gson().fromJson(string, object : TypeToken<List<AccountMap?>?>() {}.type)
                }
            })

    }
}