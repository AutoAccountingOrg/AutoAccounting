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

package net.ankio.auto.xposed.hooks.common

import de.robv.android.xposed.XposedBridge
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.SettingModel
import java.lang.reflect.Field

object XposedFramework {
    fun framework(): String {
        val tagField: Field = XposedBridge::class.java.getDeclaredField("TAG")
        // 设置字段可访问
        tagField.isAccessible = true
        // 获取TAG字段的值
        return (tagField.get(null) as String).replace("-Bridge", "")
    }

    suspend fun init() {
        val framework = framework()
        SettingModel.set(Setting.KEY_FRAMEWORK, framework)
    }
}