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

package net.ankio.auto.xposed.core.api

import android.app.Application
import com.google.gson.Gson
import net.ankio.auto.xposed.core.App
import net.ankio.dex.Dex
import net.ankio.dex.model.ClazzMethod

abstract class PartHooker {
    abstract fun hook(
        hookerManifest: HookerManifest,
        application: Application?,
        classLoader: ClassLoader
    )

    open val methodsRule = mutableListOf<Triple<String, String, ClazzMethod>>()
    open var method = mutableListOf<Triple<String, String, String>>()

    private val cache = HashMap<String, Class<*>>()

    fun method(clazzName: String, methodName: String, classLoader: ClassLoader): String {
        var clazz = cache[clazzName]
        if (clazz == null) {
            clazz = classLoader.loadClass(clazzName)
            cache[clazzName] = clazz
        }
        method.find { it.first == clazzName && it.second == methodName }?.let {
            return it.third
        }

        return ""
    }

    fun findMethods(clazzLoader: ClassLoader, hookerManifest: HookerManifest): Boolean {
        val code = App.getVersionCode()
        val adaptationVersion = App.get("methods_adaptation").toIntOrNull() ?: 0
        if (adaptationVersion == code) {
            runCatching {
                method =
                    Gson().fromJson(
                        App.get("clazz_method"),
                        List::class.java,
                    ) as MutableList<Triple<String, String, String>>
                if (method.size != methodsRule.size) {
                    throw Exception("需要重新适配！")
                }
            }.onFailure {
                App.get("methods_adaptation", "0")
                method.clear()
            }.onSuccess {
                return true
            }
        }
        methodsRule.forEach {
            val (clazz, methodName, methodClazz) = it
            var loadClazz = cache[clazz]
            if (loadClazz == null) {
                loadClazz = clazzLoader.loadClass(clazz)
                cache[clazz] = loadClazz
            }
            val findMethod =
                Dex.findMethod(
                    loadClazz!!,
                    methodClazz
                )
            if (findMethod.isEmpty()) {
                return false
            }
            method.add(Triple(clazz, methodName, findMethod))
        }
        if (method.size != methodsRule.size) return false
        App.set("methods_adaptation", code.toString())
        App.set("clazz_method", Gson().toJson(method))
        return true
    }
}