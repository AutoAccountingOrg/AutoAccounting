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

package org.ezbook.server.constant

/**
 * 设置项键名常量
 * 按照设置页面顺序组织，便于管理和维护
 */
object Setting {

    // ===================================================================
    // 记账设置 (settings_recording.xml)
    // ===================================================================

    // -------- 记账应用 --------
    const val BOOK_APP_ID = "setting_book_app_id"                    // 记账软件ID
    const val MANUAL_SYNC: String = "setting_manual_sync"            // 手动同步模式（开启后保存不自动同步）
    const val DELAYED_SYNC_THRESHOLD: String =
        "setting_delayed_sync_threshold" // 延迟同步阈值（未同步账单达到此数量时自动同步，0表示实时同步）

    // -------- 记录方式 --------
    const val AUTO_RECORD_BILL: String = "auto_record_bill"          // 自动记录账单（全局开关）
    const val LANDSCAPE_DND: String = "landscape_dnd"                // 横屏勿扰模式

    // -------- 账单识别 --------
    const val AUTO_GROUP = "setting_auto_group"                      // 自动去重（去重）
    const val AUTO_GROUP_TIME_THRESHOLD = "setting_auto_group_time_threshold" // 自动去重时间阈值（秒）
    const val AUTO_TRANSFER_RECOGNITION = "setting_auto_transfer_recognition" // 自动识别转账账单
    const val AUTO_TRANSFER_TIME_THRESHOLD = "setting_auto_transfer_time_threshold" // 转账账单合并时间阈值（秒）
    const val AI_BILL_RECOGNITION: String = "ai_bill_recognition"   // 使用AI识别账单

    // -------- 账单管理 --------
    const val SHOW_RULE_NAME = "setting_show_rule_name"              // 显示规则名称
    const val SETTING_FEE = "setting_fee"                           // 手续费
    const val SETTING_TAG = "setting_tag"                           // 标签功能
    const val NOTE_FORMAT = "setting_note_format"                   // 备注格式

    // -------- 账单标记 --------
    const val BILL_FLAG_NOT_COUNT = "setting_bill_flag_not_count"   // 不计收支标记开关
    const val BILL_FLAG_NOT_BUDGET = "setting_bill_flag_not_budget" // 不计预算标记开关

    // -------- 分类管理 --------
    const val AUTO_CREATE_CATEGORY = "setting_auto_create_category"  // 自动创建分类
    const val AI_CATEGORY_RECOGNITION: String = "ai_category_recognition" // 使用AI识别分类

    // -------- 资产管理 --------
    const val SETTING_ASSET_MANAGER = "setting_asset_manager"       // 资产管理
    const val SETTING_CURRENCY_MANAGER = "setting_multi_currency"   // 多币种
    const val SETTING_BASE_CURRENCY = "setting_base_currency"      // 本位币
    const val SETTING_SELECTED_CURRENCIES = "setting_selected_currencies" // 用户选中的常用币种（逗号分隔）
    const val SETTING_REIMBURSEMENT = "setting_reimbursement"      // 报销功能
    const val SETTING_DEBT = "setting_debt"                         // 债务功能
    const val AUTO_ASSET_MAPPING: String = "auto_asset_mapping"     // 记住资产映射（非AI版本）
    const val AI_ASSET_MAPPING: String = "ai_asset_mapping"        // 使用AI进行资产映射

    // -------- 账本配置 --------
    const val SETTING_BOOK_MANAGER = "setting_book_manager"         // 多账本
    const val DEFAULT_BOOK_NAME = "setting_default_book_name"      // 默认账本

    // ===================================================================
    // 交互设置 (settings_interaction.xml)
    // ===================================================================

    // -------- 提醒设置 --------
    const val TOAST_POSITION = "setting_toast_position"             // 提醒位置（top/center/bottom）
    const val SHOW_SUCCESS_POPUP = "setting_show_success_popup"     // 成功提示弹窗
    const val LOAD_SUCCESS: String = "load_success"                 // 加载成功
    const val SHOW_DUPLICATED_POPUP: String = "show_duplicated_popup" // 重复提示弹窗

