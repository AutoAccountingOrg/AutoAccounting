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

package net.ankio.auto.service.api

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.ankio.auto.service.CoreService
import net.ankio.auto.storage.Logger
import kotlin.coroutines.cancellation.CancellationException

/**
 * 核心服务的抽象基类
 * 定义了所有子服务必须实现的生命周期方法
 * 用于统一管理各种子服务（如服务器服务、OCR服务、悬浮窗服务等）
 */
abstract class ICoreService {

    /**
     * 服务销毁时调用
     * 子类需要在此方法中释放资源和清理工作
     */
    abstract fun onDestroy()

    /**
     * 核心服务实例的引用
     * 子服务可以通过此引用访问核心服务的功能
     */
    protected lateinit var coreService: CoreService

    /**
     * 服务创建时调用
     * 初始化子服务并保存核心服务的引用
     *
     * @param coreService 核心服务实例
     */
    open fun onCreate(coreService: CoreService) {
        this.coreService = coreService
    }

    fun launch(block: suspend CoroutineScope.() -> Unit) {
        coreService.lifecycleScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                Logger.d("服务已取消: ${e.message}")
            }
        }
    }

    /**
     * 服务启动命令处理
     * 子类需要实现此方法来处理具体的启动逻辑
     *
     * @param intent 启动意图，可能为null
     * @param flags 启动标志
     * @param startId 启动ID
     */
    abstract fun onStartCommand(intent: Intent?, flags: Int, startId: Int)
}