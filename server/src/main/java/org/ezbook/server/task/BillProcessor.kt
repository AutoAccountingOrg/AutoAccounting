package org.ezbook.server.task

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ezbook.server.Server
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.tools.Bill

class BillProcessor {
    private val scope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + 
        CoroutineExceptionHandler { _, throwable ->
           throwable.printStackTrace()
           Log.e("BillProcessor", "Coroutine error: ${throwable.message}", throwable)
        }
    )
    
    private val taskChannel = Channel<BillTask>(Channel.UNLIMITED)
    
    // 用于通知任务完成状态
    
    init {
        startProcessor()
    }
    
    private fun startProcessor() {
        scope.launch {
            for (task in taskChannel) {
                try {
                    processTask(task)
                } catch (e: Throwable) {
                    Server.log("处理任务失败: ${e.message}")
                    task.result = null
                } finally {
                    task.complete()
                }
            }
        }
    }
    
    private suspend fun processTask(task: BillTask) {
        withContext(Dispatchers.IO) {
            task.result = Bill.groupBillInfo(task.billInfoModel, task.context)
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
     * 添加新任务（非挂起版本）
     */
    fun addTaskAsync(billInfoModel: BillInfoModel, context: Context): BillTask {
        return BillTask(billInfoModel, context).also {
            scope.launch {
                taskChannel.send(it)
            }
        }
    }
    
    /**
     * 优雅关闭处理器
     */
    fun shutdown() {
        taskChannel.close() // 关闭通道，不再接受新任务
        scope.coroutineContext.cancelChildren() // 取消所有子协程
        scope.cancel() // 取消作用域
    }
    
    /**
     * 等待所有任务完成
     */
    suspend fun awaitCompletion() {
        taskChannel.close()
        scope.coroutineContext.job.children.forEach { it.join() }
    }
}
