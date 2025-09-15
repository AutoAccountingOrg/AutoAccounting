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

package net.ankio.auto.xposed.core.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.logger.Logger
import net.ankio.auto.xposed.core.utils.DataUtils.get
import net.ankio.auto.xposed.core.utils.DataUtils.set
import net.ankio.auto.xposed.core.utils.MessageUtils.toast
import net.ankio.dex.Dex
import net.ankio.dex.model.Clazz
import net.ankio.dex.result.ClazzResult
import org.ezbook.server.tools.runCatchingExceptCancel

/**
 * AdaptationUtils
 * 负责自动适配逻辑。
 */
object AdaptationUtils {

    /**
     * 缓存键：已适配版本号（字符串存储，内容为 Long）
     */
    private const val KEY_ADAPT_VERSION = "adaptation_version"

    /**
     * 缓存键：规则哈希（字符串）
     */
    private const val KEY_RULES_HASH = "adaptation_rules_hash"

    /**
     * 缓存键：已适配类映射的 JSON
     */
    private const val KEY_CLAZZ_JSON = "clazz"

    /**
     * Gson 反序列化所需的目标类型
     */
    private val clazzMapType = object : TypeToken<HashMap<String, ClazzResult>>() {}.type

    fun autoAdaption(manifest: HookerManifest): Boolean {
        if (!VersionUtils.check(manifest)) {
            toast("${manifest.appName}版本过低，无法适配，请升级到最新版本后再试。")
            return false
        }
        if (manifest.rules.isEmpty()) return true

        val currentRulesHash = computeRulesHash(manifest.rules)

        // 版本号使用 Long，与 AppRuntime.version() 对齐
        val (code, _) = VersionUtils.version()

        val savedVersion = get(KEY_ADAPT_VERSION, "").toLongOrNull() ?: 0L
        val savedRulesHash = get(KEY_RULES_HASH, "")
        manifest.d("适配版本: $savedVersion, 规则哈希: $savedRulesHash")

        if (savedVersion == code && savedRulesHash == currentRulesHash) {
            runCatching {
                manifest.clazz = Gson().fromJson(
                    get(KEY_CLAZZ_JSON, ""),
                    clazzMapType
                )
                require(manifest.clazz.size == manifest.rules.size) {
                    "适配失败: 缓存大小不匹配"
                }
                manifest.i("从缓存加载适配信息: ${manifest.clazz}")
                return true
            }.onFailure { e ->
                clearCache()
                manifest.e(e)
            }
        }

        startAdaptationAsync(manifest, manifest.rules, currentRulesHash, code)
        return false
    }

    private fun startAdaptationAsync(
        manifest: HookerManifest,
        rules: MutableList<Clazz>,
        currentRulesHash: String,
        code: Long
    ) {
        CoroutineUtils.withIO {
            runCatchingExceptCancel {
                toast("自动记账开始适配中...")

                val appInfo = AppRuntime.application!!.applicationInfo
                val path = appInfo.sourceDir
                manifest.d("应用包路径: $path")

                val hashMap = Dex.findClazz(
                    path,
                    AppRuntime.application!!.classLoader,
                    rules
                ) { found, total, ruleName ->
                    toast("适配进度 $found/$total: 找到 $ruleName")
                    manifest.d("适配进度: $found/$total, 找到规则: $ruleName")
                }
                manifest.i("hashMap.size( ${hashMap.size} ) == rules.size( ${rules.size} )")
                if (hashMap.size == rules.size) {
                    saveCache(code, currentRulesHash, hashMap)
                    manifest.clazz = hashMap
                    manifest.i("适配成功: $hashMap")
                    toast("适配成功，即将重启应用...")
                    delay(2000L)
                    AppRuntime.restart()
                } else {
                    manifest.i("适配失败: $hashMap")
                    rules.forEachIndexed { index, rule ->
                        if (!hashMap.containsKey(rule.name)) manifest.i("未能适配规则: ${rule.name}")
                    }

                    set(KEY_ADAPT_VERSION, "0")
                    toast("适配失败，请检查应用版本是否支持")
                }
            }.onFailure { e ->
                manifest.e(e)
                toast("适配过程发生错误: ${e.message}")
            }
        }
    }

    /**
     * 计算规则哈希，规则名 + 方法签名，保证同一规则集生成一致的哈希
     */
    private fun computeRulesHash(rules: List<Clazz>): String {
        val fingerprint = rules.joinToString(",") { clazz ->
            "${clazz.name}:${clazz.methods.joinToString("|") { it.toString() }}"
        }
        return fingerprint.hashCode().toString()
    }

    /**
     * 清空本地适配缓存
     */
    fun clearCache() {
        set(KEY_ADAPT_VERSION, "0")
        set(KEY_RULES_HASH, "")
        set(KEY_CLAZZ_JSON, "")
    }

    /**
     * 持久化适配缓存
     */
    private fun saveCache(versionCode: Long, rulesHash: String, map: HashMap<String, ClazzResult>) {
        set(KEY_ADAPT_VERSION, versionCode.toString())
        set(KEY_RULES_HASH, rulesHash)
        set(KEY_CLAZZ_JSON, Gson().toJson(map))
    }
}


