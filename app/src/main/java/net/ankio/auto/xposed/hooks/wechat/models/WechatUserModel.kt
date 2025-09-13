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

package net.ankio.auto.xposed.hooks.wechat.models

import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.core.api.HookerClazz
import net.ankio.dex.model.Clazz
import net.ankio.dex.model.ClazzField
import net.ankio.dex.model.ClazzMethod
import org.ezbook.server.tools.MemoryCache

/**
 * 微信用户信息解析与轻量缓存工具。
 *
 * - 通过 Xposed 反射从微信对象读取备注、昵称与用户名。
 * - 优先展示备注，其次昵称；同时维护进程内 Map 与跨进程短期 `MemoryCache`。
 * - 纯工具类，无阻塞调用；默认由上层串行调用，不做并发保护。
 */
class WechatUserModel {

    companion object : HookerClazz() {
        /**
         * Dex 匹配规则：
         * - 目标类需包含 `field_conRemark`、`field_nickname`、`field_username` 三个 `String` 字段；
         * - 且包含 `convertFrom(android.database.Cursor)` 方法。
         */
        override var rule = Clazz(
            type = "class",
            name = this::class.java.name,
            fields = arrayListOf(
                ClazzField(
                    type = "java.lang.String",
                    name = "field_conRemark"
                ),
                ClazzField(
                    type = "java.lang.String",
                    name = "field_nickname"
                ),
                ClazzField(
                    type = "java.lang.String",
                    name = "field_username"
                ),
            ),
            methods = arrayListOf(
                ClazzMethod(
                    name = "convertFrom",
                    parameters = arrayListOf(
                        ClazzField(
                            type = "android.database.Cursor"
                        )
                    )
                )
            )
        )

        /** `MemoryCache` 的键名，用于跨进程读取最近解析的用户名 */
        const val CHAT_USER = "hookerUser"

        /** 写入 `MemoryCache` 的生存时间（秒） */
        private const val DURATION_SECONDS = 300L

        /**
         * 进程内用户名映射。
         * - key：微信内部 id 或昵称
         * - value：展示用用户名（备注优先，其次昵称）
         */
        private val users = hashMapOf<String, String>()

        /**
         * 解析微信用户对象并缓存。
         *
         * @param obj 目标对象，需包含字段 `field_conRemark`、`field_nickname`、`field_username`
         */
        fun parse(obj: Any) {
            val field_conRemark = XposedHelpers.getObjectField(obj, "field_conRemark") as? String
            val field_nickname = XposedHelpers.getObjectField(obj, "field_nickname") as? String
            val field_username = XposedHelpers.getObjectField(obj, "field_username") as? String
            val username = if (field_conRemark.isNullOrEmpty()) {
                field_nickname
            } else {
                field_conRemark
            }!!
            if (field_username.isNullOrEmpty()) return
            users[field_username] = username
            if (field_nickname != null) users[field_nickname] = username
            MemoryCache.put(CHAT_USER, username, DURATION_SECONDS)
        }

        /**
         * 根据微信 id/昵称获取展示名。
         * 先查进程内缓存，其次查 `MemoryCache`，都缺失时回退为入参。
         *
         * @param wx 微信用户唯一标识或昵称
         * @return 展示用用户名
         */
        fun get(wx: String): String {
            return users[wx] ?: MemoryCache.get(CHAT_USER) as? String ?: wx
        }
    }


}