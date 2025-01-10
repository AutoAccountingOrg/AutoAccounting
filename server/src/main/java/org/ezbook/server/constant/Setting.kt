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
    val PROACTIVELY_MODEL: String = "proactively_model"
    val BOOK_APP_ACTIVITY: String = "book_app_activity"
    const val HASH_BAOXIAO_BILL: String = "hash_baoxiao_bill"
    const val SHOW_DUPLICATED_POPUP: String = "show_duplicated_popup"
    const val LANDSCAPE_DND: String = "landscape_dnd"
    const val AI_AUXILIARY: String = "ai_auxiliary"
    const val SMS_PERMISSION: String = "sms_permission"
    const val NOTIFICATION_PERMISSION: String = "notification_permission"
    const val SMS_FILTER: String = "sms_filter"

    // 检查间隔
    const val LAST_UPDATE_CHECK_TIME: String = "last_update_check_time"

    // 是否开启自动记账
    const val HOOK_WECHAT: String = "hook_wechat"
    const val HOOK_AUTO_SERVER: String = "hook_auto_server"

    //提醒用户记账软件设置
    const val SETTING_REMIND_BOOK: String = "setting_remind_book"
    const val DONATE_TIME: String = "donate_time"
    const val LOAD_SUCCESS: String = "load_success"
    const val KEY_FRAMEWORK: String = "framework"
    const val LAST_BACKUP_TIME: String = "last_backup_time"
    const val SHOW_AUTO_BILL_TIP = "show_auto_bill_tip"

    //自动备份
    const val AUTO_BACKUP = "auto_backup"

    // one api需要提供地址和模型
    const val AI_ONE_API_URI = "ai_one_api_uri"
    const val AI_ONE_API_MODEL = "ai_one_api_model"

    // 所用的AI模型
    const val AI_MODEL = "ai_model"

    // 是否使用AI辅助识别
    const val USE_AI = "use_ai"

    // API_KEY
    const val API_KEY = "api_key"

    // 同步类型
    const val SYNC_TYPE = "setting_sync_type"

    // 展示规则名称
    const val SHOW_RULE_NAME = "setting_show_rule_name"

    //自动记账对应的记账软件
    const val BOOK_APP_ID = "setting_book_app_id"

    //默认账本
    const val DEFAULT_BOOK_NAME = "setting_default_book_name"

    //监听的App列表
    const val LISTENER_APP_LIST = "setting_listener_app_list"

    // 调试模式
    const val DEBUG_MODE = "setting_debug_mode"

    // js: 通用
    const val JS_COMMON = "setting_js_common"

    // js 分类
    const val JS_CATEGORY = "setting_js_category"


    // 同步的资产的md5
    const val HASH_ASSET = "setting_hash_asset"

    // 同步的账单的md5
    const val HASH_BILL = "setting_hash_bill"

    // 同步的账本的md5
    const val HASH_BOOK = "setting_hash_book"

    // 同步的分类的md5
    const val HASH_CATEGORY = "setting_hash_category"


    /////////////////////////////////一些设置项///////////////////////
    //悬浮窗超时时间
    const val FLOAT_TIMEOUT_OFF = "setting_float_timeout_off"

    //悬浮标签倒计时结束后的操作
    const val FLOAT_TIMEOUT_ACTION = "setting_float_timeout_action"

    //悬浮标签被点击
    const val FLOAT_CLICK = "setting_float_click"

    //悬浮标签被长按
    const val FLOAT_LONG_CLICK = "setting_float_long_click"

    //是否显示记账成功的弹窗
    const val SHOW_SUCCESS_POPUP = "setting_show_success_popup"

    // 本地备份地址
    const val LOCAL_BACKUP_PATH = "setting_local_backup_path"

    // 使用Webdav
    const val USE_WEBDAV = "setting_use_webdav"

    // Webdav host
    const val WEBDAV_HOST = "setting_webdav_host"

    // webdav user
    const val WEBDAV_USER = "setting_webdav_user"

    // webdav password
    const val WEBDAV_PASSWORD = "setting_webdav_password"

    // 是否自动分组（去重）
    const val AUTO_GROUP = "setting_auto_group"

    // 使用圆角风格
    const val USE_ROUND_STYLE = "setting_use_round_style"

    // 是否自动创建分类
    const val AUTO_CREATE_CATEGORY = "setting_auto_create_category"

    // 规则版本
    const val RULE_VERSION = "setting_rule_version"

    // 规则更新时间
    const val RULE_UPDATE_TIME = "setting_rule_update_time"

    // 更新渠道
    const val UPDATE_CHANNEL = "setting_update_channel"


    //分类是否展示父类
    const val CATEGORY_SHOW_PARENT = "setting_category_show_parent"

    // 支出的颜色为红色
    const val EXPENSE_COLOR_RED = "setting_expense_color_red"

    // 发送错误报告
    const val SEND_ERROR_REPORT = "setting_send_error_report"

    // 系统语言
    const val SYSTEM_LANGUAGE = "setting_system_language"


    // 备注格式
    const val NOTE_FORMAT = "setting_note_format"

    // 使用系统皮肤
    const val USE_SYSTEM_SKIN = "setting_use_system_skin"

    // 检查更新类型
    const val CHECK_UPDATE_TYPE = "setting_check_update_type"

    //检查应用更新
    const val CHECK_APP_UPDATE = "setting_check_app_update"

    //检查规则更新
    const val CHECK_RULE_UPDATE = "setting_check_rule_update"

    // 资产管理
    const val SETTING_ASSET_MANAGER = "setting_asset_manager"

    // 多币种
    const val SETTING_CURRENCY_MANAGER = "setting_multi_currency"

    // 报销
    const val SETTING_REIMBURSEMENT = "setting_reimbursement"

    // 债务
    const val SETTING_DEBT = "setting_debt"

    // 多账本
    const val SETTING_BOOK_MANAGER = "setting_book_manager"

    // 手续费
    const val SETTING_FEE = "setting_fee"

    // 标签
    const val SETTING_TAG = "setting_tag"
}