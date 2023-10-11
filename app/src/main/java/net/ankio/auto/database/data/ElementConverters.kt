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

package net.ankio.auto.database.data

import androidx.room.TypeConverter
import com.google.gson.Gson

class ElementConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromElement(data: FlowElementList): String = gson.toJson(data)

    @TypeConverter
    fun toElement(json: String): FlowElementList = gson.fromJson(json, FlowElementList::class.java)
}
