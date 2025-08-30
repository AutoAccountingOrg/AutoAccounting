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

package net.ankio.auto.ui.model

/**
 * 替换项数据模型
 * 用于替换预览功能，显示替换项并支持删除操作
 *
 * 设计原则（遵循Linus好品味）：
 * 1. 简洁数据结构：只包含必要字段
 * 2. 不可变性：使用data class确保数据一致性
 * 3. 清晰语义：字段名直接表达含义
 *
 * @param from 原始值
 * @param to 替换后的值
 */
data class ReplaceItem(
    val from: String,
    val to: String
) {
    /**
     * 获取替换预览文本
     * @return 格式化的替换预览字符串
     */
    fun getPreviewText(): String = "\"$from\" → \"$to\""
}
