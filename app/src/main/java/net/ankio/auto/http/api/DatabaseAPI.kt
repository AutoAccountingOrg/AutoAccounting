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

package net.ankio.auto.http.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.http.LocalNetwork
import net.ankio.auto.storage.Logger
import org.ezbook.server.tools.runCatchingExceptCancel
import java.io.File
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * DatabaseAPI 对象提供了与数据库管理相关的网络请求操作
 * 包括数据库备份、恢复和清理功能
 */
object DatabaseAPI {

    private val logger = KotlinLogging.logger(this::class.java.name)
    /**
     * 导出数据库
     * @param targetFile 目标文件路径
     * @return 是否成功导出
     */
    suspend fun export(targetFile: File): Boolean = withContext(Dispatchers.IO) {
        val requestUtils = net.ankio.auto.http.RequestsUtils()
        val result = requestUtils.download("http://127.0.0.1:52045/db/export", targetFile)
        true
    }

    /**
     * 导入数据库
     * @param sourceFile 源文件路径
     * @return 是否成功导入
     */
    suspend fun import(sourceFile: File) = withContext(Dispatchers.IO) {
        val requestUtils = net.ankio.auto.http.RequestsUtils()
        val result = requestUtils.upload("http://127.0.0.1:52045/db/import", sourceFile)
        logger.info { result.toString() }
    }

    /**
     * 清空数据库
     * @return 服务器响应结果
     */
    suspend fun clear() = withContext(Dispatchers.IO) {

        runCatchingExceptCancel {
            LocalNetwork.post<String>("db/clear").getOrThrow()
        }.getOrElse {
            logger.error(it) { "clear error: ${it.message}" }
            throw it
        }
    }
} 