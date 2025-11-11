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

package net.ankio.auto.utils

import android.content.ComponentName
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatDelegate
import net.ankio.auto.autoApp
import net.ankio.auto.constant.WorkMode
import androidx.core.content.edit
import net.ankio.auto.App
import net.ankio.auto.BuildConfig
import net.ankio.auto.http.api.SettingAPI
import net.ankio.auto.xposed.XposedModule
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting

/**
 * 偏好设置管理器
 * 统一管理应用所有配置项，使用 Setting.kt 定义的 key 和 DefaultData.kt 定义的默认值
 * 提供类型安全的读写接口，并支持与后端同步
 */
object PrefManager {

    // ======== 类型代理：统一封装 SharedPreferences 读写 ========
    // 使用 runCatching 提供容错机制，避免类型转换异常导致崩溃
    private fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        runCatching { pref.getBoolean(key, defaultValue) }.getOrDefault(defaultValue)

    private fun putBoolean(key: String, value: Boolean) {
        pref.edit { putBoolean(key, value) }
        // 异步同步到服务端
        App.launch { SettingAPI.set(key, value.toString()) }
    }

    private fun getInt(key: String, defaultValue: Int): Int =
        runCatching { pref.getInt(key, defaultValue) }.getOrDefault(defaultValue)

    private fun putInt(key: String, value: Int) {
        pref.edit { putInt(key, value) }
        // 异步同步到服务端
        App.launch { SettingAPI.set(key, value.toString()) }
    }

    private fun getLong(key: String, defaultValue: Long): Long =
        runCatching { pref.getLong(key, defaultValue) }.getOrDefault(defaultValue)

    private fun putLong(key: String, value: Long) {
        pref.edit { putLong(key, value) }
        // 异步同步到服务端
        App.launch { SettingAPI.set(key, value.toString()) }
    }

    private fun getString(key: String, defaultValue: String): String =
        runCatching { pref.getString(key, defaultValue) ?: defaultValue }.getOrDefault(defaultValue)

    private fun putString(key: String, value: String) {
        pref.edit { putString(key, value) }
        // 异步同步到服务端
        App.launch { SettingAPI.set(key, value) }
    }

    /** SharedPreferences 实例 - 存储所有配置项 */
    private val pref = autoApp.getSharedPreferences("settings", MODE_PRIVATE)

    // ======== AI 设置 ========

    /**
     * 旧版 Provider 名称（兼容用）
     */
    var apiProvider: String
        get() = getString(Setting.API_PROVIDER, DefaultData.API_PROVIDER)
        set(value) = putString(Setting.API_PROVIDER, value)


    /**
     * 统一 API Key（与服务端 SettingUtils.apiKey 对齐）
     */
    var apiKey: String
        get() = getString(Setting.API_KEY, DefaultData.API_KEY)
        set(value) = putString(Setting.API_KEY, value)


    /**
     * 直连 API 地址（与服务端 SettingUtils.apiUri 对齐）
     */
    var apiUri: String
        get() = getString(Setting.API_URI, DefaultData.API_URI)
        set(value) = putString(Setting.API_URI, value)

    /**
     * 直连模型（与服务端 SettingUtils.apiModel 对齐）
     */
    var apiModel: String
        get() = getString(Setting.API_MODEL, DefaultData.API_MODEL)
        set(value) = putString(Setting.API_MODEL, value)


    /** 使用AI识别账单 - 从原始数据中提取账单信息 */
    var aiBillRecognition: Boolean
        get() = getBoolean(Setting.AI_BILL_RECOGNITION, DefaultData.AI_BILL_RECOGNITION)
        set(value) = putBoolean(Setting.AI_BILL_RECOGNITION, value)

    /** 使用AI识别分类 - 自动分类账单 */
    var aiCategoryRecognition: Boolean
        get() = getBoolean(Setting.AI_CATEGORY_RECOGNITION, DefaultData.AI_CATEGORY_RECOGNITION)
        set(value) = putBoolean(Setting.AI_CATEGORY_RECOGNITION, value)

