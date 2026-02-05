/*
 * Copyright (C) 2026 ankio(ankio@ankio.net)
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

package net.ankio.auto.xposed.hooks.qianji.models

import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.core.api.HookerClazz
import net.ankio.dex.model.Clazz

/**
 * 钱迹 TagGroup 模型包装类（Xposed 反射委托）。
 * - 仅透传宿主字段与方法，不复制业务逻辑；
 * - 保持与宿主类字段/方法一致，避免破坏兼容性。
 */
class QjTagGroupModel(private val groupObj: Any) {
    companion object : HookerClazz() {
        private const val CLAZZ = "com.mutangtech.qianji.data.model.TagGroup"

        override var rule = Clazz(name = this::class.java.name, nameRule = CLAZZ)

        fun fromObject(obj: Any): QjTagGroupModel = QjTagGroupModel(obj)
    }

    fun toObject(): Any = groupObj

    /**
     * 获取分组ID
     */
    fun getGroupId(): String? {
        return XposedHelpers.callMethod(groupObj, "getGroupId") as String?
    }

    /**
     * 获取分组名称
     */
    fun getName(): String? {
        return XposedHelpers.callMethod(groupObj, "getName") as String?
    }

    /**
     * 获取用户ID
     */
    fun getUserId(): String? {
        return XposedHelpers.callMethod(groupObj, "getUserId") as String?
    }

    /**
     * 获取账本ID
     */
    fun getBookId(): Long? {
        return XposedHelpers.callMethod(groupObj, "getBookId") as Long?
    }

    /**
     * 获取排序
     */
    fun getSort(): Int {
        return XposedHelpers.callMethod(groupObj, "getSort") as Int
    }

    /**
     * 获取创建时间
     */
    fun getCreateTime(): Long {
        return XposedHelpers.callMethod(groupObj, "getCreateTime") as Long
    }

    /**
     * 获取标签列表（包装为 QjTagModel）
     */
    fun getTagList(): List<QjTagModel> {
        val list = XposedHelpers.getObjectField(groupObj, "tagList") as? List<*> ?: emptyList<Any>()
        return list.mapNotNull { it?.let { QjTagModel.fromObject(it) } }
    }
}