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

package org.ezbook.server.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.ezbook.server.constant.AnalysisTaskStatus

/**
 * AI分析任务数据模型
 */
@Entity
class AnalysisTaskModel {
    /** 主键ID */
    @PrimaryKey(autoGenerate = true)
    var id = 0L

    /** 任务标题（周期显示名称） */
    var title: String = ""

    /** 开始时间戳 */
    var startTime: Long = 0

    /** 结束时间戳 */
    var endTime: Long = 0

    /** 任务状态 */
    var status: AnalysisTaskStatus = AnalysisTaskStatus.PENDING

    /** 创建时间 */
    var createTime: Long = System.currentTimeMillis()

    /** 更新时间 */
    var updateTime: Long = System.currentTimeMillis()

    /** 分析结果HTML内容 */
    var resultHtml: String? = null

    /** 错误信息（失败时记录） */
    var errorMessage: String? = null

    /** 进度百分比 (0-100) */
    var progress: Int = 0
} 