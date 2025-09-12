package org.ezbook.server.task

import android.content.Context
import kotlinx.coroutines.channels.Channel
import org.ezbook.server.Server
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.tools.BillManager
import org.ezbook.server.tools.runCatchingExceptCancel
import io.github.oshai.kotlinlogging.KotlinLogging

class BillProcessor {

    private val logger = KotlinLogging.logger(this::class.java.name)

    private val taskChannel = Channel<BillTask>(Channel.UNLIMITED)

    // 用于通知任务完成状态

    init {
        startProcessor()
    }

    private fun startProcessor() {
        Server.withIO {
            for (task in taskChannel) {
                runCatchingExceptCancel {
                    task.result = BillManager.groupBillInfo(task.billInfoModel, task.context)
                }.onFailure {
                    logger.error(it) { "处理任务失败: ${it.message}" }
                    task.result = null
                    task.complete()
                }.onSuccess {
                    task.complete()
                }
            }
        }
    }


    /**
     * 添加新任务
     * @return 任务对象，可用于跟踪任务状态
     */
    suspend fun addTask(billInfoModel: BillInfoModel, context: Context): BillTask {
        return BillTask(billInfoModel, context).also {
            taskChannel.send(it)
        }
    }


    /**
     * 优雅关闭处理器
     */
    fun shutdown() {
        taskChannel.close() // 关闭通道，不再接受新任务
    }

}
