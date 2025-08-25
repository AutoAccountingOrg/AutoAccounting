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

package org.ezbook.server.db.model

import org.ezbook.server.constant.BillType

/**
 * 账单摘要数据模型
 * 专门用于AI摘要查询，只包含必要的字段，避免查询返回字段不匹配问题
 */
data class BillSummaryModel(
    /**
     * 记账时间
     */
    val time: Long,

    /**
     * 账单类型
     */
    val type: BillType,

    /**
     * 金额
     */
    val money: Double,

    /**
     * 分类名称
     */
    val cateName: String,

    /**
     * 商户名称
     */
    val shopName: String,

    /**
     * 商品名称
     */
    val shopItem: String
)
