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

import org.ezbook.server.Server
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
    private suspend fun setString(key: String, value: String) {
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

    /** AI功能总开关 */
    suspend fun featureAiAvailable(): Boolean =
        getBoolean(Setting.FEATURE_AI_AVAILABLE, DefaultData.FEATURE_AI_AVAILABLE)

    /** 设置：AI功能总开关 */
    suspend fun setFeatureAiAvailable(value: Boolean) =
        setBoolean(Setting.FEATURE_AI_AVAILABLE, value)

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

    /** 禁用规则参与匹配开关 - 命中禁用规则时跳过AI识别 */
    suspend fun ruleMatchIncludeDisabled(): Boolean =
        getBoolean(Setting.RULE_MATCH_INCLUDE_DISABLED, DefaultData.RULE_MATCH_INCLUDE_DISABLED)

    /** 设置：禁用规则参与匹配开关 */
    suspend fun setRuleMatchIncludeDisabled(value: Boolean) =
        setBoolean(Setting.RULE_MATCH_INCLUDE_DISABLED, value)

    /** AI 总结自定义 Prompt */
    suspend fun aiSummaryPrompt(): String =
        getString(Setting.AI_SUMMARY_PROMPT, DefaultData.AI_SUMMARY_PROMPT)

    /** 设置：AI 总结自定义 Prompt */
    suspend fun setAiSummaryPrompt(value: String) =
        setString(Setting.AI_SUMMARY_PROMPT, value)

    /** AI账单识别提示词 */
    suspend fun aiBillRecognitionPrompt(): String =
        getString(Setting.AI_BILL_RECOGNITION_PROMPT, DefaultData.AI_BILL_RECOGNITION_PROMPT)

    /** 设置：AI账单识别提示词 */
    suspend fun setAiBillRecognitionPrompt(value: String) =
        setString(Setting.AI_BILL_RECOGNITION_PROMPT, value)

    /** AI资产映射提示词 */
    suspend fun aiAssetMappingPrompt(): String =
        getString(Setting.AI_ASSET_MAPPING_PROMPT, DefaultData.AI_ASSET_MAPPING_PROMPT)

    /** 设置：AI资产映射提示词 */
    suspend fun setAiAssetMappingPrompt(value: String) =
        setString(Setting.AI_ASSET_MAPPING_PROMPT, value)

    /** AI分类识别提示词 */
    suspend fun aiCategoryRecognitionPrompt(): String =
        getString(
            Setting.AI_CATEGORY_RECOGNITION_PROMPT,
            DefaultData.AI_CATEGORY_RECOGNITION_PROMPT
        )

    /** 设置：AI分类识别提示词 */
    suspend fun setAiCategoryRecognitionPrompt(value: String) =
        setString(Setting.AI_CATEGORY_RECOGNITION_PROMPT, value)

    // —— AI 接入参数 ——

    /** 后端统一 API Key */
    suspend fun apiKey(value: String): String = getString(Setting.API_KEY, value)

    /** 设置：后端统一 API Key */
    suspend fun setApiKey(value: String) = setString(Setting.API_KEY, value)

    /** 直连 API 地址 */
    suspend fun apiUri(uri: String): String = getString(Setting.API_URI, uri)

    /** 设置：直连 API 地址 */
    suspend fun setApiUri(value: String) = setString(Setting.API_URI, value)

    /** 直连 API 模型 */
    suspend fun apiModel(model: String): String = getString(Setting.API_MODEL, model)

    /** 设置：直连 API 模型 */
    suspend fun setApiModel(value: String) = setString(Setting.API_MODEL, value)

    /** 直连 API 提供商 */
    suspend fun apiProvider(): String = getString(Setting.API_PROVIDER, DefaultData.API_PROVIDER)

    /** 设置：直连 API 提供商 */
    suspend fun setApiProvider(value: String) = setString(Setting.API_PROVIDER, value)


    /** 调试开关（Server.logD 用） */
    suspend fun debugMode(): Boolean = getBoolean(Setting.DEBUG_MODE, false)

    /** 注入的通用 JS（RuleGenerator 用） */
    suspend fun jsCommon(): String = getString(Setting.JS_COMMON, DefaultData.JS_COMMON)

    /** 注入的分类 JS（RuleGenerator 用） */
    suspend fun jsCategory(): String = getString(Setting.JS_CATEGORY, DefaultData.JS_CATEGORY)

    /** 横屏勿扰（BillService 用） */
    suspend fun landscapeDnd(): Boolean =
        getBoolean(Setting.LANDSCAPE_DND, DefaultData.LANDSCAPE_DND)

    /** 自动去重（BillManager 用） */
    suspend fun autoGroup(): Boolean = getBoolean(Setting.AUTO_GROUP, DefaultData.AUTO_GROUP)

    /** 自动去重时间阈值（秒） */
    suspend fun autoGroupTimeThreshold(): Int =
        getInt(Setting.AUTO_GROUP_TIME_THRESHOLD, DefaultData.AUTO_GROUP_TIME_THRESHOLD)

    /** 自动识别转账账单 */
    suspend fun autoTransferRecognition(): Boolean =
        getBoolean(Setting.AUTO_TRANSFER_RECOGNITION, DefaultData.AUTO_TRANSFER_RECOGNITION)

    /** 转账账单合并时间阈值（秒） */
    suspend fun autoTransferTimeThreshold(): Int =
        getInt(Setting.AUTO_TRANSFER_TIME_THRESHOLD, DefaultData.AUTO_TRANSFER_TIME_THRESHOLD)

    /** 功能：资产管理（AssetsMap 用） */
    suspend fun featureAssetManager(): Boolean =
        getBoolean(Setting.SETTING_ASSET_MANAGER, DefaultData.SETTING_ASSET_MANAGER)


    /**
     * 备注格式模板
     *
     * 允许设置为空字符串以关闭备注生成。
     * - 返回逻辑：数据库为 null 时返回默认值；否则如实返回（包括空串）
     */
    suspend fun noteFormat(): String {
        val raw = getRawOrNull(Setting.NOTE_FORMAT)
        return raw ?: DefaultData.NOTE_FORMAT
    }


    suspend fun bookName(): String =
        getString(Setting.DEFAULT_BOOK_NAME, DefaultData.DEFAULT_BOOK_NAME)

    suspend fun autoAssetMap(): Boolean =
        getBoolean(Setting.AUTO_ASSET_MAPPING, DefaultData.AUTO_ASSET_MAPPING)

    /** 本位币（默认 CNY） */
    suspend fun baseCurrency(): String =
        getString(Setting.SETTING_BASE_CURRENCY, DefaultData.SETTING_BASE_CURRENCY)

    /** 多币种开关 */
    suspend fun featureMultiCurrency(): Boolean =
        getBoolean(Setting.SETTING_CURRENCY_MANAGER, DefaultData.SETTING_CURRENCY_MANAGER)

    suspend fun ruleVersion(): String =
        getString(Setting.RULE_VERSION, DefaultData.RULE_VERSION)
}