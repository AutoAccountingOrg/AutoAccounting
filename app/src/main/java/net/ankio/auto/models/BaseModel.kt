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

package net.ankio.auto.models

import com.google.gson.Gson
import com.google.gson.JsonArray
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.Logger

/**
 * 基础数据类，用于和自动记账服务交互
 */
abstract class BaseModel {
    var id: Int = 0
    companion object{
        suspend inline fun <reified T : BaseModel> get(page: Int, pageSize:Int, others: HashMap<String, Any>): List<T> {
            others["page"] = page
            others["pageSize"] = pageSize
            // 获取子类名称
            val clazz = T::class.java.simpleName.replace("Model", "")
            val data = AppUtils.getService().sendMsg("/$clazz/get", others)
            return runCatching {
                // 使用 Gson 解析数据为 List<T>
                Gson().fromJson(data as JsonArray, Array<T>::class.java).toList()
            }.onFailure {
                Logger.e("BaseModelError", it)
            }.getOrDefault(emptyList())
        }
    }
}