    /** 使用AI进行资产映射 - 将账单映射到对应资产账户 */
    var aiAssetMapping: Boolean
        get() = getBoolean(Setting.AI_ASSET_MAPPING, DefaultData.AI_ASSET_MAPPING)
        set(value) = putBoolean(Setting.AI_ASSET_MAPPING, value)

    /** 使用AI进行账单总结（月度） - 生成月度财务总结 */
    var aiMonthlySummary: Boolean
        get() = getBoolean(Setting.AI_MONTHLY_SUMMARY, DefaultData.AI_MONTHLY_SUMMARY)
        set(value) = putBoolean(Setting.AI_MONTHLY_SUMMARY, value)

    /** AI 总结自定义Prompt - 用户自定义的总结提示词 */
    var aiSummaryPrompt: String
        get() = getString(Setting.AI_SUMMARY_PROMPT, DefaultData.AI_SUMMARY_PROMPT)
        set(value) = putString(Setting.AI_SUMMARY_PROMPT, value)

    /** AI账单识别提示词 - 用户自定义的账单识别提示词 */
    var aiBillRecognitionPrompt: String
        get() = getString(
            Setting.AI_BILL_RECOGNITION_PROMPT,
            DefaultData.AI_BILL_RECOGNITION_PROMPT
        )
        set(value) = putString(Setting.AI_BILL_RECOGNITION_PROMPT, value)

    /** AI资产映射提示词 - 用户自定义的资产映射提示词 */
    var aiAssetMappingPrompt: String
        get() = getString(Setting.AI_ASSET_MAPPING_PROMPT, DefaultData.AI_ASSET_MAPPING_PROMPT)
        set(value) = putString(Setting.AI_ASSET_MAPPING_PROMPT, value)

    /** AI分类识别提示词 - 用户自定义的分类识别提示词 */
    var aiCategoryRecognitionPrompt: String
        get() = getString(
            Setting.AI_CATEGORY_RECOGNITION_PROMPT,
            DefaultData.AI_CATEGORY_RECOGNITION_PROMPT
        )
        set(value) = putString(Setting.AI_CATEGORY_RECOGNITION_PROMPT, value)


    // ======== 自动记账设置 ========

    /** 自动记录账单开关 - 全局自动记账功能总开关 */
    var autoRecordBill: Boolean
        get() = getBoolean(Setting.AUTO_RECORD_BILL, DefaultData.AUTO_RECORD_BILL)
        set(value) = putBoolean(Setting.AUTO_RECORD_BILL, value)

    /** 自动资产映射开关 - 自动将账单映射到对应资产账户（非AI版本） */
    var autoAssetMapping: Boolean
        get() = getBoolean(Setting.AUTO_ASSET_MAPPING, DefaultData.AUTO_ASSET_MAPPING)
        set(value) = putBoolean(Setting.AUTO_ASSET_MAPPING, value)


    /** 记账软件包名 - 目标记账应用的包名 */
    var bookApp: String
        get() = getString(Setting.BOOK_APP_ID, DefaultData.BOOK_APP)
        set(value) = putString(Setting.BOOK_APP_ID, value)

    /** 自动记账提示开关 - 是否显示记账成功提示 */
    var showAutoBillTip: Boolean
        get() = getBoolean(Setting.SHOW_AUTO_BILL_TIP, DefaultData.SHOW_AUTO_BILL_TIP)
        set(value) = putBoolean(Setting.SHOW_AUTO_BILL_TIP, value)

    /** 记账提醒开关 - 是否启用记账提醒功能 */
    var settingRemindBook: Boolean
        get() = getBoolean(Setting.SETTING_REMIND_BOOK, DefaultData.SETTING_REMIND_BOOK)
        set(value) = putBoolean(Setting.SETTING_REMIND_BOOK, value)

    /** 记住分类开关 - 手动选择分类时记住该选择，用于相似账单 */
    var rememberCategory: Boolean
        get() = getBoolean(Setting.AUTO_CREATE_CATEGORY, DefaultData.AUTO_CREATE_CATEGORY)
        set(value) = putBoolean(Setting.AUTO_CREATE_CATEGORY, value)

