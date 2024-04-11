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

import android.app.Activity
import android.content.Context
import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.exceptions.AutoServiceException
import net.ankio.auto.utils.request.RequestsUtils
import net.ankio.common.config.AccountingConfig
import java.io.File
import java.io.FileOutputStream


/**
 * AutoAccountingServiceUtils
 * 自动记账服务调用工具
 * @throws AutoServiceException
 */
class AutoAccountingServiceUtils(private val mContext: Context) {

    companion object {
        private const val HOST = "http://127.0.0.1"
        private const val PORT = 52045
        private const val SUPPORT_VERSION = "1.0.1"
        // 将isServerStart转换为挂起函数
        suspend fun isServerStart(retries:Int = 0): Boolean = withContext(Dispatchers.IO) {
            runCatching {
                val version = AppUtils.getService().request("/", count = retries).trim()
                SUPPORT_VERSION==version
            }.getOrElse {
                false
            }
        }


        fun getToken(mContext: Context): String {
            val path =
                Environment.getExternalStorageDirectory().path + "/Android/data/${mContext.packageName}/shell/token.txt"
            val file = File(path)
            if (file.exists()) {
                return file.readText().trim()
            }
            return get("token", mContext)
        }

        /**
         * 获取文件内容
         */
        fun get(name: String, mContext: Context): String {
            val path =
                Environment.getExternalStorageDirectory().path + "/Android/data/${mContext.packageName}/cache/shell/${name}.txt"
            val file = File(path)
            if (file.exists()) {
                return file.readText().trim()
            }
            return ""
        }

        fun set(name: String, data: String, mContext: Context) {
            val path =
                Environment.getExternalStorageDirectory().path + "/Android/data/${mContext.packageName}/cache/shell/${name}.txt"
            val file = File(path)
            if (!file.exists()) {
                file.createNewFile()
            }
            file.writeText(data)

        }

        fun delete(name: String, mContext: Context) {
            val path =
                Environment.getExternalStorageDirectory().path + "/Android/data/${mContext.packageName}/cache/shell/${name}.txt"
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }

        fun log(data: String, mContext: Context) {
            runCatching {
                val path =
                    Environment.getExternalStorageDirectory().path + "/Android/data/${mContext.packageName}/cache/shell/log.txt"
                val file = File(path)
                val parentDir = file.parentFile
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs() // 创建所有必需的父目录
                }
                if (!file.exists()) {
                    file.createNewFile() // 创建文件
                }
                // 将当前日志追加到文件
                file.appendText("\n[ ${DateUtils.getTime(System.currentTimeMillis())} ]\n$data\n")

                // 处理日志，超过500行只保留最后的500行
                val lines = file.readLines()
                if (lines.size > 500) {
                    file.writeText(lines.takeLast(500).joinToString(separator = "\n"))
                }
            }.onFailure {
                //写入到文件失败，忽略
            }
        }

        /**
         * 获取请求地址
         */
        fun getUrl(path: String = ""): String {
            return "$HOST:$PORT$path"
        }

