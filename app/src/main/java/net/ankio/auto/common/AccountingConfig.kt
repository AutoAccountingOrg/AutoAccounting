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

package net.ankio.auto.common

import com.google.gson.Gson
import net.ankio.auto.utils.SpUtils

data class AccountingConfig(
    var assetManagement: Boolean = true,//是否开启资产管理
    var multiCurrency: Boolean = true,//是否开启多币种
    var reimbursement: Boolean = true,//是否开启报销
    var lending: Boolean = true,//是否开启债务功能
    var multiBooks: Boolean = true,//是否开启多账本
    var fee: Boolean = true,//是否开启手续费
){
    override fun toString(): String {
        return "AccountingConfig(assetManagement=$assetManagement, multiCurrency=$multiCurrency, reimbursement=$reimbursement, lending=$lending, multiBooks=$multiBooks, fee=$fee)"
    }

    companion object{
        fun get():AccountingConfig{
            return runCatching {
                Gson().fromJson(SpUtils.getString("auto_config",""),AccountingConfig::class.java)
            }.getOrNull()?:AccountingConfig()
        }

        fun set(config: AccountingConfig){
            SpUtils.putString("auto_config",Gson().toJson(config))
        }

        fun setItem(key:String,value:Boolean){
            val config = get()
            when(key){
                "assetManagement" -> config.assetManagement = value
                "multiCurrency" -> config.multiCurrency = value
                "reimbursement" -> config.reimbursement = value
                "lending" -> config.lending = value
                "multiBooks" -> config.multiBooks = value
                "fee" -> config.fee = value
            }
            set(config)
        }
    }

}