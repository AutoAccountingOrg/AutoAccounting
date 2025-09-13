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

package net.ankio.auto.xposed.hooks.qianji.filter

import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.core.api.HookerClazz
import net.ankio.auto.xposed.hooks.qianji.models.QjAssetAccountModel
import net.ankio.dex.model.Clazz

/**
 * 钱迹宿主 `AssetsFilter` 的轻量包装（Xposed 反射委托）。
 * - 完全转发宿主行为，不复制逻辑；
 * - 提供与其他过滤器一致的 Kotlin API；
 * - 支持传入包装对象或原始宿主对象。
 */
class AssetsFilter private constructor(private var filterObj: Any?) {

    companion object : HookerClazz() {
        /** 宿主类名 */
        private const val CLAZZ = "com.mutangtech.qianji.filter.filters.AssetsFilter"

        /** 精确类名规则 */
        override var rule = Clazz(name = this::class.java.name, nameRule = CLAZZ)

        /** 新建空过滤器 */
        fun newInstance(): AssetsFilter = fromObject(XposedHelpers.newInstance(clazz()))

        /** 使用单个资产账户构造 */
        fun withAsset(account: QjAssetAccountModel): AssetsFilter =
            fromObject(XposedHelpers.newInstance(clazz(), account.toObject()))

        /** 包装已有宿主对象 */
        fun fromObject(obj: Any): AssetsFilter {
            if (obj::class.java.name != CLAZZ) {
                throw IllegalArgumentException("${obj::class.java.name} must be instance of $CLAZZ")
            }
            return AssetsFilter(obj)
        }
    }

    /** 返回底层宿主对象 */
    fun toObject(): Any = filterObj!!

    /** 添加资产账户（包装对象） */
    fun add(account: QjAssetAccountModel) {
        XposedHelpers.callMethod(filterObj, "add", account.toObject())
    }

    /** 添加资产账户（原始宿主对象） */
    fun addRaw(accountObj: Any?) {
        if (accountObj == null) return
        XposedHelpers.callMethod(filterObj, "add", accountObj)
    }

    /** 清空 */
    fun clear() {
        XposedHelpers.callMethod(filterObj, "clear")
    }

    /** 首个资产账户（包装返回，可能为 null） */
    fun getFirst(): QjAssetAccountModel? =
        (XposedHelpers.callMethod(
            filterObj,
            "getFirst"
        ) as Any?)?.let { QjAssetAccountModel.fromObject(it) }

    /** 键名固定为 "assets" */
    fun getKey(): String = XposedHelpers.callMethod(filterObj, "getKey") as String

    /** 当前数量 */
    fun getSize(): Int = XposedHelpers.callMethod(filterObj, "getSize") as Int

    /** 值序列化：以逗号拼接的资产 ID 列表 */
    fun value(): String = XposedHelpers.callMethod(filterObj, "getValue") as String

    /** 资产 ID 集合 */
    fun ids(): Set<Long> =
        (XposedHelpers.callMethod(filterObj, "ids") as Set<*>).mapNotNull {
            when (it) {
                is Long -> it
                is Int -> it.toLong()
                is String -> it.toLongOrNull()
                else -> null
            }
        }.toCollection(LinkedHashSet())
}