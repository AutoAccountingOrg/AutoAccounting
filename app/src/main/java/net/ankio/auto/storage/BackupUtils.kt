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

package net.ankio.auto.storage

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
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.exceptions.PermissionException
import net.ankio.auto.exceptions.RestoreBackupException
import net.ankio.auto.request.RequestsUtils
import net.ankio.auto.ui.activity.MainActivity
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.dialog.BackupSelectorDialog
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import okhttp3.Credentials
import org.ezbook.server.constant.Setting
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

class BackupUtils(private val context: Context) {
    private var filename =
        "backup_${BuildConfig.VERSION_NAME}(${SUPPORT_VERSION})_${System.currentTimeMillis()}.$SUFFIX"

    private val uri = Uri.parse(ConfigUtils.getString(Setting.LOCAL_BACKUP_PATH, ""))

    companion object {
        const val SUFFIX = "pk"
        const val SUPPORT_VERSION = 202 // 支持恢复数据的版本号

        private var backupLauncher: ActivityResultLauncher<Uri?>? = null
        private var restoreLauncher: ActivityResultLauncher<Array<String>>? = null

        fun initRequestPermission(activity: MainActivity) {
            // 初始化backup launcher
            backupLauncher = activity.registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                uri?.let {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or 
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    activity.contentResolver.takePersistableUriPermission(uri, takeFlags)
                    ConfigUtils.putString(Setting.LOCAL_BACKUP_PATH, uri.toString())
                }
            }

            // 初始化restore launcher
            restoreLauncher = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                uri?.let {
                    val fileExtension =
                        activity.contentResolver.getType(uri)
                            ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }

                    if (!SUFFIX.equals(fileExtension, true)) {
                        ToastUtils.info(R.string.backup_error)
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
                            Logger.e(it2.message ?: "", it2)
                            if (it2 is RestoreBackupException) {
                                ToastUtils.error(it2.message!!)
                            } else {
                                ToastUtils.info(R.string.backup_error)
                            }
                        }.onSuccess {
                            loadingUtils.close()
                            ToastUtils.info(R.string.restore_success)
                            delay(3000)
                            App.restart()
                        }
                    }
                }
            }
        }

        fun requestPermission(activity: MainActivity) {
            backupLauncher?.launch(null)
        }

        fun requestRestore(activity: MainActivity) {
            restoreLauncher?.launch(arrayOf("*/*"))
        }

        fun hasAccessPermission(context: Context): Boolean {
            val persistedUriPermissions = context.contentResolver.persistedUriPermissions
            return persistedUriPermissions.any {
                it.uri ==
                        Uri.parse(
                            ConfigUtils.getString(
                                Setting.LOCAL_BACKUP_PATH,
                                "",
                            ),
                        ) && it.isReadPermission && it.isWritePermission
            }
        }

       suspend fun autoBackup(context: MainActivity){

           fun shouldBackup(): Boolean {
               val lastBackupTime = ConfigUtils.getLong(Setting.LAST_BACKUP_TIME, 0L)
               val currentTime = System.currentTimeMillis()
               return (currentTime - lastBackupTime) >= Constants.BACKUP_TIME
           }

            if (ConfigUtils.getBoolean(Setting.AUTO_BACKUP)) {
                    if (!shouldBackup()) return

                    ConfigUtils.putLong(Setting.LAST_BACKUP_TIME, System.currentTimeMillis())

                    ToastUtils.info(R.string.backup_loading)
                    val backupUtils = BackupUtils(context)
                    runCatching {
                        if (ConfigUtils.getBoolean(Setting.USE_WEBDAV)) {
                            backupUtils.putWebdavBackup(context)
                        } else {
                            backupUtils.putLocalBackup()
                        }
                    }.onFailure {
                        Logger.e("自动备份失败", it)
                        ToastUtils.error(R.string.backup_error)
                    }.onSuccess {
                        ConfigUtils.putLong("last_backup_time", System.currentTimeMillis())
                        ToastUtils.info(R.string.backup_success)
                    }

            }
        }
    }

    /**
     * 打包数据文件
     */
    private suspend fun packData(filename: String) =
        withContext(Dispatchers.IO) {

            // 构建备份数据包

            // auto.db , 数据库文件
            // shared_prefs , SharedPreferences文件夹

            val backupDir = File(context.filesDir, "backup")

            if (backupDir.exists()) {
                backupDir.deleteRecursively()
            }
            backupDir.mkdirs()

            val settingFile = File(backupDir, "settings.json")
            ConfigUtils.copyTo(context, settingFile)

            // 使用okhttp实现文件下载
            val requestUtils = RequestsUtils(context)
            val dbFile = File(backupDir, "auto.db")
            val result = requestUtils.download("http://127.0.0.1:52045/db/export", dbFile)
            Logger.i("Download database: $result")

            if (!result) {
                throw RestoreBackupException(context.getString(R.string.backup_error))
            }

            val json =
                Gson().toJson(
                    mapOf(
                        "version" to SUPPORT_VERSION,
                        "versionName" to BuildConfig.VERSION_NAME,
                        "packageName" to BuildConfig.APPLICATION_ID,
                        "packageVersion" to BuildConfig.VERSION_CODE,
                    ),
                )

            // json写入备份文件夹
            val outputStream = FileOutputStream(File(backupDir, "auto.index"))
            outputStream.write(json.toByteArray())
            outputStream.close()


            ZipUtils.zipAll(backupDir, filename)
        }


    /**
     * 解压备份文件
     */
    private suspend fun unpackData(file: File) = withContext(Dispatchers.IO) {

        val backupDir = File(context.filesDir, "backup")
        if (backupDir.exists()) {
            backupDir.deleteRecursively()
        }
        backupDir.mkdirs()

        ZipUtils.unzip(file.absolutePath, backupDir.absolutePath) {
            Logger.i("Unzip progress:$it")
        }
        file.delete()
        val indexFile = File(backupDir, "auto.index")
        val json = indexFile.readText()
        indexFile.delete()
        val map = Gson().fromJson(json, JsonObject::class.java)
        Logger.i("Backup Data:$map")
        val version = map.get("version").asInt
        if (version < SUPPORT_VERSION) {
            throw RestoreBackupException(
                context.getString(
                    R.string.unsupport_backup,
                    map["versionName"],
                ),
            )
        }
        val pack = map["packageName"].asString
        if (pack != BuildConfig.APPLICATION_ID) {
            throw RestoreBackupException(context.getString(R.string.unspport_package_backup))
        }
        val settingFile = File(backupDir, "settings.json")
        ConfigUtils.copyFrom(context, settingFile)
        val dbFile = File(backupDir, "auto.db")

        val requestUtils = RequestsUtils(context)
        val result = requestUtils.upload("http://127.0.0.1:52045/db/import", dbFile)
        Logger.i("Upload result:$result")
    }

    /**
     * 从本地获取备份
     */
    private suspend fun getLocalBackup(uri: Uri) =
        withContext(Dispatchers.IO) {
            val contentResolver: ContentResolver = context.contentResolver
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val file = File(context.cacheDir, filename)
                file.writeBytes(inputStream.readBytes())
                unpackData(file)
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
                    "application/$SUFFIX",
                    filename,
                )

            val outputStream: OutputStream? =
                newFileUri?.let {
                    context.contentResolver.openOutputStream(it)
                }
            outputStream?.apply {
                val file = File(context.cacheDir, filename)
                packData(file.absolutePath)
                FileInputStream(file).use { fis ->
                    fis.copyTo(this)
                }
                this.close()
            }
        }

    suspend fun putWebdavBackup(mainActivity: MainActivity) =
        withContext(Dispatchers.IO) {
            val file = File(context.cacheDir, filename)
            val loadingUtils = LoadingUtils(mainActivity)
            withContext(Dispatchers.Main) {
                loadingUtils.show(R.string.backup_pack)
            }
            packData(file.absolutePath)
            // 使用requestUtils上传
            val requestUtils = RequestsUtils(context)
            withContext(Dispatchers.Main) {
                loadingUtils.setText(R.string.backup_webdav)
            }

            val (url, username, password) = getWebdavInfo()
            // Logger.i("url:$url,username:$username,password:$password")
            runCatching {
                requestUtils.addHeader("Authorization", Credentials.basic(username, password))
                requestUtils.mkcol("$url/AutoAccounting")
                uploadFile(requestUtils, url, file, loadingUtils)
            }.onFailure {
                Logger.e("Failed to create backup:$it")
                showWebDavMsg(100)
                withContext(Dispatchers.Main) {
                    loadingUtils.close()
                }
            }
        }

    private suspend fun uploadFile(
        requestUtils: RequestsUtils,
        url: String,
        file: File,
        loadingUtils: LoadingUtils,
    ) {
        runCatching {
            val result =
                requestUtils.put("$url/AutoAccounting/$filename", file)
            showWebDavMsg(result.first)
            withContext(Dispatchers.Main) {
                loadingUtils.close()
            }
        }.onFailure {
            Logger.e("Upload Failed:$it", it)
            showWebDavMsg(100)
            withContext(Dispatchers.Main) {
                loadingUtils.close()
            }
        }
    }

    private fun getWebdavInfo(): Array<String> {
        val url = ConfigUtils.getString(Setting.WEBDAV_HOST, "").trim('/')
        val username = ConfigUtils.getString(Setting.WEBDAV_USER, "")
        val password = ConfigUtils.getString(Setting.WEBDAV_PASSWORD, "")
        return arrayOf(url, username, password)
    }

    private fun showWebDavMsg(code: Int) {
        Logger.i("Webdav Return code:$code")
        when (code) {
            100 -> ToastUtils.error(R.string.net_error_msg)
            200 -> ToastUtils.info(R.string.backup_success)
            201 -> ToastUtils.info(R.string.backup_success)
            204 -> ToastUtils.info(R.string.backup_success)
            401 -> ToastUtils.error(R.string.backup_auth)
            403 -> ToastUtils.error(R.string.backup_auth)
            404 -> ToastUtils.error(R.string.backup_not_found)
            409 -> ToastUtils.error(R.string.backup_not_found)
            else -> {

            }
        }
    }

    private suspend fun downFromWebDav(
        url: String,
        requestUtils: RequestsUtils,
        mainActivity: MainActivity
    ) = withContext(Dispatchers.IO) {
        val loadingUtils = LoadingUtils(mainActivity)
        withContext(Dispatchers.Main) {

            loadingUtils.show(R.string.restore_webdav)
        }
        runCatching {
            filename = "auto_backup.$SUFFIX"
            val file = File(context.cacheDir, filename)
            val result = requestUtils.download(url, file)

            if (result) {
                withContext(Dispatchers.Main) {
                    loadingUtils.setText(R.string.restore_loading)
                }
                unpackData(file)
                withContext(Dispatchers.Main) {
                    loadingUtils.close()
                    ToastUtils.info(R.string.restore_success)

                }
            } else {
                withContext(Dispatchers.Main) {
                    showWebDavMsg(404)
                    loadingUtils.close()
                }
            }
        }.onFailure {
            Logger.e("Download Error: $it")
            withContext(Dispatchers.Main) {
                loadingUtils.close()
            }
            showWebDavMsg(100)
        }
    }

    suspend fun getWebdavBackup(mainActivity: MainActivity) =
        withContext(Dispatchers.Main) {
            val requestUtils = RequestsUtils(context)
            val (url, username, password) = getWebdavInfo()


            runCatching {
                requestUtils.addHeader("Authorization", Credentials.basic(username, password))

                val dialog = BackupSelectorDialog(mainActivity, {
                    if (it.isEmpty()) return@BackupSelectorDialog

                    mainActivity.lifecycleScope.launch {
                        downFromWebDav(
                            "$url/AutoAccounting/${Uri.encode(it)}",
                            requestUtils,
                            mainActivity
                        )

                    }

                }, requestUtils, "$url/AutoAccounting/")
                dialog.show()


            }.onFailure {
                Logger.e("Download Error: $it")
                showWebDavMsg(100)
            }
        }
}
