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
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AnalysisUtils
import org.ezbook.server.constant.DataType
import org.ezbook.server.tools.MD5HashTable

class MessageBoxHooker : PartHooker() {
    // 基于内容哈希的短时去重，默认 TTL=60s，避免对象级别的膨胀
    private val mD5HashTable by lazy { MD5HashTable() }
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
            // 基于内容指纹进行去重
            val md5 = MD5HashTable.md5(result)
            if (mD5HashTable.contains(md5)) {
                return@after
            }
            mD5HashTable.put(md5)
            // 收到的是数组，拆分为单条逐一分析
            gson.fromJson(result, JsonArray::class.java).forEach { jsonObject ->

                val jsonArray =
                    JsonArray().apply {
                        add(jsonObject)
                    }

                this.d("Hooked Alipay Message Box：$jsonArray")
                // 调用分析服务进行数据分析
                analysisData(DataType.DATA, gson.toJson(jsonArray))
            }
        }


    }
}
