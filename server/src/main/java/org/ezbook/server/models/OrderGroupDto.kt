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

package org.ezbook.server.models

import org.ezbook.server.db.model.BillInfoModel

/**
 * 账单按日期分组的响应模型
 * 服务端分组，避免客户端重复计算
 * @param date 日期字符串，格式：yyyy-MM-dd
 * @param bills 该日期下的账单列表
 */
data class OrderGroupDto(
    val date: String,
    val bills: List<BillInfoModel>
)

