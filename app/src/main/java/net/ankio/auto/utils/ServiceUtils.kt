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

import android.content.Context
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hjq.toast.Toaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.DialogProgressBinding
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ServiceUtils(private val context: Context) {
    private var shell: String
    private var cacheDir: File
    init {
        val packageName  = context.packageName
        val filePath =  Environment.getExternalStorageDirectory().path +"/Android/data/$packageName/"
        cacheDir = File(filePath)
        if(!cacheDir.exists()){
            cacheDir.mkdirs()
        }


        AppUtils.getScope().launch {
            copyAssets()
        }

        shell = "sh ${cacheDir.path}/shell/starter.sh"
    }

    fun hasRoot():Boolean{
      return runCatching { Runtime.getRuntime().exec("su")
          true }.getOrDefault(false)
    }

    fun startServerByRoot(showUi: Boolean = true) {
        var textView: TextView? = null
        var scrollView: ScrollView? = null
        var progressDialog: AlertDialog? =  null
        if(showUi){
            val layoutInflater = LayoutInflater.from(context)
            val dialogBinding = DialogProgressBinding.inflate(layoutInflater)
             textView = dialogBinding.progressText
             scrollView = dialogBinding.scrollView
            progressDialog =
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.title_command)
                    .setView(dialogBinding.root)
                    .setCancelable(false) // 设置对话框不可关闭
                    .show()
        }


        // 在协程中检查 root 权限并执行命令
        AppUtils.getScope().launch(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec("su")
                val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
                val bufferedWriter = OutputStreamWriter(process.outputStream)

                Logger.i("Executing shell command: $shell")

                // 写入命令
                bufferedWriter.write(shell)
                bufferedWriter.flush()
                bufferedWriter.close()

                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    if(showUi){
                        withContext(Dispatchers.Main) {
                            // 更新 TextView 来显示命令输出
                            textView!!.append(line + "\n")
                            scrollView!!.post { scrollView.fullScroll(View.FOCUS_DOWN) }
                        }
                    }else{
                        line?.let { Logger.d(it) }
                    }

                }
                process.waitFor()
                bufferedReader.close()
            } catch (e: Exception) {
                Logger.e("Error executing shell command", e)
                if(showUi) {
                    withContext(Dispatchers.Main) {
                        textView!!.append(context.getString(R.string.no_root_permission))
                    }
                }else{
                    Logger.e(context.getString(R.string.no_root_permission))
                }
            } finally {
                // 等待5秒钟关闭对话框
                if(showUi) {
                    delay(5000L)
                    withContext(Dispatchers.Main) {
                        progressDialog!!.dismiss()
                    }
                }
            }
        }
    }

    fun copyAdbCommand() {
        AppUtils.copyToClipboard("adb shell $shell")
        Toaster.show(context.getString(R.string.copy_command_success))
    }

    private suspend fun copyAssets() =
        withContext(Dispatchers.IO) {
            val context = AppUtils.getApplication()
            val cacheDir = cacheDir.path + File.separator + "shell"
            val copyFiles = arrayListOf("version.txt", "starter.sh", "apps.txt")
            // 检查cpu架构
            val cpu = System.getProperty("os.arch")!!
            val androidCpu =
                when {
                    cpu.contains("arm") -> "armeabi-v7a"
                    cpu.contains("aarch64") -> "arm64-v8a"
                    cpu.contains("i386") || cpu.contains("i686") -> "x86"
                    cpu.contains("x86_64") -> "x86_64"
                    else -> "arm64-v8a"
                }
            copyFiles.add("$androidCpu/auto_accounting_starter")
            copyFiles.forEach {
                // 从assets复制到cacheDir
                runCatching {
                    context.assets.open("shell" + File.separator + it).use { input ->
                        val file = File(cacheDir, it)
                        if (!file.exists()) {
                            file.parentFile?.mkdirs()
                        } else {
                            file.delete()
                        }
                        file.createNewFile()
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                }.onFailure {
                    Logger.e("复制文件失败", it)
                }
            }
        }

}