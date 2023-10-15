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

package net.ankio.auto.utils

import com.google.gson.Gson
import com.google.gson.JsonArray

fun <T> List<T>.toJsonArray(): JsonArray {
    val jsonArray = JsonArray()
    this.forEach { element ->
        val gsonElement = Gson().toJsonTree(element)
        jsonArray.add(gsonElement)
    }
    return jsonArray
}