    /** 自动去重（去重）开关 - 是否合并相似交易记录 */
    var autoGroup: Boolean
        get() = getBoolean(Setting.AUTO_GROUP, DefaultData.AUTO_GROUP)
        set(value) = putBoolean(Setting.AUTO_GROUP, value)

    /** 监听应用白名单 - 允许自动记账的应用包名列表（CSV 格式存储） */
    var appWhiteList: MutableList<String>
        get() = getString(Setting.LISTENER_APP_LIST, DefaultData.APP_FILTER)
            .split(",").filter { it.isNotEmpty() }.toMutableList()
        set(value) = putString(Setting.LISTENER_APP_LIST, value.joinToString(","))

    /** 主动模式开关 - 是否启用主动引导用户记账 */
    var featureLeading: Boolean
        get() = getBoolean(Setting.PROACTIVELY_MODEL, DefaultData.PROACTIVELY_MODEL)
        set(value) = putBoolean(Setting.PROACTIVELY_MODEL, value)

    /** 数据过滤关键字 - 用于筛选交易信息的关键词列表（CSV 格式存储） */
    var dataFilter: MutableList<String>
        get() = getString(Setting.DATA_FILTER, DefaultData.DATA_FILTER)
            .split(",").filter { it.isNotEmpty() }.toMutableList()
        set(value) = putString(Setting.DATA_FILTER, value.joinToString(","))

    // ======== 权限设置 ========

    /** 短信过滤规则 - 用于过滤短信内容的正则表达式或关键词 */
    var smsFilter: String
        get() = getString(Setting.SMS_FILTER, DefaultData.SMS_FILTER)
        set(value) = putString(Setting.SMS_FILTER, value)

    /** 横屏勿扰模式开关 - 横屏时是否暂停自动记账 */
    var landscapeDnd: Boolean
        get() = getBoolean(Setting.LANDSCAPE_DND, DefaultData.LANDSCAPE_DND)
        set(value) = putBoolean(Setting.LANDSCAPE_DND, value)

    // ======== 同步和备份设置 ========

    /** 同步类型 - 数据同步方式（如 "webdav", "local", "none"） */
    var syncType: String
        get() = getString(Setting.SYNC_TYPE, DefaultData.SYNC_TYPE)
        set(value) = putString(Setting.SYNC_TYPE, value)

    /** 最后同步时间 - Unix 时间戳 */
    var lastSyncTime: Long
        get() = getLong(Setting.LAST_SYNC_TIME, DefaultData.LAST_SYNC_TIME)
        set(value) = putLong(Setting.LAST_SYNC_TIME, value)

    /** 自动备份开关 - 是否定期自动备份数据 */
    var autoBackup: Boolean
        get() = getBoolean(Setting.AUTO_BACKUP, DefaultData.AUTO_BACKUP)
        set(value) = putBoolean(Setting.AUTO_BACKUP, value)

    /** 手动同步模式 - 开启后保存账单不主动调用同步 */
    var manualSync: Boolean
        get() = getBoolean(Setting.MANUAL_SYNC, DefaultData.MANUAL_SYNC)
        set(value) = putBoolean(Setting.MANUAL_SYNC, value)

    /** 最后备份时间 - Unix 时间戳 */
    var lastBackupTime: Long
        get() = getLong(Setting.LAST_BACKUP_TIME, DefaultData.LAST_BACKUP_TIME)
        set(value) = putLong(Setting.LAST_BACKUP_TIME, value)

    /** WebDAV 服务开关 - 是否启用 WebDAV 同步 */
    var useWebdav: Boolean
        get() = getBoolean(Setting.USE_WEBDAV, DefaultData.USE_WEBDAV)
        set(value) = putBoolean(Setting.USE_WEBDAV, value)

    /** WebDAV 服务器地址 */
    var webdavHost: String
        get() = getString(Setting.WEBDAV_HOST, DefaultData.WEBDAV_HOST)
        set(value) = putString(Setting.WEBDAV_HOST, value)

