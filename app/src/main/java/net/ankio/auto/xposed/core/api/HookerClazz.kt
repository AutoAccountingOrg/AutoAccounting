/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.dex.model.Clazz

/**
 * 所有需要xposed调用的类都必须使用HookerClazz抽象出来
 */
abstract class HookerClazz {
    abstract var rule: Clazz //hook的类的规则

    private val manifest by lazy {
        AppRuntime.manifest
    }

    /**
     * 安全地根据规则名获取 Class；若不存在返回 null。
     */
    private fun clazzOrNull(name: String): Class<*>? {
        val result = manifest.clazz[name] ?: return null
        return runCatching { AppRuntime.classLoader.loadClass(result.clazzName) }.getOrNull()
    }

    /**
     * 根据当前子类的 `rule.name` 获取实际 `Class`。
     * 若名称为空或未命中映射，将抛出异常。
     */
    fun clazz(): Class<*> {
        val name = this::class.java.name ?: error("类名不允许为空")
        return clazz(name)
    }

    fun method(name: String): String {
        val clazz = this::class.java.name ?: error("类名不允许为空")
        return method(clazz, name)
    }

    fun clazz(name: String): Class<*> {
        return clazzOrNull(name)
            ?: throw IllegalStateException("找不到类: $name for ${manifest.packageName}")
    }

    fun method(clazzName: String, methodName: String): String {
        val clazzResult = manifest.clazz[clazzName] ?: error("找不到指定方法，类不存在：$clazzName")
        val method = clazzResult.methodResults[methodName]
            ?: error("找不到指定方法, $clazzName->$methodName")
        return method.methodName
    }
}