    // -------- OCR识别 --------
    const val OCR_FLIP_TRIGGER: String = "ocr_flip_trigger"        // 翻转手机触发当前页面识别（非Xposed模式）
    const val OCR_SHOW_ANIMATION: String = "ocr_show_animation"    // OCR识别时显示动画

    // -------- 弹窗风格 --------
    const val USE_ROUND_STYLE = "setting_use_round_style"          // 圆角风格
    const val IS_EXPENSE_RED = "setting_is_expense_red"             // 支出是否显示为红色
    const val IS_INCOME_UP = "setting_is_income_up"                 // 收入是否显示向上箭头

    // -------- 记账小面板 --------
    const val FLOAT_GRAVITY_POSITION = "setting_float_gravity_position" // 记账小面板显示位置（left/right/top）
    const val FLOAT_TIMEOUT_OFF = "setting_float_timeout_off"       // 超时时间
    const val FLOAT_TIMEOUT_ACTION = "setting_float_timeout_action" // 超时操作
    const val FLOAT_CLICK = "setting_float_click"                  // 点击事件
    const val FLOAT_LONG_CLICK = "setting_float_long_click"         // 长按事件

    // -------- 记账面板 --------
    const val CONFIRM_DELETE_BILL: String = "confirm_delete_bill"   // 删除账单前二次确认

    // ===================================================================
    // AI助理 (settings_ai_assistant.xml)
    // ===================================================================

    // -------- AI配置 --------
    const val FEATURE_AI_AVAILABLE: String = "feature_ai_available" // AI功能可用性总开关
    const val API_PROVIDER = "api_provider"                         // API提供商
    const val API_KEY = "api_key"                                  // API密钥
    const val API_URI: String = "api_uri"                          // API地址
    const val API_MODEL: String = "api_model"                      // API模型

    // -------- 提示词管理 --------
    const val AI_BILL_RECOGNITION_PROMPT: String = "ai_bill_recognition_prompt" // 自定义AI账单识别Prompt
    const val AI_ASSET_MAPPING_PROMPT: String = "ai_asset_mapping_prompt" // 自定义AI资产映射Prompt
    const val AI_CATEGORY_RECOGNITION_PROMPT: String =
        "ai_category_recognition_prompt" // 自定义AI分类识别Prompt
    const val AI_SUMMARY_PROMPT: String = "ai_summary_prompt"      // 自定义AI总结Prompt

    // -------- AI功能 --------
    const val AI_MONTHLY_SUMMARY: String = "ai_monthly_summary"     // 使用AI进行账单总结（月度）
    const val RULE_MATCH_INCLUDE_DISABLED: String =
        "rule_match_include_disabled" // 禁用规则参与匹配（命中即跳过AI）

    // ===================================================================
    // 数据管理 (settings_data_management.xml)
    // ===================================================================

    // -------- 自动备份 --------
    const val AUTO_BACKUP = "auto_backup"                           // 自动备份
    const val BACKUP_KEEP_COUNT = "setting_backup_keep_count"      // 备份保留数量

    // -------- 本地备份 --------
    const val LOCAL_BACKUP_PATH = "setting_local_backup_path"      // 本地备份路径

    // -------- WebDAV备份 --------
    const val USE_WEBDAV = "setting_use_webdav"                    // 启用WebDAV
    const val WEBDAV_URL = "setting_webdav_url"                    // WebDAV服务器URL
    const val WEBDAV_USER = "setting_webdav_user"                  // WebDAV用户名
    const val WEBDAV_PASSWORD = "setting_webdav_password"         // WebDAV密码

    // ===================================================================
    // 系统设置 (settings_system.xml)
    // ===================================================================

    // -------- 外观设置 --------
    const val SYSTEM_LANGUAGE = "setting_system_language"         // 系统语言
    const val UI_FOLLOW_SYSTEM_ACCENT = "setting_ui_follow_system_accent" // 跟随系统强调色
    const val UI_THEME_COLOR = "setting_ui_theme_color"            // 主题色标识
    const val UI_DARK_THEME_MODE = "setting_ui_dark_theme_mode"    // 深色主题模式（-1跟随系统/1强制亮/2强制暗等）
    const val UI_PURE_BLACK = "setting_ui_pure_black"              // 纯黑暗色