    /** WebDAV 用户名 */
    var webdavUser: String
        get() = getString(Setting.WEBDAV_USER, DefaultData.WEBDAV_USER)
        set(value) = putString(Setting.WEBDAV_USER, value)

    /** WebDAV 密码 */
    var webdavPassword: String
        get() = getString(Setting.WEBDAV_PASSWORD, DefaultData.WEBDAV_PASSWORD)
        set(value) = putString(Setting.WEBDAV_PASSWORD, value)

    /** WebDAV 路径 */
    var webdavPath: String
        get() = getString(Setting.WEBDAV_PATH, DefaultData.WEBDAV_PATH)
        set(value) = putString(Setting.WEBDAV_PATH, value)

    /** 本地备份路径 */
    var localBackupPath: String
        get() = getString(Setting.LOCAL_BACKUP_PATH, DefaultData.LOCAL_BACKUP_PATH)
        set(value) = putString(Setting.LOCAL_BACKUP_PATH, value)

    /** 备份保留数量 - 本地和WebDAV备份都保留的文件数量 */
    var backupKeepCount: Int
        get() = getInt(Setting.BACKUP_KEEP_COUNT, DefaultData.BACKUP_KEEP_COUNT)
        set(value) = putInt(Setting.BACKUP_KEEP_COUNT, value)

    // ======== 同步哈希值 ========

    /** 资产数据同步哈希值 - 用于检测数据变更 */
    var hashAsset: String
        get() = getString(Setting.HASH_ASSET, DefaultData.HASH_ASSET)
        set(value) = putString(Setting.HASH_ASSET, value)

    /** 账单数据同步哈希值 */
    var hashBill: String
        get() = getString(Setting.HASH_BILL, DefaultData.HASH_BILL)
        set(value) = putString(Setting.HASH_BILL, value)

    /** 账本数据同步哈希值 */
    var hashBook: String
        get() = getString(Setting.HASH_BOOK, DefaultData.HASH_BOOK)
        set(value) = putString(Setting.HASH_BOOK, value)

    /** 分类数据同步哈希值 */
    var hashCategory: String
        get() = getString(Setting.HASH_CATEGORY, DefaultData.HASH_CATEGORY)
        set(value) = putString(Setting.HASH_CATEGORY, value)

    /** 报销单数据同步哈希值 */
    var hashBaoxiaoBill: String
        get() = getString(Setting.HASH_BAOXIAO_BILL, DefaultData.HASH_BAOXIAO_BILL)
        set(value) = putString(Setting.HASH_BAOXIAO_BILL, value)

    // ======== UI 外观设置 ========

    /** 深色主题模式 - 取值遵循 AppCompatDelegate.MODE_NIGHT_* */
    var darkTheme: Int
        get() = getInt(Setting.UI_DARK_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) = putInt(Setting.UI_DARK_THEME_MODE, value)

    /** 纯黑暗色开关 - 深色模式下是否使用纯黑背景 */
    var blackDarkTheme: Boolean
        get() = getBoolean(Setting.UI_PURE_BLACK, DefaultData.UI_PURE_BLACK)
        set(value) = putBoolean(Setting.UI_PURE_BLACK, value)

    /** 跟随系统强调色开关 - 是否使用系统动态颜色 */
    var followSystemAccent: Boolean
        get() = getBoolean(Setting.UI_FOLLOW_SYSTEM_ACCENT, DefaultData.UI_FOLLOW_SYSTEM_ACCENT)
        set(value) = putBoolean(Setting.UI_FOLLOW_SYSTEM_ACCENT, value)

    /** 主题色标识 - 应用主题色名称（如 "MATERIAL_DEFAULT"） */
    var themeColor: String
        get() = getString(Setting.UI_THEME_COLOR, DefaultData.UI_THEME_COLOR)
        set(value) = putString(Setting.UI_THEME_COLOR, value)

