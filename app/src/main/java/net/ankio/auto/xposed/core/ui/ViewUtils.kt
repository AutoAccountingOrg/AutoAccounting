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

package net.ankio.auto.xposed.core.ui

import android.view.View
import de.robv.android.xposed.XposedHelpers

object ViewUtils {
    fun getViewById(
        rClass: String,
        obj: View,
        classLoader: ClassLoader,
        id: String,
    ): View {
        val clazz = classLoader.loadClass(rClass)
        val resourceId = clazz.getField(id).getInt(null)
        // 调用 findViewById 并转换为 TextView
        return XposedHelpers.callMethod(
            obj,
            "findViewById",
            resourceId,
        ) as View
    }
}