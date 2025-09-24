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
 *  limitations under the License.
 */

package org.ezbook.server.constant

/**
 * AI分析任务状态枚举
 */
enum class AnalysisTaskStatus {
    /** 待处理 */
    PENDING,

    /** 分析中 */
    PROCESSING,

    /** 已完成 */
    COMPLETED,

    /** 失败 */
    FAILED
} 