    // -------- 更新设置 --------
    const val AUTO_CHECK_APP_UPDATE = "setting_check_app_update"   // 应用更新
    const val AUTO_CHECK_RULE_UPDATE = "setting_check_rule_update" // 规则更新
    const val CHECK_APP_UPDATE = AUTO_CHECK_APP_UPDATE             // 应用更新（别名，向后兼容）
    const val CHECK_RULE_UPDATE = AUTO_CHECK_RULE_UPDATE           // 规则更新（别名，向后兼容）
    const val UPDATE_CHANNEL = "setting_update_channel"            // 更新渠道

    // -------- 高级功能 --------
    const val DEBUG_MODE = "setting_debug_mode"                    // 调试模式
    const val SEND_ERROR_REPORT = "setting_send_error_report"      // 错误报告

    // ===================================================================
    // 其他设置（不在设置页面显示，但需要保留）
    // ===================================================================

    // -------- 自动记账相关（内部使用） --------
    const val HOOK_AUTO_SERVER: String = "hook_auto_server"         // 自动记账服务
    const val SHOW_AUTO_BILL_TIP = "show_auto_bill_tip"            // 自动记账提示
    const val SETTING_REMIND_BOOK: String = "setting_remind_book"   // 记账提醒
    const val LISTENER_APP_LIST = "setting_listener_app_list"       // 监听应用列表
    const val PROACTIVELY_MODEL: String = "proactively_model"       // 主动模式
    const val DATA_FILTER: String = "setting_data_filter"           // 数据关键词过滤（白名单），逗号分隔
    const val DATA_FILTER_BLACKLIST: String = "setting_data_filter_blacklist" // 数据关键词过滤（黑名单），逗号分隔

    // -------- 权限设置 --------
    const val SMS_FILTER: String = "sms_filter"                     // 短信过滤

    // -------- 同步设置 --------
    const val SYNC_TYPE = "setting_sync_type"                      // 同步类型
    const val LAST_SYNC_TIME: String = "last_sync_time"             // 最后同步时间
    const val LAST_BACKUP_TIME: String = "last_backup_time"         // 最后备份时间

    // -------- 同步哈希值 --------
    const val HASH_ASSET = "setting_hash_asset"                    // 资产哈希
    const val HASH_BILL = "setting_hash_bill"                      // 账单哈希
    const val HASH_BOOK = "setting_hash_book"                       // 账本哈希
    const val HASH_CATEGORY = "setting_hash_category"               // 分类哈希
    const val HASH_BAOXIAO_BILL: String = "hash_baoxiao_bill"       // 报销单哈希
    const val HASH_TAG = "setting_hash_tag"                        // 标签哈希

    // -------- UI设置（其他） --------
    const val USE_SYSTEM_SKIN = "setting_use_system_skin"          // 系统皮肤
    const val CATEGORY_SHOW_PARENT = "setting_category_show_parent" // 显示父分类

    // -------- 功能模块开关（其他） --------
    const val IGNORE_ASSET: String = "ignore_asset"                 // 忽略资产

    // -------- 系统设置（其他） --------
    const val KEY_FRAMEWORK: String = "framework"                  // 框架标识
    const val HIDE_ICON: String = "setting_hide_icon"               // 是否隐藏启动图标
    const val INTRO_INDEX: String = "setting_intro_index"           // 引导页索引/阶段
    const val LOCAL_ID: String = "setting_local_id"                // 本地实例ID
    const val TOKEN: String = "setting_token"                       // 访问令牌
    const val GITHUB_CONNECTIVITY: String = "setting_github_connectivity" // GitHub连通性探测

    // -------- 更新设置（其他） --------
    const val LAST_UPDATE_CHECK_TIME: String = "last_update_check_time" // 检查更新时间
    const val CHECK_UPDATE_TYPE = "setting_check_update_type"      // 更新类型
    const val RULE_VERSION = "setting_rule_version"                // 规则版本
    const val RULE_UPDATE_TIME = "setting_rule_update_time"        // 规则更新时间

    // -------- 脚本设置 --------
    const val JS_COMMON = "setting_js_common"                      // 通用脚本
    const val JS_CATEGORY = "setting_js_category"                  // 分类脚本

    // -------- 其他 --------
    const val DONATE_TIME: String = "donate_time"                  // 捐赠时间
}
