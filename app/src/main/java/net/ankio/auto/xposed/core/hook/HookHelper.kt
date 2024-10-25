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

package net.ankio.auto.xposed.core.hook

import de.robv.android.xposed.XposedHelpers


fun XposedHelpers.mapFields(obj:Any,callback:(key:String,value:Any?)->Unit){
    val fields = obj.javaClass.declaredFields
    for (field in fields) {
        field.isAccessible = true
        val value = field.get(obj)
        callback(field.name,value)
    }
}

fun XposedHelpers.mapStaticFields(clazz:Class<*>,callback:(key:String,value:Any?)->Unit){
    val fields = clazz.declaredFields
    for (field in fields) {
        field.isAccessible = true
        val value = XposedHelpers.getStaticObjectField(clazz,field.name)
        callback(field.name,value)
    }
}