    /** 圆角风格开关 - 是否使用圆角 UI 设计 */
    var uiRoundStyle: Boolean
        get() = getBoolean(Setting.USE_ROUND_STYLE, DefaultData.USE_ROUND_STYLE)
        set(value) = putBoolean(Setting.USE_ROUND_STYLE, value)

    /** 系统皮肤开关 - 是否跟随系统外观设置 */
    var useSystemSkin: Boolean
        get() = getBoolean(Setting.USE_SYSTEM_SKIN, DefaultData.USE_SYSTEM_SKIN)
        set(value) = putBoolean(Setting.USE_SYSTEM_SKIN, value)

    /** 显示规则名称开关 - 记账时是否显示匹配的规则名 */
    var showRuleName: Boolean
        get() = getBoolean(Setting.SHOW_RULE_NAME, DefaultData.SHOW_RULE_NAME)
        set(value) = putBoolean(Setting.SHOW_RULE_NAME, value)

    /** 成功提示弹窗开关 - 记账成功后是否显示提示 */
    var showSuccessPopup: Boolean
        get() = getBoolean(Setting.SHOW_SUCCESS_POPUP, DefaultData.SHOW_SUCCESS_POPUP)
        set(value) = putBoolean(Setting.SHOW_SUCCESS_POPUP, value)

    /** 重复提示弹窗开关 - 检测到重复记录时是否提示 */
    var showDuplicatedPopup: Boolean
        get() = getBoolean(Setting.SHOW_DUPLICATED_POPUP, DefaultData.SHOW_DUPLICATED_POPUP)
        set(value) = putBoolean(Setting.SHOW_DUPLICATED_POPUP, value)

    /** 删除账单二次确认 - 删除前是否弹出确认 */
    var confirmDeleteBill: Boolean
        get() = getBoolean(Setting.CONFIRM_DELETE_BILL, DefaultData.CONFIRM_DELETE_BILL)
        set(value) = putBoolean(Setting.CONFIRM_DELETE_BILL, value)

    /** 显示父分类开关 - 分类显示时是否包含父级分类 */
    var categoryShowParent: Boolean
        get() = getBoolean(Setting.CATEGORY_SHOW_PARENT, DefaultData.CATEGORY_SHOW_PARENT)
        set(value) = putBoolean(Setting.CATEGORY_SHOW_PARENT, value)

    /** 支出是否显示为红色 - true=支出红色/收入绿色，false=支出绿色/收入红色 */
    var isExpenseRed: Boolean
        get() = getBoolean(Setting.IS_EXPENSE_RED, DefaultData.IS_EXPENSE_RED)
        set(value) = putBoolean(Setting.IS_EXPENSE_RED, value)

    /** 收入是否显示向上箭头 - true=收入向上/支出向下，false=收入向下/支出向上 */
    var isIncomeUp: Boolean
        get() = getBoolean(Setting.IS_INCOME_UP, DefaultData.IS_INCOME_UP)
        set(value) = putBoolean(Setting.IS_INCOME_UP, value)

    /** 备注格式模板 - 自动记账时的备注格式（如 "【商户名称】【商品名称】"） */
    var noteFormat: String
        get() = getString(Setting.NOTE_FORMAT, DefaultData.NOTE_FORMAT)
        set(value) = putString(Setting.NOTE_FORMAT, value)

    /** 系统语言设置 - 应用语言（"SYSTEM" 或具体语言代码） */
    var language: String
        get() = getString(Setting.SYSTEM_LANGUAGE, DefaultData.SYSTEM_LANGUAGE)
        set(value) = putString(Setting.SYSTEM_LANGUAGE, value)

    // ======== 悬浮窗设置 ========

    /** 悬浮窗超时时间 - 自动关闭时间（秒，如 "10"） */
    var floatTimeoutOff: Int
        get() = getInt(Setting.FLOAT_TIMEOUT_OFF, DefaultData.FLOAT_TIMEOUT_OFF)
        set(value) = putInt(Setting.FLOAT_TIMEOUT_OFF, value)

