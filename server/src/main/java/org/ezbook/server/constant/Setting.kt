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

object Setting {
    // ======== AI设置 ========

    const val AI_BILL_RECOGNITION: String = "ai_bill_recognition"        // 使用AI识别账单
    const val AI_CATEGORY_RECOGNITION: String = "ai_category_recognition" // 使用AI识别分类
    const val AI_ASSET_MAPPING: String = "ai_asset_mapping"              // 使用AI进行资产映射
    const val AI_MONTHLY_SUMMARY: String = "ai_monthly_summary"          // 使用AI进行账单总结（月度）
    const val AI_SUMMARY_PROMPT: String = "ai_summary_prompt"            // 自定义AI总结Prompt
    const val AI_BILL_RECOGNITION_PROMPT: String = "ai_bill_recognition_prompt" // 自定义AI账单识别Prompt
    const val AI_ASSET_MAPPING_PROMPT: String = "ai_asset_mapping_prompt" // 自定义AI资产映射Prompt
    const val AI_CATEGORY_RECOGNITION_PROMPT: String =
        "ai_category_recognition_prompt" // 自定义AI分类识别Prompt
    const val API_PROVIDER = "api_provider"                                     // API提供商
    const val API_KEY = "api_key"                                     // API密钥
    const val API_URI: String = "api_uri"                                  // API地址
    const val API_MODEL: String = "api_model"                              // API模型

    // ======== 自动记账设置 ========
    const val AUTO_RECORD_BILL: String = "auto_record_bill"         // 自动记录账单（全局开关）
    const val AUTO_ASSET_MAPPING: String = "auto_asset_mapping"     // 自动资产映射（非AI版本）
    const val HOOK_AUTO_SERVER: String = "hook_auto_server"          // 自动记账服务
    const val BOOK_APP_ID = "setting_book_app_id"                    // 记账软件ID
    const val SHOW_AUTO_BILL_TIP = "show_auto_bill_tip"             // 自动记账提示
    const val SETTING_REMIND_BOOK: String = "setting_remind_book"    // 记账提醒
    const val AUTO_CREATE_CATEGORY = "setting_auto_create_category"  // 自动创建分类
    const val AUTO_GROUP = "setting_auto_group"                      // 自动去重（去重）
    const val LISTENER_APP_LIST = "setting_listener_app_list"        // 监听应用列表
    const val PROACTIVELY_MODEL: String = "proactively_model"             // 主动模式
    const val DATA_FILTER: String = "setting_data_filter"                 // 数据关键词过滤，逗号分隔

    // ======== 权限设置 ========
    const val SMS_FILTER: String = "sms_filter"                      // 短信过滤
    const val LANDSCAPE_DND: String = "landscape_dnd"                // 横屏勿扰模式

    // ======== 同步和备份 ========
    const val SYNC_TYPE = "setting_sync_type"                        // 同步类型
    const val LAST_SYNC_TIME: String = "last_sync_time"                   // 最后同步时间
    const val MANUAL_SYNC: String = "setting_manual_sync"                 // 手动同步模式（开启后保存不自动同步）
    const val AUTO_BACKUP = "auto_backup"                           // 自动备份
    const val LAST_BACKUP_TIME: String = "last_backup_time"         // 最后备份时间

    // WebDAV配置
    const val USE_WEBDAV = "setting_use_webdav"                     // 启用WebDAV
    const val WEBDAV_HOST = "setting_webdav_host"                   // WebDAV服务器
    const val WEBDAV_USER = "setting_webdav_user"                   // WebDAV用户名
    const val WEBDAV_PASSWORD = "setting_webdav_password"           // WebDAV密码
    const val WEBDAV_PATH = "setting_webdav_path"                   // WebDAV路径
    const val LOCAL_BACKUP_PATH = "setting_local_backup_path"       // 本地备份路径
    const val BACKUP_KEEP_COUNT = "setting_backup_keep_count"       // 备份保留数量

    // 同步哈希值
    const val HASH_ASSET = "setting_hash_asset"                     // 资产哈希
    const val HASH_BILL = "setting_hash_bill"                       // 账单哈希
    const val HASH_BOOK = "setting_hash_book"                       // 账本哈希
    const val HASH_CATEGORY = "setting_hash_category"               // 分类哈希
    const val HASH_BAOXIAO_BILL: String = "hash_baoxiao_bill"      // 报销单哈希

