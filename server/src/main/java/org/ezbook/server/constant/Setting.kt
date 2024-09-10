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
}