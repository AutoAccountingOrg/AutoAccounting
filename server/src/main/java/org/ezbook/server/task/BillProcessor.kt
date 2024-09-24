package org.ezbook.server.task

import android.content.Context
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.tools.Bill
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class BillProcessor {
    private val queue: BlockingQueue<BillTask> = LinkedBlockingQueue()
    private val worker = Thread {
        while (true) {
            try {
                val task: BillTask = queue.take() // 阻塞，直到有任务可处理
                task.result = Bill.groupBillInfo(task.billInfoModel, task.context)
                task.complete() // 通知任务已完成
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
