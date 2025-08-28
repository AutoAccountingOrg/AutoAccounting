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

package net.ankio.auto.storage.backup

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.http.RequestsUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.PrefManager
import okhttp3.Credentials
import java.io.File

/**
 * WebDAV 管理器，负责处理 WebDAV 相关的备份和恢复操作
 */
class WebDAVManager(private val context: Context) {

    private val requestUtils = RequestsUtils()

    /**
     * WebDAV 配置信息
     */
    data class WebDAVConfig(
        val url: String,
        val username: String,
        val password: String
    )

    /**
     * 获取 WebDAV 配置
     */
    private fun getWebDAVConfig(): WebDAVConfig {
        return WebDAVConfig(
            url = PrefManager.webdavHost.trim('/'),
            username = PrefManager.webdavUser,
            password = PrefManager.webdavPassword
        )
    }

    /**
     * 初始化 WebDAV 请求工具
     */
    private fun initRequestUtils(config: WebDAVConfig) {
        requestUtils.addHeader("Authorization", Credentials.basic(config.username, config.password))
    }

    /**
     * 上传备份文件到 WebDAV
     * @param file 要上传的文件
     * @param filename 文件名
     * @param loadingUtils 加载提示工具
     */
    suspend fun uploadBackup(
        file: File,
        filename: String,
        loadingUtils: LoadingUtils
    ) = withContext(Dispatchers.IO) {
        val config = getWebDAVConfig()
        initRequestUtils(config)

        try {
            // 创建目录
            requestUtils.mkcol("${config.url}/AutoAccounting")

            // 上传文件
            withContext(Dispatchers.Main) {
                loadingUtils.setText(R.string.backup_webdav)
            }

            val result = requestUtils.put("${config.url}/AutoAccounting/$filename", file)
            handleWebDAVResponse(result.first)

        } catch (e: Exception) {
            Logger.e("Failed to upload backup: $e")
            handleWebDAVResponse(100) // 网络错误
            throw e
        }
    }

    /**
     * 从 WebDAV 下载备份文件
     * @param filename 文件名
     * @param targetFile 目标文件
     * @param loadingUtils 加载提示工具
     * @return 是否成功下载
     */
    suspend fun downloadBackup(
        filename: String,
        targetFile: File,
        loadingUtils: LoadingUtils
    ): Boolean = withContext(Dispatchers.IO) {
        val config = getWebDAVConfig()
        initRequestUtils(config)

        try {
            withContext(Dispatchers.Main) {
                loadingUtils.setText(R.string.restore_webdav)
            }

            val downloadUrl = "${config.url}/AutoAccounting/$filename"
            val result = requestUtils.download(downloadUrl, targetFile)

            if (!result) {
                handleWebDAVResponse(404) // 文件不存在
            }

            result
        } catch (e: Exception) {
            Logger.e("Failed to download backup: $e")
            handleWebDAVResponse(100) // 网络错误
            false
        }
    }

    /**
     * 处理 WebDAV 响应状态码
     * @param code HTTP 状态码
     */
    private fun handleWebDAVResponse(code: Int) {
        Logger.i("WebDAV response code: $code")

        val messageResId = when (code) {
            200, 201, 204 -> R.string.backup_success
            401, 403 -> R.string.backup_auth
            404, 409 -> R.string.backup_not_found
            100 -> R.string.net_error_msg
            else -> return
        }

        if (code in listOf(200, 201, 204)) {
            ToastUtils.info(messageResId)
        } else {
            ToastUtils.error(messageResId)
        }
    }
}
