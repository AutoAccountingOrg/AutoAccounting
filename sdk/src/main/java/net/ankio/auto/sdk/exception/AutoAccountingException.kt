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

package net.ankio.auto.sdk.exception

class AutoAccountingException(message:String,val code:Int = 0) : Exception(message){

    companion object{
        const val CODE_SERVER_ERROR = 100 //服务未启动
        const val CODE_SERVER_AUTHORIZE = 101 //服务未授权
        const val CODE_SERVER_UN_INIT = 102 //没有初始化自动记账
    }

    fun getCode():Int{
        return code
    }
    override fun toString(): String {
        return "AutoAccountingException(message='$message', code=$code)"
    }
}