        fun config(context: Context): AccountingConfig {
           return runCatching {
                val bookAppConfig = get("bookAppConfig", context)
                val config = Gson().fromJson(bookAppConfig, AccountingConfig::class.java)
                if (config == null) {
                    set("bookAppConfig", Gson().toJson(AccountingConfig()), context)
                    AccountingConfig()
                } else {
                    config
                }
            }.onFailure {
                Logger.e("获取配置失败", it)
            }.getOrNull() ?: AccountingConfig()
        }

    }



    suspend fun request(
        path: String,
        query: HashMap<String, String> = hashMapOf(),
        data: HashMap<String, String> = hashMapOf(),
        contentType: Int = RequestsUtils.TYPE_FORM,
        count: Int = 0
    ): String {
        return runCatching {
            withContext(Dispatchers.IO) {
                RequestsUtils(mContext).post(
                    getUrl(path),
                    query = query,
                    data = data,
                    contentType = contentType,
                    headers = hashMapOf(
                        "Authorization" to getToken(mContext)
                    )
                ).apply {
                    if (code != 200) {
                        throw AutoServiceException(
                            String(byteArray),
                            AutoServiceException.CODE_SERVER_AUTHORIZE
                        )
                    }
                }.let {
                    String(it.byteArray).trim()
                }
            }
        }.getOrElse {
            val nextCount = count + 1 // 递增 count 的值
            if (nextCount > 4) {
                throw it
            } else {
                Thread.sleep((nextCount * 300).toLong())
                request(path, query, data, contentType, nextCount) // 明确传递递增后的 count 值
            }
        }
    }


    /**
     * 获取数据
     */
    suspend fun get(name: String):String = withContext(Dispatchers.IO){
            request(
                "/get",
                query = hashMapOf("name" to name),
            )
    }

    /**
     * 设置数据
     */
    suspend fun set(name: String, value: String) = withContext(Dispatchers.IO){
        request(
            "/set",
            query = hashMapOf("name" to name),
            data = hashMapOf("raw" to value),
            contentType = RequestsUtils.TYPE_RAW,
        )
        }


    /**
     * 设置App记录数据
     */
   suspend fun putData(value: String)= withContext(Dispatchers.IO){
        request(
            "/data",
            data = hashMapOf("raw" to value),
            contentType = RequestsUtils.TYPE_RAW,
        )

    }


    suspend fun js(string: String):String = withContext(Dispatchers.IO){
        request(
            "/js",
            data = hashMapOf("raw" to string),
            contentType = RequestsUtils.TYPE_RAW,
        )
    }

    /**
     * 获取记录的数据
     */
    suspend fun getData():String = withContext(Dispatchers.IO){
        get("data")
    }

    /**
     * 设置App记录日志
     */
    suspend fun putLog(value: String)= withContext(Dispatchers.IO){

          request(
              "/log",
              data = hashMapOf("raw" to value),
              contentType = RequestsUtils.TYPE_RAW,
          )

    }

    /**
     * 获取App记录的日志
     */
    suspend fun getLog():String = withContext(Dispatchers.IO){
        get("log")
    }
    /**
     * 获取配置(API)
     */
   suspend fun config():AccountingConfig = withContext(Dispatchers.IO){
        runCatching {
            val bookAppConfig = get("bookAppConfig")
            val config = Gson().fromJson(bookAppConfig, AccountingConfig::class.java)
            if(config==null){
                set("bookAppConfig",Gson().toJson(AccountingConfig()))
                AccountingConfig()
            }else{
                config
            }
        }.onFailure {
            Logger.e("获取配置失败",it)
        }.getOrNull()?:AccountingConfig()
    }

    suspend  fun copyAssetsShellFolderToCache(activity: Activity,cacheDir: File?) = withContext(Dispatchers.IO) {
        val shellFolderPath = "shell"
        val destinationPath = cacheDir!!.path + File.separator //+ shellFolderPath
        Logger.i("Copying shell folder from assets to $destinationPath")
        copyAssetsDirToSDCard(activity, shellFolderPath, destinationPath)
    }

    private fun copyAssetsDirToSDCard(context: Context, assetsDirName: String, sdCardPath: String) {
       // Logger.d("copyAssetsDirToSDCard() called with: context = [$context], assetsDirName = [$assetsDirName], sdCardPath = [$sdCardPath]")
        try {
            val list = context.assets.list(assetsDirName)
            if (list!!.isEmpty()) {
                val inputStream = context.assets.open(assetsDirName)
                val mByte = ByteArray(1024)
                var bt = 0
                val file = File(
                    sdCardPath + File.separator
                            + assetsDirName.substring(assetsDirName.lastIndexOf('/'))
                )
                if (!file.exists()) {
                    file.createNewFile()
                } else {
                    return
                }
                val fos = FileOutputStream(file)
                while ((inputStream.read(mByte).also { bt = it }) != -1) {
                    fos.write(mByte, 0, bt)
                }
                fos.flush()
                inputStream.close()
                fos.close()
            } else {
                var subDirName = assetsDirName
                if (assetsDirName.contains("/")) {
                    subDirName = assetsDirName.substring(assetsDirName.lastIndexOf('/') + 1)
                }
                val newPath = sdCardPath + File.separator + subDirName
                val file = File(newPath)
                if (!file.exists()) file.mkdirs()
                for (s: String in list) {
                    copyAssetsDirToSDCard(context, assetsDirName + File.separator + s, newPath)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}