    /** 悬浮窗超时操作 - 超时后的动作（如 "dismiss", "minimize"） */
    var floatTimeoutAction: String
        get() = getString(Setting.FLOAT_TIMEOUT_ACTION, DefaultData.FLOAT_TIMEOUT_ACTION)
        set(value) = putString(Setting.FLOAT_TIMEOUT_ACTION, value)

    /** 悬浮窗点击事件 - 单击悬浮窗的响应动作 */
    var floatClick: String
        get() = getString(Setting.FLOAT_CLICK, DefaultData.FLOAT_CLICK)
        set(value) = putString(Setting.FLOAT_CLICK, value)

    /** 悬浮窗长按事件 - 长按悬浮窗的响应动作 */
    var floatLongClick: String
        get() = getString(Setting.FLOAT_LONG_CLICK, DefaultData.FLOAT_LONG_CLICK)
        set(value) = putString(Setting.FLOAT_LONG_CLICK, value)

    /** 记账小面板显示位置 - left/right/top */
    var floatGravityPosition: String
        get() = getString(Setting.FLOAT_GRAVITY_POSITION, DefaultData.FLOAT_GRAVITY_POSITION)
        set(value) = putString(Setting.FLOAT_GRAVITY_POSITION, value)

    /** 提醒位置 - top/center/bottom */
    var toastPosition: String
        get() = getString(Setting.TOAST_POSITION, DefaultData.TOAST_POSITION)
        set(value) = putString(Setting.TOAST_POSITION, value)

    // ======== OCR 显示设置 ========
    /** 是否在OCR识别期间显示动画悬浮窗 */
    var ocrShowAnimation: Boolean
        get() = getBoolean(Setting.OCR_SHOW_ANIMATION, DefaultData.OCR_SHOW_ANIMATION)
        set(value) = putBoolean(Setting.OCR_SHOW_ANIMATION, value)

    /** 是否启用翻转手机触发当前页面识别（非Xposed模式） */
    var ocrFlipTrigger: Boolean
        get() = getBoolean(Setting.OCR_FLIP_TRIGGER, DefaultData.OCR_FLIP_TRIGGER)
        set(value) = putBoolean(Setting.OCR_FLIP_TRIGGER, value)

    // ======== 功能模块开关 ========

    /** 资产管理功能开关 - 是否启用资产管理模块 */
    var featureAssetManage: Boolean
        get() = getBoolean(Setting.SETTING_ASSET_MANAGER, DefaultData.SETTING_ASSET_MANAGER)
        set(value) = putBoolean(Setting.SETTING_ASSET_MANAGER, value)

    /** 多币种功能开关 - 是否支持多币种记账 */
    var featureMultiCurrency: Boolean
        get() = getBoolean(Setting.SETTING_CURRENCY_MANAGER, DefaultData.SETTING_CURRENCY_MANAGER)
        set(value) = putBoolean(Setting.SETTING_CURRENCY_MANAGER, value)

    /** 报销功能开关 - 是否启用报销记录功能 */
    var featureReimbursement: Boolean
        get() = getBoolean(Setting.SETTING_REIMBURSEMENT, DefaultData.SETTING_REIMBURSEMENT)
        set(value) = putBoolean(Setting.SETTING_REIMBURSEMENT, value)

    /** 债务功能开关 - 是否启用借贷记录功能 */
    var featureDebt: Boolean
        get() = getBoolean(Setting.SETTING_DEBT, DefaultData.SETTING_DEBT)
        set(value) = putBoolean(Setting.SETTING_DEBT, value)

    /** 多账本功能开关 - 是否支持多个账本管理 */
    var featureMultiBook: Boolean
        get() = getBoolean(Setting.SETTING_BOOK_MANAGER, DefaultData.SETTING_BOOK_MANAGER)
        set(value) = putBoolean(Setting.SETTING_BOOK_MANAGER, value)

    /** 手续费功能开关 - 是否启用手续费记录 */
    var featureFee: Boolean
        get() = getBoolean(Setting.SETTING_FEE, DefaultData.SETTING_FEE)
        set(value) = putBoolean(Setting.SETTING_FEE, value)

