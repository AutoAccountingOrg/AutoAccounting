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
import java.util.concurrent.CountDownLatch


data class BillTask(
    val billInfoModel: BillInfoModel,
    val context: Context,
    var result: BillInfoModel? = null // 根据需要的返回类型进行更改
) {
    private val latch = CountDownLatch(1)

    fun await() {
        latch.await() // 阻塞当前线程，直到任务完成
    }

    fun complete() {
        latch.countDown() // 任务完成，释放等待的线程
    }
}