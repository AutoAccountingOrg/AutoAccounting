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

package org.ezbook.server.tools

import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.Db

/**
 * 服务器端设置访问工具
 *
 * 设计目标：
 * - 使用数据库持久化配置（Room，参见 SettingDao）
 * - 提供类型安全、可容错的 suspend 读写接口（避免阻塞线程）
 * - 风格靠近客户端的 PrefManager，但避免 runBlocking
 *
 * 用法示例：
 * ```kotlin
 * val enabled = SettingUtils.aiBillRecognition()             // 读取布尔配置
 * SettingUtils.setAiBillRecognition(true)                    // 写入布尔配置
 * val apiKey = SettingUtils.apiKey()                         // 读取字符串配置
 * SettingUtils.setApiKey("sk-xxx")                           // 写入字符串配置
 * ```
 */
object SettingUtils {

    // ===================== 底层原始访问（字符串） =====================

    /**
     * 从数据库查询配置的原始字符串值。
     *
     * - 失败（异常/空）时返回 null，不抛异常
     */
    private suspend fun getRaw(key: String): String? {
        return runCatching { Db.get().settingDao().query(key)?.value }.getOrNull()
    }

    /**
     * 暴露只读的原始值获取（供路由或通用场景使用）。
     */
    suspend fun getRawOrNull(key: String): String? = getRaw(key)

    /**
     * 将字符串值写入数据库。
     *
     * - 失败时吞掉异常，保证调用方简单可靠
     */
    private suspend fun setRaw(key: String, value: String) {
        runCatching { Db.get().settingDao().set(key, value) }
    }

    // ===================== 类型化读写（通用） =====================

    /** 读取字符串配置；空或缺失时返回默认值 */
    suspend fun getString(key: String, defaultValue: String): String {
        val raw = getRaw(key)
        return if (raw.isNullOrEmpty()) defaultValue else raw
    }

    /** 写入字符串配置 */
    suspend fun setString(key: String, value: String) {
        setRaw(key, value)
    }

