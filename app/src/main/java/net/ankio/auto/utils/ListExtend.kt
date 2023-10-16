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

import android.content.ContentValues
import com.google.gson.Gson
import com.google.gson.JsonArray
import kotlin.reflect.full.declaredMemberProperties
import android.database.Cursor
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties

fun <T> List<T>.toJsonArray(): JsonArray {
    val jsonArray = JsonArray()
    this.forEach { element ->
        val gsonElement = Gson().toJsonTree(element)
        jsonArray.add(gsonElement)
    }
    return jsonArray
}
fun Any.toContentValues(): ContentValues {
    val values = ContentValues()
    val properties = this::class.declaredMemberProperties

    for (prop in properties) {
        when (val value = prop.getter.call(this)) {
            is String -> values.put(prop.name, value)
            is Int -> values.put(prop.name, value)
            is Long -> values.put(prop.name, value)
            is Boolean -> values.put(prop.name, value)
            is Double -> values.put(prop.name, value)
            is Float -> values.put(prop.name, value)
            // 添加其他支持的数据类型
        }
    }

    return values
}




inline fun <reified T : Any> Cursor.toObjects(): List<T> {
    val objects = mutableListOf<T>()

    if (moveToFirst()) {
        do {
            val obj = T::class.createInstance()

            T::class.memberProperties.forEach { prop ->
                val columnIndex = getColumnIndex(prop.name)

                if (columnIndex != -1) {
                    when (prop.returnType.classifier) {
                        String::class -> (prop as? KMutableProperty<*>)?.setter?.call(obj, getString(columnIndex))
                        Int::class -> (prop as? KMutableProperty<*>)?.setter?.call(obj, getInt(columnIndex))
                        Long::class -> (prop as? KMutableProperty<*>)?.setter?.call(obj, getLong(columnIndex))
                        Boolean::class -> (prop as? KMutableProperty<*>)?.setter?.call(obj, getInt(columnIndex) != 0)
                        Double::class -> (prop as? KMutableProperty<*>)?.setter?.call(obj, getDouble(columnIndex))
                        Float::class -> (prop as? KMutableProperty<*>)?.setter?.call(obj, getFloat(columnIndex))
                        // 添加其他支持的数据类型
                    }
                }
            }

            objects.add(obj)
        } while (moveToNext())
    }

    return objects
}



