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

package net.ankio.auto.xposed.hooks.alipay.hooks

import com.google.gson.Gson
import com.google.gson.JsonArray
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import org.ezbook.server.constant.DataType

class MessageBoxHooker : PartHooker() {
    override fun hook() {

        val syncMessage =
            Hooker.loader("com.alipay.mobile.rome.longlinkservice.syncmodel.SyncMessage")


        // 有些路径不会主动调用 getData，仅调用 toString 写日志
        // 这里仅触发 getData，让统一的 getData 钩子完成处理；自身不做解析
        Hooker.after(syncMessage, "toString") { methodHookParam ->
            XposedHelpers.callMethod(methodHookParam.thisObject, "getData")
        }

        Hooker.after(syncMessage, "getData") { methodHookParam ->
            // 拦截 getData 获取原始同步消息内容，避免与 toString 造成的二次触发
            val result = methodHookParam.result as String
            val gson = Gson()

            val array = gson.fromJson(result, JsonArray::class.java)
            d("MessageBoxHooker: Alipay sync message : ${result.take(200)}")
            array.forEach { jsonObject ->
                val jsonArray = JsonArray().apply { add(jsonObject) }
                analysisData(DataType.DATA, gson.toJson(jsonArray))
            }
        }


    }
}
