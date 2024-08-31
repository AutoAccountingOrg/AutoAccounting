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

package org.ezbook.server.server


import org.nanohttpd.protocols.http.ClientHandler
import org.nanohttpd.protocols.http.threading.IAsyncRunner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


 class BoundRunner : IAsyncRunner {
    private val executorService: ExecutorService = Executors.newCachedThreadPool()

    override fun exec(clientHandler: ClientHandler?) {
        // 将任务提交给线程池执行
        executorService.submit(clientHandler)
    }

    override fun closeAll() {
        // 关闭线程池
        executorService.shutdown()
    }

    override fun closed(clientHandler: ClientHandler?) {
        // 当客户端连接关闭时，处理相关逻辑（可选）

    }
}