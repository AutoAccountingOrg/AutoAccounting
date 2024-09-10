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
    //自动记账配置
    const val AUTO_CONFIG = "setting_auto_config"
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
    //是否显示记账成功的弹窗
    const val SHOW_SUCCESS_POPUP = "setting_show_success_popup"



    // 本地备份地址
    const val LOCAL_BACKUP_PATH = "setting_local_backup_path"
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

    // Github的AccessToken
    const val GITHUB_ACCESS_TOKEN = "setting_github_access_token"

    // 系统语言
    const val SYSTEM_LANGUAGE = "setting_system_language"

}