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

import net.ankio.auto.utils.PrefManager
import org.json.JSONArray

/**
 * 页面特征管理器
 *
 * 负责：存储、匹配、内容指纹生成。
 * 匹配逻辑：包名 + activity（空=任意）+ 指纹（空=不校验，否则相似度阈值）
 */
object PageSignatureManager {

    private const val FINGERPRINT_MAX_LEN = 200
    private const val FINGERPRINT_SIMILARITY_THRESHOLD = 0.6

    /**
     * 获取所有已记住的页面签名
     */
    fun getAll(): List<PageSignature> {
        val raw = PrefManager.pageSignatures
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { PageSignature.fromJson(it) }
            }
        }.getOrElse { emptyList() }
    }

    /**
     * 添加页面签名
     */
    fun add(sig: PageSignature) {
        val list = getAll().toMutableList()
        list.removeAll { it.key() == sig.key() }
        list.add(sig)
        save(list)
    }

    /**
     * 移除指定 key 的签名
     */
    fun remove(key: String) {
        val list = getAll().filter { it.key() != key }.toMutableList()
        save(list)
    }

    /**
     * 检查当前页面是否在已记住列表中
     *
     * @param packageName 包名
     * @param activityName Activity 类名（可为空）
     * @param contentFingerprint 当前页面内容指纹（可选，用于 activity 为空时的模糊匹配）
     */
    /**
     * 匹配条件：1 包名 2 activity（空=任意） 3 指纹（空=不校验，否则相似度阈值）
     */
    fun matches(
        packageName: String,
        activityName: String,
        contentFingerprint: String? = null
    ): Boolean = getAll().any { sig ->
        sig.packageName == packageName &&
                (sig.activityName.isBlank() || sig.activityName == activityName) &&
                (contentFingerprint.isNullOrBlank() ||
                        fingerprintSimilarity(
                            sig.contentFingerprint,
                            contentFingerprint
                        ) >= FINGERPRINT_SIMILARITY_THRESHOLD)
    }

    /**
     * 生成内容指纹：移除可变信息后归一化，相同布局得相同指纹
     * - 千分位逗号移除（1,234.56 → 1234.56）
     * - 数字替换为 #
     */
    fun generateFingerprint(ocrText: String): String {
        if (ocrText.isBlank()) return ""
        return ocrText
            .replace(Regex("(?<=\\d),(?=\\d)"), "")
            .replace(Regex("\\d+(\\.\\d+)?"), "#")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(FINGERPRINT_MAX_LEN)
    }

    /**
     * 简单相似度：公共子串比例
     */
    private fun fingerprintSimilarity(a: String, b: String): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val shorter = if (a.length < b.length) a else b
        val longer = if (a.length >= b.length) a else b
        var maxMatch = 0
        for (i in 0..(longer.length - shorter.length)) {
            var match = 0
            for (j in shorter.indices) {
                if (longer[i + j] == shorter[j]) match++
            }
            if (match > maxMatch) maxMatch = match
        }
        return maxMatch.toDouble() / shorter.length
    }

    private fun save(list: List<PageSignature>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        PrefManager.pageSignatures = arr.toString()
    }
}