    /** 标签功能开关 - 是否启用交易标签功能 */
    var featureTag: Boolean
        get() = getBoolean(Setting.SETTING_TAG, DefaultData.SETTING_TAG)
        set(value) = putBoolean(Setting.SETTING_TAG, value)

    /** 忽略资产开关 - 记账时是否忽略资产变动 */
    var ignoreAsset: Boolean
        get() = getBoolean(Setting.IGNORE_ASSET, DefaultData.IGNORE_ASSET)
        set(value) = putBoolean(Setting.IGNORE_ASSET, value)

    /** 默认账本名称 - 新建记录时的默认账本 */
    var defaultBook: String
        get() = getString(Setting.DEFAULT_BOOK_NAME, DefaultData.DEFAULT_BOOK_NAME)
        set(value) = putString(Setting.DEFAULT_BOOK_NAME, value)

    // ======== 系统设置 ========

    /** 调试模式开关 - 是否启用调试功能和日志 */
    var debugMode: Boolean
        get() = getBoolean(Setting.DEBUG_MODE, BuildConfig.DEBUG)
        set(value) {
            putBoolean(Setting.DEBUG_MODE, value)
        }

    /** 错误报告开关 - 是否自动发送崩溃和错误报告 */
    var sendErrorReport: Boolean
        get() = getBoolean(Setting.SEND_ERROR_REPORT, DefaultData.SEND_ERROR_REPORT)
        set(value) = putBoolean(Setting.SEND_ERROR_REPORT, value)

    /** 工作模式（框架标识） - 当前使用的 Hook 框架类型 */
    var workMode: WorkMode
        get() = WorkMode.valueOf(getString(Setting.KEY_FRAMEWORK, DefaultData.KEY_FRAMEWORK))
        set(value) {
            // 持久化工作模式
            putString(Setting.KEY_FRAMEWORK, value.name)

            // 当切换到 Xposed / LSPatch 模式时，自动从白名单中移除已 Hook 的应用，避免重复监听
            if (value == WorkMode.Xposed || value == WorkMode.LSPatch) {
                val hookedPkgs = runCatching {
                    XposedModule.get().map { it.packageName }
                        .filter { pkg -> pkg.isNotBlank() && pkg != "android" && pkg != BuildConfig.APPLICATION_ID }
                        .toSet()
                }.getOrElse { emptySet() }

                if (hookedPkgs.isNotEmpty()) {
                    val current = appWhiteList
                    val filtered = current.filterNot { it in hookedPkgs }.toMutableList()
                    if (filtered.size != current.size) {
                        // 仅在有变更时写回，减少不必要同步
                        appWhiteList = filtered
                    }
                }
            }
        }

    /** 加载成功标记 - 模块是否成功加载 */
    var loadSuccess: Boolean
        get() = getBoolean(Setting.LOAD_SUCCESS, DefaultData.LOAD_SUCCESS)
        set(value) = putBoolean(Setting.LOAD_SUCCESS, value)

    /** 捐赠时间记录 - 用户捐赠的时间戳或标识 */
    var donateTime: String
        get() = getString(Setting.DONATE_TIME, DefaultData.DONATE_TIME)
        set(value) = putString(Setting.DONATE_TIME, value)

    /** 隐藏启动图标开关 - 是否在桌面隐藏应用图标 */
    var hideIcon: Boolean
        get() = getBoolean(Setting.HIDE_ICON, DefaultData.HIDE_ICON)
        set(value) {
            putBoolean(Setting.HIDE_ICON, value)
            // 获取启动器组件名称
            val component = ComponentName(autoApp, "com.close.hook.ads.MainActivityLauncher")
            // 根据设置值确定组件状态：隐藏时禁用，显示时启用
            val status =
                if (value) PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                else PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            // 动态切换组件启用状态，无需重启应用
            autoApp.packageManager.setComponentEnabledSetting(
                component,
                status,
                PackageManager.DONT_KILL_APP
            )
        }

