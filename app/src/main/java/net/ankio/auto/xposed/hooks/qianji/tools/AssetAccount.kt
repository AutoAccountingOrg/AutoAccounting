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

package net.ankio.auto.xposed.hooks.qianji.tools

import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.hooks.qianji.tools.LoanInfo

class AssetAccount(private val classLoader: ClassLoader, private var obj: Any? = null) {

    val assetClazz by lazy {
        XposedHelpers.findClass(
            "com.mutangtech.qianji.data.model.AssetAccount",
            classLoader
        )
    }

    init {
        if (obj == null) {
            obj = XposedHelpers.newInstance(assetClazz)
            setMoney(0.0)
            addUserCount()
            setCurrency("CNY")
            setIcon("")
            setIncount(0)
            setLastPayTime(0)
        }
    }

    val get = obj!!

    fun setMoney(f: Double) {
        XposedHelpers.callMethod(obj, "setMoney", f)
    }

    fun addMoney(f: Double) {
        XposedHelpers.callMethod(obj, "addMoney", f)
    }

    fun addUserCount() {
        XposedHelpers.callMethod(obj, "addUserCount")
    }

    fun changeMoney(f: Double) {
        XposedHelpers.callMethod(obj, "changeMoney", f)
    }

    fun getLoanInfo(): LoanInfo {
        return LoanInfo(classLoader, XposedHelpers.callMethod(obj, "getLoanInfo"))
    }

    fun getMoney(): Double {
        return XposedHelpers.callMethod(obj, "getMoney") as Double
    }


    fun getName(): String {
        return XposedHelpers.callMethod(obj, "getName") as String
    }

    fun getStype(): Int {
        return XposedHelpers.callMethod(obj, "getStype") as Int
    }

    fun getType(): Int {
        return XposedHelpers.callMethod(obj, "getType") as Int
    }

    fun getUsecount(): Int {
        return XposedHelpers.callMethod(obj, "getUsecount") as Int
    }

    fun getUserid(): String {
        return XposedHelpers.callMethod(obj, "getUserid") as String
    }

    fun setCurrency(s: String) {
        XposedHelpers.callMethod(obj, "setCurrency", s)
    }


    fun setIcon(s: String) {
        XposedHelpers.callMethod(obj, "setIcon", s)
    }


    fun setIncount(v: Int) {
        XposedHelpers.callMethod(obj, "setIncount", v)
    }


    fun setLastPayTime(v: Long) {
        XposedHelpers.callMethod(obj, "setLastPayTime", v)
    }

    fun setLoanInfo(loanInfo0: LoanInfo) {
        XposedHelpers.callMethod(obj, "setLoanInfo", loanInfo0.get)
    }


    fun setName(s: String) {
        XposedHelpers.callMethod(obj, "setName", s)
    }


    fun setStype(v: Int) {
        XposedHelpers.callMethod(obj, "setStype", v)
    }

    fun setType(v: Int) {
        XposedHelpers.callMethod(obj, "setType", v)
    }

    fun setUsecount(v: Int) {
        XposedHelpers.callMethod(obj, "setUsecount", v)
    }

    fun setUserid(s: String) {
        XposedHelpers.callMethod(obj, "setUserid", s)
    }

    fun getId(): Long {
        return XposedHelpers.callMethod(obj,"getId") as Long
    }
}