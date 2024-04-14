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

package net.ankio.auto.utils

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.hjq.toast.Toaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.exceptions.PermissionException
import net.ankio.auto.exceptions.RestoreBackupException
import net.ankio.auto.ui.activity.BaseActivity
import net.ankio.auto.ui.activity.MainActivity
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.utils.request.RequestsUtils
import okhttp3.Credentials
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupUtils(private val context: Context) {
    private val SUPPORT_VERSION = 201 // 支持恢复数据的版本号

    private var filename = "auto_backup_${System.currentTimeMillis()}.$suffix"

    private val uri = Uri.parse(SpUtils.getString("backup_uri", ""))

    companion object {
        const val suffix = "pk"

        fun registerBackupLauncher(activity: BaseActivity): ActivityResultLauncher<Uri?> {
            return activity.registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                uri?.let {
                    // 请求持久化权限
                    val takeFlags: Int =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    activity.contentResolver.takePersistableUriPermission(uri, takeFlags)
                    SpUtils.putString("backup_uri", uri.toString())
                }
            }
        }

        fun registerRestoreLauncher(activity: BaseActivity): ActivityResultLauncher<Array<String>> {
            return activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                uri?.let {
                    val fileExtension =
                        activity.contentResolver.getType(uri)
                            ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }

                    if (!suffix.equals(fileExtension, true)) {
                        Logger.i("fileExtension:$fileExtension")
                        Toaster.show(R.string.backup_error)
                        return@let
                    }

                    activity.lifecycleScope.launch {
                        val loadingUtils = LoadingUtils(activity)
                        loadingUtils.show(R.string.restore_loading)
                        runCatching {
                            val backupUtils = BackupUtils(activity)
                            backupUtils.getLocalBackup(it)
                        }.onFailure { it2 ->
                            loadingUtils.close()
                            if (it2 is RestoreBackupException) {
                                Toaster.show(it2.message)
                            } else {
                                Toaster.show(R.string.backup_error)
                                Logger.e(it2.message ?: "", it2)
                            }
                        }.onSuccess {
                            loadingUtils.close()
                            Toaster.show(R.string.restore_success)
                            delay(3000)
                            AppUtils.restart()
                        }
                    }
                }
            }
        }

        fun requestPermission(activity: MainActivity) {
            activity.backupLauncher.launch(null)
        }

        fun hasAccessPermission(context: Context): Boolean {
            val persistedUriPermissions = context.contentResolver.persistedUriPermissions
            return persistedUriPermissions.any {
                it.uri ==
                    Uri.parse(
                        SpUtils.getString(
                            "backup_uri",
                            "",
                        ),
                    ) && it.isReadPermission && it.isWritePermission
            }
        }
    }

    /**
     * 打包数据文件
     */
    private suspend fun packData(outputStream: OutputStream) =
        withContext(Dispatchers.IO) {
            val dataDir = context.filesDir // Getting the application's internal storage folder
            val cacheDir = context.cacheDir // Getting the application's cache folder
            val externalCacheDir =
                context.externalCacheDir // Getting the application's external cache folder

            val json =
                Gson().toJson(
                    mapOf(
                        "version" to SUPPORT_VERSION,
                        "versionName" to BuildConfig.VERSION_NAME,
                        "packageName" to BuildConfig.APPLICATION_ID,
                        "packageVersion" to BuildConfig.VERSION_CODE,
                    ),
                )

            ZipOutputStream(outputStream).use { zos ->
                // 将json写入压缩包
                zos.putNextEntry(ZipEntry("auto.index"))
                zos.write(json.toByteArray())
                addToZip(zos, dataDir, "data")
                addToZip(zos, cacheDir, "cache")
                if (externalCacheDir != null) {
                    addToZip(zos, externalCacheDir, "external_cache")
                }
            }
        }

    /**
     * 添加文件到压缩内容中
     */
    private fun addToZip(
        zos: ZipOutputStream,
        file: File,
        basePath: String,
    ) {
        if (file.isDirectory) {
            // 如果是文件夹，则递归地添加其内容
            file.listFiles()?.forEach { child ->
                addToZip(zos, child, "$basePath/${child.name}")
            }
        } else {
            // 如果是文件，则直接添加到ZIP文件中
            // 检查文件名，如果是备份文件，则跳过
            if (file.name != filename) {
                FileInputStream(file).use { fis ->
                    val zipEntry = ZipEntry(basePath)
                    zos.putNextEntry(zipEntry)
                    fis.copyTo(zos)
                    zos.closeEntry()
                }
            }
        }
    }

    private suspend fun checkUseful(inputStream: InputStream) =
        withContext(Dispatchers.IO) {
            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = null
                var useful = false
                while (zis.nextEntry.also { entry = it } != null) {
                    if (entry!!.name == "auto.index") {
                        val json =
                            zis.bufferedReader().use {
                                it.readText()
                            }
                        Gson().fromJson(json, Map::class.java).let { map ->
                            Logger.i("备份恢复包的数据信息:$map")
                            val version = (map["version"] as Double).toInt()
                            if (version < SUPPORT_VERSION) {
                                throw RestoreBackupException(
                                    context.getString(
                                        R.string.unsupport_backup,
                                        map["versionName"] as String,
                                    ),
                                )
                            }
                            val pack = map["packageName"] as String
                            if (pack != BuildConfig.APPLICATION_ID) {
                                throw RestoreBackupException(context.getString(R.string.unspport_package_backup))
                            }
                        }
                        useful = true
                        break
                    }
                    zis.closeEntry()
                }
                zis.close()
                if (!useful) {
                    throw RestoreBackupException(context.getString(R.string.unspport_package))
                }
            }
        }

    private suspend fun unpackData(
        inputStream: InputStream,
        name: String,
    ) = withContext(Dispatchers.IO) {
        ZipInputStream(inputStream).use { zis ->

            var entry: ZipEntry? = null

            while (zis.nextEntry.also { entry = it } != null) {
                if (entry!!.name != name) {
                    val outputFile =
                        when {
                            entry!!.name.startsWith("data/") ->
                                File(
                                    context.filesDir,
                                    entry!!.name.removePrefix("data/"),
                                )

                            entry!!.name.startsWith("cache/") ->
                                File(
                                    context.cacheDir,
                                    entry!!.name.removePrefix("cache/"),
                                )

                            entry!!.name.startsWith("external_cache/") ->
                                File(
                                    context.externalCacheDir,
                                    entry!!.name.removePrefix("external_cache/"),
                                )

                            else -> continue
                        }
                    FileOutputStream(outputFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
            }
        }
    }

    private suspend fun getLocalBackup(uri: Uri) =
        withContext(Dispatchers.IO) {
            val contentResolver: ContentResolver = context.contentResolver
            contentResolver.openInputStream(uri)?.use { inputStream ->
                checkUseful(inputStream)
            }
            contentResolver.openInputStream(uri)?.use { inputStream ->
                unpackData(inputStream, uri.lastPathSegment ?: "")
            }
        }

    /**
     * 文件备份到本地
     */
    suspend fun putLocalBackup() =
        withContext(Dispatchers.IO) {
            if (!hasAccessPermission(context)) throw PermissionException("No Storage Permission.")

            val documentUri =
                DocumentsContract.buildDocumentUriUsingTree(
                    uri,
                    DocumentsContract.getTreeDocumentId(uri),
                )

            val newFileUri =
                DocumentsContract.createDocument(
                    context.contentResolver,
                    documentUri,
                    "application/$suffix",
                    filename,
                )

            val outputStream: OutputStream? =
                newFileUri?.let {
                    context.contentResolver.openOutputStream(it)
                }
            outputStream?.apply {
                packData(this)
                this.close()
            }
        }

    suspend fun putWebdavBackup(mainActivity: MainActivity) =
        withContext(Dispatchers.IO) {
            filename = "auto_backup.$suffix"
            val file = File(context.cacheDir, filename)
            val loadingUtils = LoadingUtils(mainActivity)
            val outputStream = FileOutputStream(file)
            withContext(Dispatchers.Main) {
                loadingUtils.show(R.string.backup_pack)
            }
            packData(outputStream)
            // 使用requestUtils上传
            val requestUtils = RequestsUtils(context)
            withContext(Dispatchers.Main) {
                loadingUtils.setText(R.string.backup_webdav)
            }

            val (url, username, password) = getWebdavInfo()

            runCatching {
                val result =
                    requestUtils.mkcol(
                        "$url/AutoAccounting",
                        headers =
                            hashMapOf(
                                "Authorization" to Credentials.basic(username, password),
                            ),
                    )
                if (result.code == 201) {
                    uploadFile(requestUtils, url, username, password, file, loadingUtils)
                } else {
                    showWebDavMsg(result.code)
                    withContext(Dispatchers.Main) {
                        loadingUtils.close()
                    }
                }
            }.onFailure {
                Logger.e("创建目录失败:$it")
                showWebDavMsg(100)
                withContext(Dispatchers.Main) {
                    loadingUtils.close()
                }
            }
        }

    private suspend fun uploadFile(
        requestUtils: RequestsUtils,
        url: String,
        username: String,
        password: String,
        file: File,
        loadingUtils: LoadingUtils,
    ) {
        runCatching {
            val result =
                requestUtils.put(
                    "$url/AutoAccounting/$filename",
                    data =
                        hashMapOf(
                            "raw" to file.path.toString(),
                        ),
                    contentType = RequestsUtils.TYPE_RAW,
                    headers =
                        hashMapOf(
                            "Authorization" to Credentials.basic(username, password),
                        ),
                )
            showWebDavMsg(result.code)
            withContext(Dispatchers.Main) {
                loadingUtils.close()
            }
        }.onFailure {
            Logger.e("上传失败:$it", it)
            showWebDavMsg(100)
            withContext(Dispatchers.Main) {
                loadingUtils.close()
            }
        }
    }

    private fun getWebdavInfo(): Array<String> {
        val url = SpUtils.getString("setting_webdav_host", "").trim('/')
        val username = SpUtils.getString("setting_webdav_username", "")
        val password = SpUtils.getString("setting_webdav_password", "")
        return arrayOf(url, username, password)
    }

    private fun showWebDavMsg(code: Int) {
        when (code) {
            100 -> Toaster.show(R.string.net_error_msg)
            200 -> Toaster.show(R.string.backup_success)
            201 -> Toaster.show(R.string.backup_success)
            204 -> Toaster.show(R.string.backup_success)
            401 -> Toaster.show(R.string.backup_auth)
            403 -> Toaster.show(R.string.backup_auth)
            404 -> Toaster.show(R.string.backup_not_found)
            409 -> Toaster.show(R.string.backup_not_found)
            else -> Toaster.show(R.string.backup_unknown)
        }
    }

    suspend fun getWebdavBackup(mainActivity: MainActivity) =
        withContext(Dispatchers.IO) {
            val requestUtils = RequestsUtils(context)
            val (url, username, password) = getWebdavInfo()

            filename = "auto_backup.$suffix"
            val file = File(context.cacheDir, filename)
            val loadingUtils = LoadingUtils(mainActivity)
            withContext(Dispatchers.Main) {
                loadingUtils.show(R.string.restore_webdav)
            }
            runCatching {
                val result =
                    requestUtils.get(
                        "$url/AutoAccounting/$filename",
                        headers =
                            hashMapOf(
                                "Authorization" to Credentials.basic(username, password),
                            ),
                    )

                if (result.code == 200) {
                    withContext(Dispatchers.Main) {
                        loadingUtils.setText(R.string.restore_loading)
                    }
                    mainActivity.lifecycleScope.launch {
                        file.writeBytes(result.byteArray)
                        unpackData(file.inputStream(), filename)
                        withContext(Dispatchers.Main) {
                            loadingUtils.close()
                            Toaster.show(R.string.restore_success)
                            delay(3000)
                            AppUtils.restart()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showWebDavMsg(result.code)
                        loadingUtils.close()
                    }
                }
            }.onFailure {
                Logger.e("下载失败$it")
                withContext(Dispatchers.Main) {
                    loadingUtils.close()
                }
                showWebDavMsg(100)
            }
        }
}