    /** 引导页进度索引 - 用户完成的引导步骤 */
    var introIndex: Int
        get() = getInt(Setting.INTRO_INDEX, DefaultData.INTRO_INDEX)
        set(value) = putInt(Setting.INTRO_INDEX, value)

    /** 本地实例 ID - 设备唯一标识符 */
    var localID: String
        get() = getString(Setting.LOCAL_ID, DefaultData.LOCAL_ID)
        set(value) = putString(Setting.LOCAL_ID, value)

    /** 访问令牌 - 用于服务端认证的 Token */
    var token: String
        get() = getString(Setting.TOKEN, DefaultData.TOKEN)
        set(value) = putString(Setting.TOKEN, value)

    /** GitHub 连通性标记 - 是否能正常访问 GitHub 服务 */
    var githubConnectivity: Boolean
        get() = getBoolean(Setting.GITHUB_CONNECTIVITY, DefaultData.GITHUB_CONNECTIVITY)
        set(value) = putBoolean(Setting.GITHUB_CONNECTIVITY, value)

    // ======== 更新设置 ========

    /** 最后检查更新时间 - Unix 时间戳 */
    var lastUpdateCheckTime: Long
        get() = getLong(Setting.LAST_UPDATE_CHECK_TIME, DefaultData.LAST_UPDATE_CHECK_TIME)
        set(value) = putLong(Setting.LAST_UPDATE_CHECK_TIME, value)

    /** 更新渠道 - 应用更新来源（如 "stable", "beta"） */
    var appChannel: String
        get() = getString(Setting.UPDATE_CHANNEL, DefaultData.UPDATE_CHANNEL)
        set(value) = putString(Setting.UPDATE_CHANNEL, value)

    /** 更新类型设置 - 更新检查方式（如 "auto", "manual"） */
    var checkUpdateType: String
        get() = getString(Setting.CHECK_UPDATE_TYPE, DefaultData.CHECK_UPDATE_TYPE)
        set(value) = putString(Setting.CHECK_UPDATE_TYPE, value)

    /** 自动检查应用更新开关 */
    var autoCheckAppUpdate: Boolean
        get() = getBoolean(Setting.CHECK_APP_UPDATE, DefaultData.CHECK_APP_UPDATE)
        set(value) = putBoolean(Setting.CHECK_APP_UPDATE, value)

    /** 自动检查规则更新开关 */
    var autoCheckRuleUpdate: Boolean
        get() = getBoolean(Setting.CHECK_RULE_UPDATE, DefaultData.CHECK_RULE_UPDATE)
        set(value) = putBoolean(Setting.CHECK_RULE_UPDATE, value)

    /** 规则版本号 - 当前使用的记账规则版本 */
    var ruleVersion: String
        get() = getString(Setting.RULE_VERSION, DefaultData.RULE_VERSION)
        set(value) = putString(Setting.RULE_VERSION, value)

    /** 规则更新时间 - 最后更新规则的时间标识 */
    var ruleUpdate: String
        get() = getString(Setting.RULE_UPDATE_TIME, DefaultData.RULE_UPDATE_TIME)
        set(value) = putString(Setting.RULE_UPDATE_TIME, value)

    // ======== 脚本设置 ========

    /** 通用脚本内容 - 用户自定义的通用处理脚本 */
    var jsCommon: String
        get() = getString(Setting.JS_COMMON, DefaultData.JS_COMMON)
        set(value) = putString(Setting.JS_COMMON, value)

    /** 分类脚本内容 - 用户自定义的分类处理脚本 */
    var jsCategory: String
        get() = getString(Setting.JS_CATEGORY, DefaultData.JS_CATEGORY)
        set(value) = putString(Setting.JS_CATEGORY, value)

    // ======== Canary版本警告设置 ========

    /** 上次警告的Canary版本 - 记录已经显示过警告的版本号 */
    var lastCanaryWarningVersion: String
        get() = getString("canary_warning_version", "")
        set(value) = putString("canary_warning_version", value)


}