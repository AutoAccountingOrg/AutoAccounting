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

package net.ankio.auto.hooks.qianji.tools

import de.robv.android.xposed.XposedHelpers

class LoanInfo(classLoader: ClassLoader,private var obj:Any? = null) {
    val loanInfoModelClazz  by lazy {
        XposedHelpers.findClass(
            "com.mutangtech.qianji.asset.model.LoanInfo",
            classLoader
        )
    }
    init {
        if (obj == null){
            obj = XposedHelpers.newInstance(loanInfoModelClazz)
            setTotalMoney(0.0)
            setTotalpay(0.0)
            setStartdate("")
            setEnddate("")
        }
    }

    val get = obj!!



    fun getEnddate(): String {
        return XposedHelpers.callMethod(obj,"getEnddate") as String
    }


    fun getStartdate(): String {
        return XposedHelpers.callMethod(obj,"getStartdate") as String
    }

    fun getTotalMoney(): Double {
        return XposedHelpers.callMethod(obj,"getTotalMoney") as Double
    }

    fun getTotalpay(): Double {
        return XposedHelpers.callMethod(obj,"getTotalpay") as Double
    }

    fun setEnddate(s: String) {
        XposedHelpers.callMethod(obj,"setEnddate",s)
    }



    fun setStartdate(s: String) {
        XposedHelpers.callMethod(obj,"setStartdate",s)
    }

    fun setTotalMoney(f: Double) {
        XposedHelpers.callMethod(obj,"setTotalMoney",f)
    }

    fun setTotalpay(f: Double) {
        XposedHelpers.callMethod(obj,"setTotalpay",f)
    }
}