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

/**
 * 分类聚合查询行模型。
 * @property parent 父类名称
 * @property child 子类名称（可能为空字符串）
 * @property amount 金额合计
 * @property count 记录数量
 */
data class CategoryAggregateRow(
    val parent: String,
    val child: String,
    val amount: Double?,
    val count: Int = 0
)