    // ======== UI设置 ========
    const val USE_ROUND_STYLE = "setting_use_round_style"           // 圆角风格
    const val USE_SYSTEM_SKIN = "setting_use_system_skin"           // 系统皮肤
    const val SHOW_RULE_NAME = "setting_show_rule_name"             // 显示规则名称
    const val SHOW_SUCCESS_POPUP = "setting_show_success_popup"     // 成功提示弹窗
    const val SHOW_DUPLICATED_POPUP: String = "show_duplicated_popup" // 重复提示弹窗
    const val CONFIRM_DELETE_BILL: String = "confirm_delete_bill"   // 删除账单前二次确认
    const val CATEGORY_SHOW_PARENT = "setting_category_show_parent" // 显示父分类
    const val IS_EXPENSE_RED = "setting_is_expense_red"             // 支出是否显示为红色
    const val IS_INCOME_UP = "setting_is_income_up"                 // 收入是否显示向上箭头
    const val NOTE_FORMAT = "setting_note_format"                   // 备注格式
    const val SYSTEM_LANGUAGE = "setting_system_language"           // 系统语言
    const val UI_DARK_THEME_MODE = "setting_ui_dark_theme_mode"     // 深色主题模式（-1跟随系统/1强制亮/2强制暗等）
    const val UI_PURE_BLACK = "setting_ui_pure_black"               // 纯黑暗色
    const val UI_FOLLOW_SYSTEM_ACCENT = "setting_ui_follow_system_accent" // 跟随系统强调色
    const val UI_THEME_COLOR = "setting_ui_theme_color"             // 主题色标识

    // ======== 悬浮窗设置 ========
    const val FLOAT_TIMEOUT_OFF = "setting_float_timeout_off"       // 超时时间
    const val FLOAT_TIMEOUT_ACTION = "setting_float_timeout_action" // 超时操作
    const val FLOAT_CLICK = "setting_float_click"                   // 点击事件
    const val FLOAT_LONG_CLICK = "setting_float_long_click"         // 长按事件
    const val FLOAT_GRAVITY_POSITION =
        "setting_float_gravity_position"   // 记账小面板显示位置（left/right/top）

    // ======== OCR显示设置 ========
    const val OCR_SHOW_ANIMATION: String = "ocr_show_animation"      // OCR识别时显示动画
    const val OCR_FLIP_TRIGGER: String = "ocr_flip_trigger"        // 翻转手机触发当前页面识别（非Xposed模式）

    // ======== 功能模块开关 ========
    const val SETTING_ASSET_MANAGER = "setting_asset_manager"       // 资产管理
    const val SETTING_CURRENCY_MANAGER = "setting_multi_currency"   // 多币种
    const val SETTING_REIMBURSEMENT = "setting_reimbursement"      // 报销功能
    const val SETTING_DEBT = "setting_debt"                         // 债务功能
    const val SETTING_BOOK_MANAGER = "setting_book_manager"         // 多账本
    const val SETTING_FEE = "setting_fee"                          // 手续费
    const val SETTING_TAG = "setting_tag"                          // 标签功能
    const val IGNORE_ASSET: String = "ignore_asset"                      // 忽略资产
    const val DEFAULT_BOOK_NAME = "setting_default_book_name"      // 默认账本

    // ======== 系统设置 ========
    const val DEBUG_MODE = "setting_debug_mode"                     // 调试模式
    const val SEND_ERROR_REPORT = "setting_send_error_report"      // 错误报告
    const val KEY_FRAMEWORK: String = "framework"                   // 框架标识
    const val LOAD_SUCCESS: String = "load_success"                // 加载成功
    const val DONATE_TIME: String = "donate_time"                  // 捐赠时间
    const val HIDE_ICON: String = "setting_hide_icon"               // 是否隐藏启动图标
    const val INTRO_INDEX: String = "setting_intro_index"           // 引导页索引/阶段
    const val LOCAL_ID: String = "setting_local_id"                 // 本地实例ID
    const val TOKEN: String = "setting_token"                       // 访问令牌
    const val GITHUB_CONNECTIVITY: String = "setting_github_connectivity" // GitHub连通性探测

    // ======== 更新设置 ========
    const val LAST_UPDATE_CHECK_TIME: String = "last_update_check_time" // 检查更新时间
    const val UPDATE_CHANNEL = "setting_update_channel"             // 更新渠道
    const val CHECK_UPDATE_TYPE = "setting_check_update_type"      // 更新类型
    const val CHECK_APP_UPDATE = "setting_check_app_update"        // 应用更新
    const val CHECK_RULE_UPDATE = "setting_check_rule_update"      // 规则更新
    const val RULE_VERSION = "setting_rule_version"                // 规则版本
    const val RULE_UPDATE_TIME = "setting_rule_update_time"        // 规则更新时间

    // ======== 脚本设置 ========
    const val JS_COMMON = "setting_js_common"                      // 通用脚本
    const val JS_CATEGORY = "setting_js_category"                  // 分类脚本
}