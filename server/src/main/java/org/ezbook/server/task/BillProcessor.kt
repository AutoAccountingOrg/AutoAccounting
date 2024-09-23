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

package org.ezbook.server.task

import android.content.Context
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.tools.Bill.groupBillInfo
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue


class BillProcessor {
    private val queue: BlockingQueue<BillTask> = LinkedBlockingQueue()
    private val worker = Thread {
        while (true) {
            try {
                val task: BillTask = queue.take() // 阻塞，直到有任务可处理
                task.result = groupBillInfo(task.billInfoModel, task.context)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt() // 处理线程中断
                break
            }
        }
    }

    init {
        worker.start() // 启动工作线程
    }

    fun addTask(billInfoModel: BillInfoModel, context: Context): BillTask {
        val task = BillTask(billInfoModel, context)
        queue.add(task) // 将任务添加到队列
        return task // 返回任务对象
    }

    fun shutdown() {
        worker.interrupt() // 中断工作线程
    }
}