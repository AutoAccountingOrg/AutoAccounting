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

package net.ankio.auto.service.ocr

import org.json.JSONObject

/**
 * 页面特征签名
 *
 * 用于记住「包名 + Activity + 内容特征」，以便下次自动触发 OCR。
 * - [packageName] 应用包名
 * - [activityName] Activity 类名（无障碍模式下获取，可为空）
 * - [contentFingerprint] 页面内容指纹：OCR 文本的结构化摘要，用于模糊匹配
 */
data class PageSignature(
    val packageName: String,
    val activityName: String,
    val contentFingerprint: String,
    val addedAt: Long = System.currentTimeMillis()
) {
    /** 唯一标识：包名:Activity，Activity 为空时仅包名 */
    fun key(): String = if (activityName.isBlank()) packageName else "$packageName:$activityName"

    fun toJson(): JSONObject = JSONObject().apply {
        put("packageName", packageName)
        put("activityName", activityName)
        put("contentFingerprint", contentFingerprint)
        put("addedAt", addedAt)
    }

    companion object {
        fun fromJson(obj: JSONObject): PageSignature = PageSignature(
            packageName = obj.optString("packageName", ""),
            activityName = obj.optString("activityName", ""),
            contentFingerprint = obj.optString("contentFingerprint", ""),
            addedAt = obj.optLong("addedAt", System.currentTimeMillis())
        )
    }
}