    /** 读取布尔配置；支持 true/false, 1/0, yes/no, on/off */
    suspend fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val raw = getRaw(key) ?: return defaultValue
        return when (raw.trim().lowercase()) {
            "true", "1", "yes", "y", "on" -> true
            "false", "0", "no", "n", "off" -> false
            else -> defaultValue
        }
    }

    /** 写入布尔配置 */
    suspend fun setBoolean(key: String, value: Boolean) {
        setRaw(key, value.toString())
    }

    /** 读取整型配置 */
    suspend fun getInt(key: String, defaultValue: Int): Int {
        val raw = getRaw(key) ?: return defaultValue
        return raw.toIntOrNull() ?: defaultValue
    }

    /** 写入整型配置 */
    suspend fun setInt(key: String, value: Int) {
        setRaw(key, value.toString())
    }

    /** 读取长整型配置 */
    suspend fun getLong(key: String, defaultValue: Long): Long {
        val raw = getRaw(key) ?: return defaultValue
        return raw.toLongOrNull() ?: defaultValue
    }

    /** 写入长整型配置 */
    suspend fun setLong(key: String, value: Long) {
        setRaw(key, value.toString())
    }

    // ===================== 常用配置（便捷封装） =====================
    // —— AI 功能开关 ——

    /** 使用AI识别账单 */
    suspend fun aiBillRecognition(): Boolean =
        getBoolean(Setting.AI_BILL_RECOGNITION, DefaultData.AI_BILL_RECOGNITION)

    /** 设置：使用AI识别账单 */
    suspend fun setAiBillRecognition(value: Boolean) =
        setBoolean(Setting.AI_BILL_RECOGNITION, value)

    /** 使用AI识别分类 */
    suspend fun aiCategoryRecognition(): Boolean =
        getBoolean(Setting.AI_CATEGORY_RECOGNITION, DefaultData.AI_CATEGORY_RECOGNITION)

    /** 设置：使用AI识别分类 */
    suspend fun setAiCategoryRecognition(value: Boolean) =
        setBoolean(Setting.AI_CATEGORY_RECOGNITION, value)

    /** 使用AI进行资产映射 */
    suspend fun aiAssetMapping(): Boolean =
        getBoolean(Setting.AI_ASSET_MAPPING, DefaultData.AI_ASSET_MAPPING)

    /** 设置：使用AI进行资产映射 */
    suspend fun setAiAssetMapping(value: Boolean) =
        setBoolean(Setting.AI_ASSET_MAPPING, value)

    /** 使用AI进行账单总结（月度） */
    suspend fun aiMonthlySummary(): Boolean =
        getBoolean(Setting.AI_MONTHLY_SUMMARY, DefaultData.AI_MONTHLY_SUMMARY)

    /** 设置：使用AI进行账单总结（月度） */
    suspend fun setAiMonthlySummary(value: Boolean) =
        setBoolean(Setting.AI_MONTHLY_SUMMARY, value)

    /** AI 总结自定义 Prompt */
    suspend fun aiSummaryPrompt(): String =
        getString(Setting.AI_SUMMARY_PROMPT, DefaultData.AI_SUMMARY_PROMPT)

    /** 设置：AI 总结自定义 Prompt */
    suspend fun setAiSummaryPrompt(value: String) =
        setString(Setting.AI_SUMMARY_PROMPT, value)

    // —— AI 接入参数 ——

    /** 后端统一 API Key */
    suspend fun apiKey(): String = getString(Setting.API_KEY, DefaultData.API_KEY)

    /** 设置：后端统一 API Key */
    suspend fun setApiKey(value: String) = setString(Setting.API_KEY, value)

    /** One API 网关地址 */
    suspend fun aiOneApiUri(): String =
        getString(Setting.AI_ONE_API_URI, DefaultData.AI_ONE_API_URI)

    /** 设置：One API 网关地址 */
    suspend fun setAiOneApiUri(value: String) = setString(Setting.AI_ONE_API_URI, value)

    /** One API 模型 */
    suspend fun aiOneApiModel(): String =
        getString(Setting.AI_ONE_API_MODEL, DefaultData.AI_ONE_API_MODEL)

    /** 设置：One API 模型 */
    suspend fun setAiOneApiModel(value: String) = setString(Setting.AI_ONE_API_MODEL, value)

    /** 直连 API 地址 */
    suspend fun apiUri(): String = getString(Setting.API_URI, DefaultData.API_URI)

    /** 设置：直连 API 地址 */
    suspend fun setApiUri(value: String) = setString(Setting.API_URI, value)

    /** 直连 API 模型 */
    suspend fun apiModel(): String = getString(Setting.API_MODEL, DefaultData.API_MODEL)

    /** 设置：直连 API 模型 */
    suspend fun setApiModel(value: String) = setString(Setting.API_MODEL, value)

    // —— Provider 作用域（BaseAIProvider 用） ——

    /** Provider 作用域 API Key（不存在返回传入默认值） */
    suspend fun providerApiKey(name: String, defaultValue: String): String =
        getString("${Setting.API_KEY}_$name", defaultValue)

    /** Provider 作用域 API URI（不存在返回传入默认值） */
    suspend fun providerApiUri(name: String, defaultValue: String): String =
        getString("${Setting.API_URI}_$name", defaultValue)

    /** Provider 作用域 Model（不存在返回传入默认值） */
    suspend fun providerApiModel(name: String, defaultValue: String): String =
        getString("${Setting.API_MODEL}_$name", defaultValue)

    // —— 其他服务端直接使用到的配置 ——

    /** 当前 AI Provider 名称（AiManager 用） */
    suspend fun aiModel(): String = getString(Setting.AI_MODEL, DefaultData.AI_MODEL)

    /** 调试开关（Server.logD 用） */
    suspend fun debugMode(): Boolean = getBoolean(Setting.DEBUG_MODE, DefaultData.DEBUG_MODE)

    /** 注入的通用 JS（RuleGenerator 用） */
    suspend fun jsCommon(): String = getString(Setting.JS_COMMON, DefaultData.JS_COMMON)

    /** 注入的分类 JS（RuleGenerator 用） */
    suspend fun jsCategory(): String = getString(Setting.JS_CATEGORY, DefaultData.JS_CATEGORY)

    /** 横屏勿扰（BillService 用） */
    suspend fun landscapeDnd(): Boolean =
        getBoolean(Setting.LANDSCAPE_DND, DefaultData.LANDSCAPE_DND)

    /** 自动去重（BillManager 用） */
    suspend fun autoGroup(): Boolean = getBoolean(Setting.AUTO_GROUP, DefaultData.AUTO_GROUP)

    /** 功能：资产管理（AssetsMap 用） */
    suspend fun featureAssetManager(): Boolean =
        getBoolean(Setting.SETTING_ASSET_MANAGER, DefaultData.SETTING_ASSET_MANAGER)
}